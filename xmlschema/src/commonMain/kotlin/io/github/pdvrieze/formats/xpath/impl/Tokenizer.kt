/*
 * Copyright (c) 2026.
 *
 * This file is part of xmlutil.
 *
 * This file is licenced to you under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance
 * with the License.  You should have  received a copy of the license
 * with the source distribution. Alternatively, you may obtain a copy
 * of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or
 * implied.  See the License for the specific language governing
 * permissions and limitations under the License.
 */

package io.github.pdvrieze.formats.xpath.impl

import io.github.pdvrieze.formats.xpath.SpecVersion
import io.github.pdvrieze.formats.xpath.XPathVersion
import io.github.pdvrieze.formats.xpath.impl.token.*
import io.github.pdvrieze.xml.schematypes.values.XsdInteger
import nl.adaptivity.xmlutil.XmlReader
import nl.adaptivity.xmlutil.core.impl.multiplatform.ifAssertions
import nl.adaptivity.xmlutil.core.internal.isNameChar11
import nl.adaptivity.xmlutil.core.internal.isNameStartChar
import nl.adaptivity.xmlutil.isXmlWhitespace

@XPathInternal
internal abstract class Tokenizer(protected val str: String, private val posInfo: XmlReader.LocationInfo?, private val useXML11:Boolean = true) {
    /** Flag to determine whether the last token was delimited */
    private var lastWasDelimited: Boolean = true

//    private var lastIsPeeked: Boolean = false

    private var lastToken: Token? = null
        set(value) {
            value?.let { lastWasDelimited = it.isDelimiting }
            field = value
        }

    protected var curPos = 0
        private set

    abstract val xpathVersion: XPathVersion

    abstract val isXPath2: Boolean
    abstract val isXPath30: Boolean
    abstract val isXPath31: Boolean

    val SpecVersion.isSupported: Boolean
        get() = this is XPathVersion && this <= xpathVersion

    fun mark(): Mark = Mark(curPos, lastWasDelimited)

    protected fun parseStringLiteral(): StringLiteral {
        val delim = when (str[curPos]) {
            '\'' -> '\''
            '"' -> '"'
            else -> parseError("Literal does not start with quote")
        }
        ++curPos
        var start = curPos

        val string = StringBuilder()

        while (curPos < str.length) {
            when (str[curPos]) {
                delim if (curPos + 1 < str.length && str[curPos + 1] == delim) -> {
                    string.append(str, start, curPos + 1)
                    curPos += 2 // skip reading the second delimiter (again)
                    start = curPos // Set the start after the second delimiter
                }

                delim -> break
                else -> ++curPos
            }
        }
        if (curPos > start) {
            string.append(str, start, curPos)
        }
        parseRequire(curPos < str.length, "Literal string not closed")
        lastWasDelimited = true
        return StringLiteral(string.toString()).also { curPos+=1 } // skip delim
    }

    @OptIn(NeedsXPath2::class)
    protected fun parseUnsignedLong(): Long {
        val s = parseDigitSequence()
        return (s.toLongOrNull()
            ?: throw NumberFormatException("'$s' is not a valid long value"))
    }

    @OptIn(NeedsXPath2::class)
    protected fun parseUnsignedInt(): Int {
        val s = parseDigitSequence()
        return (s.toIntOrNull()
            ?: throw NumberFormatException("'$s' is not a valid long value"))
    }

    private fun parseDigitSequence(): String {
        val start = curPos
        val l = str.length
        while (curPos < l && str[curPos] in '0'..'9') {
            curPos += 1
        }
        val s = str.substring(start, curPos)
        lastWasDelimited = false
        return s
    }

    protected fun parseNumber(): NumberLiteral<*> {
        ensureDelimited()
        val start = curPos

        var seenPeriod = false

        when (str[curPos]) {
            '-' -> curPos += 1
            '.' -> {
                seenPeriod = true
                curPos += 1
            }
        }

        parseRequire(curPos < str.length && str[curPos].isDigit(), "@$start> '${str.substring(start, curPos)}' not a number")
        var seenExp = false
        while (curPos < str.length) {
            when (str[curPos]) {
                '.' -> when {
                    seenPeriod || seenExp -> return DecimalLiteral(str.substring(start, curPos))
                    else -> seenPeriod = true
                }

                'e', 'E' -> when {
                    seenExp -> return DoubleLiteral(str.substring(start, curPos).toDouble())
                    else -> {
                        seenExp = true
                        // skip signs here
                        if (curPos + 1 < str.length) when (val c = str[curPos + 1]) {
                            '+', '-' -> curPos+=1
                        }
                    }
                }

                !in '0'..'9' -> break
            }
            curPos += 1
        }
        val substr = str.substring(start, curPos)

        lastWasDelimited = false

        @OptIn(NeedsXPath2::class)
        // TODO support decimal values without reverting to doubles
        return when {
            seenExp || !isXPath2 -> DoubleLiteral(substr.toDouble())
            seenPeriod -> DecimalLiteral(substr)

            else -> when (val l = substr.toLongOrNull()) {
                null -> IntegerLiteral(XsdInteger(substr))
                else -> LongLiteral(l)
            }
        }
    }

    protected fun parseQNameOrBuiltin(): QNameOrBuiltin {
        ensureDelimited()

        if (peekNext("Q{")) return parseEQNameTokenUndelim()
        val prefixOrLocal = parseNCName()
        BuiltinToken.getBuiltin(prefixOrLocal.name)?.let { return saveToken { it } }

        return saveToken {
            when {
                tryCurrent(':') ->
                    UnresolvedQNameToken(null, parseNCName(), prefixOrLocal)

                else -> UnresolvedQNameToken(null, prefixOrLocal, "")
            }
        }
    }

    /**
     * Parse a word (not allowing ':' letters)
     */
    protected fun parseNCName(): NCName {
        ensureDelimited()
        return parseNCNameUndelim()
    }

    protected fun parseNCNameUndelim(): NCName {
        val l = str.length
        require(curPos < l) { "Expected NCName, found end of input" }

        val start = curPos
        require(str[curPos].let { c -> c != ':' && isNameStartChar(c) }) { "Expected NCName, found '${str[curPos]}'" }
        curPos += 1
        while (curPos < l && isNameChar11(str[curPos], false)) {
            curPos += 1
        }

        return saveToken { NCName(str.substring(start, curPos)) }
    }

    protected fun parseQName(): UnresolvedQNameToken = saveToken {
        ensureDelimited()
        parseQNameUndelim()
    }

    private fun parseQNameUndelim(): UnresolvedQNameToken {
        val prefixOrLocal = parseNCNameUndelim()

        return when {
            tryCurrent(':') ->
                UnresolvedQNameToken(null, parseNCNameUndelim(), prefixOrLocal)

            else -> UnresolvedQNameToken(null, prefixOrLocal, "")
        }
    }

    protected fun parseEQNameTokenDelim(): UnresolvedQNameToken {
        ensureDelimited()

        return parseEQNameTokenUndelim()
    }

    protected fun parseEQNameTokenUndelim(): UnresolvedQNameToken {
        if (!(isXPath30 && tryCurrent("Q{"))) {
            return parseQNameUndelim()
        }
        val nsStart = curPos
        val l = str.length
        while (curPos < l && str[curPos] != '}') {
            curPos += 1
        }
        val namespace = str.substring(nsStart, curPos) // note that trimming is not expected
        parseRequire(tryCurrentToken('}'), "Expected '}' after namespace name")

        val localName = parseNCNameUndelim()
        return saveToken { UnresolvedQNameToken(namespace, localName, null) }
    }


    @IgnorableReturnValue
    fun <T> parseRequireNotNull(value: T?, message: String): T {
        return value ?: parseError(message)
    }

    fun parseRequire(condition: Boolean, message: String = "Unexpected token") {
        parseRequire(condition) { message }
    }

    fun parseError(cause: Throwable, startIdx: Int = curPos): Nothing =
        parseError(null, cause.message ?: "<unknown error>", cause, startIdx)


    inline fun <T> parseRequireNotNull(value: T?, message: () -> String): T {
        return value ?: parseError(message())
    }

    inline fun parseRequire(condition: Boolean, message: () -> String) {
        if (!condition) {
            parseError("Invalid expression", message())
        }
    }

    fun parseError(message: String, startIdx: Int = curPos): Nothing {
        parseError(null, message, startIdx = startIdx)
    }

    fun parseError(msgPrefix: String?, message: String, cause: Throwable? = null, startIdx: Int = curPos): Nothing {
        val indent = " ".repeat(6)
        val msg = buildString {
            msgPrefix?.let {
                append(msgPrefix)
                if (posInfo != null && msgPrefix.lastOrNull() != ' ') append(' ')
            }
            if (posInfo != null) append(" at $posInfo")
            append(": ").appendLine(message)
            val a = str.lastIndexOf('\n', startIdx)
            val pos = if (a < 0) startIdx else (startIdx - a)
            val b = str.indexOf('\n', startIdx)
            appendLine(((if (b < 0) str else str.substring(0, b))).prependIndent(indent))
            for (_ in 0 until (pos + indent.length)) append(' ')
            when {
                b < 0 -> append("^")
                else -> appendLine("^").append(str.substring(b + 1).prependIndent(indent))
            }
        }
        throw IllegalArgumentException(msg, cause)
    }

    protected fun ensureDelimited() {
        skipWhitespace()
        parseRequire(lastWasDelimited, "Multiple non-delimiting tokens succeeding each other")
    }

    protected fun skipWhitespace() {
        val start = curPos
        val l = str.length
        while (curPos < l) {
            val c = str[curPos]
            when {
                c == '(' -> {
                    if (str.getOrNull(curPos + 1) != ':') break

                    curPos += 2
                    parseCommentCont()
                }

                !isXmlWhitespace(c) -> break
                else -> ++curPos
            }
        }
        if (curPos > start) lastWasDelimited = true
    }

    private fun parseComment() {
        check(tryCurrent("(:"))
        return parseCommentCont()
    }

    private fun parseCommentCont() {
        val start = curPos - 2
        while (curPos < str.length) {
            val c = str[curPos]
            when (c) {
                ':' if tryCurrent(":)") -> return
                // support nesting
                '(' if str.getOrNull(curPos+1) ==':' -> parseComment()
                else -> curPos += 1
            }
        }
        parseError("Comment not closed", start)
    }

    /**
     * Read until the delimiter, consuming the delimiter as well.
     */
    protected fun readUntil(delim: Char): String {
        val end = str.indexOf(delim, curPos)
        if (end < 0) parseError("Expected '$delim' but found end of input")
        curPos = end + 1
        return str
    }

    protected fun peekNextToken(): Int {
        skipWhitespace()
        return if (curPos <str.length) str[curPos].code else -1
    }

    protected fun peekNextCharToken(): Char {
        skipWhitespace()
        return if (curPos <str.length) str[curPos] else '\u0000'
    }

    protected fun peekNextChar(cnt: Int): Char {
        val idx = curPos + cnt
        return if (idx < str.length) str[idx] else '\u0000'
    }

    protected fun peekNextChar(): Char {
        return if (curPos < str.length) str[curPos] else '\u0000'
    }

    protected fun peekNext(s: String): Boolean {
        return str.startsWith(s, curPos)
    }

    protected fun peekNextToken(s: String): Boolean {
        skipWhitespace()
        return (lastWasDelimited || Token.isDelim(s[0])) && str.startsWith(s, curPos)
    }

    protected fun peekNext(c: Char): Boolean {
        return curPos < str.length && str[curPos] == c
    }

    protected fun peekNextToken(c: Char): Boolean {
        skipWhitespace()
        return (lastWasDelimited || Token.isDelim(c)) && str[curPos] == c
    }

    protected fun tryCurrent(token : WordToken): Boolean {
        skipWhitespace()
        val delim = lastWasDelimited
        if (curPos < str.length) {
            val op = token
            val newI = curPos + op.literal.length
            if (!str.startsWith(op.literal, curPos)) return false

            if (!op.isDelimiting) {
                if (!delim) {
                    parseError("Missing delimiter before non-delimiting operator")
                }
                if (newI < str.length && !Token.isDelimOrWS(str[newI])) return false
            }

            if (!op.minVersion.isSupported) return false

            // important for distinguishing between operators such as '!' and '!=' or '<" and '<='
            if (op is Operator && op.longer.any { str.startsWith(it.literal, curPos) })
                return false

            curPos += op.literal.length

            val _ = saveToken { op }
            return true
        }
        return false

    }

    protected fun tryCurrent(s: String): Boolean {
        if(str.startsWith(s, curPos)) {
            val delim = Token.isDelim(s.last())
            val newI = curPos + s.length
            if (!delim && !Token.isDelimOrWS(str.getOrNull(newI))) return false
            curPos = newI
            lastWasDelimited = delim
            return true
        }
        return false
    }

    protected fun tryCurrentToken(s: String): Boolean {
        skipWhitespace()
        return tryCurrent(s)
    }

    protected open fun tryCurrent(char: Char): Boolean {
        if (! (lastWasDelimited || Token.isDelim(char))) {
            return false
        }
        if(curPos < str.length && str[curPos] == char) {
            curPos += 1
            lastWasDelimited = Token.isDelim(char)
            return true
        }
        return false
    }

    protected fun tryCurrentToken(char: Char): Boolean {
        skipWhitespace()
        return tryCurrent(char)
    }

    protected fun peekAnyOf(vararg chars: Char): Boolean {
        return curPos < str.length && str[curPos] in chars
    }

    protected fun tryAnyOf(vararg chars: Char): Char {
        if (curPos < str.length) {
            val ch = str[curPos]
            if (ch in chars) {
                curPos += 1
                lastWasDelimited = Token.isDelim(ch)
                return ch
            }
        }
        return '\u0000'
    }

    /** Note that this function does exist to ensure that length extensions are checked */
    protected fun tryAnyOf(vararg operators: Operator): Operator? {
        skipWhitespace()
        val delim = lastWasDelimited
        if (curPos < str.length) {
            for (op in operators) {
                if ((curPos + op.literal.length < str.length) &&
                    str.startsWith(op.literal, curPos) &&
                    op.longer.none { str.startsWith(it.literal, curPos) }
                ) {
                    if (!op.isDelimiting && !delim) {
                        parseError("Missing delimiter before non-delimiting operator")
                    }
                    if (op.minVersion.isSupported) {
                        curPos += op.literal.length

                        return saveToken { op }
                    }
                }
            }
        }
        return null
    }

    protected fun <T : WordToken> tryAnyOf(vararg tokens: T): T? {
        skipWhitespace()
        val delim = lastWasDelimited
        var longestToken: T? = null
        if (curPos < str.length) {
            for (token in tokens) {
                if ((longestToken== null || token.literal.length > longestToken.literal.length) && str.startsWith(token.literal, curPos)) {
                    val newI = curPos + token.literal.length
                    if (!token.isDelimiting) {
                        if (!token.isDelimiting && !delim) {
                            parseError("Missing delimiter before non-delimiting operator")
                        }

                        if (newI < str.length && isNameChar11(str[newI])) continue
                    }


                    if (token.minVersion.isSupported) {
                        curPos += token.literal.length

                        longestToken = token
                    }
                }
            }
        }
        return saveToken { longestToken }
    }

    protected fun assertPrevious(kw: WordToken) {
        ifAssertions {
            // first skip any trailing whitespace
            var end = curPos - 1
            // TODO also skip comments
            while (end >= 0 && isXmlWhitespace(str[end])) --end
            end += 1

            val expected = kw.literal

            val start = end - expected.length
            if (start < 0) parseError("Parse continuation not preceded by '$expected' due to length issue")


            if (str.substring(
                    start,
                    end
                ) != expected
            ) parseError("Parse continuation not preceded by '$expected', found: '${str.substring(start, end)}'")
        }
    }

    private inline fun <R: Token?> saveToken(crossinline body: () -> R): R {
        return body().also { if (it != null) lastToken = it }
    }

    inner class Mark(private val i: Int, private val lastWasDelimited: Boolean) {
        fun reset() {
            this@Tokenizer.curPos = i
            this@Tokenizer.lastWasDelimited = lastWasDelimited
        }
    }

}

