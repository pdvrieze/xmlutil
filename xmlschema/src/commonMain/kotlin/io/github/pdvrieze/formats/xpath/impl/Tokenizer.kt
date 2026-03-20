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

import io.github.pdvrieze.formats.xpath.XPathVersion
import io.github.pdvrieze.formats.xpath.impl.token.*
import io.github.pdvrieze.xml.schematypes.values.XsdInteger
import nl.adaptivity.xmlutil.XmlReader
import nl.adaptivity.xmlutil.core.internal.isNameChar11
import nl.adaptivity.xmlutil.core.internal.isNameStartChar
import nl.adaptivity.xmlutil.isXmlWhitespace

@XPathInternal
internal abstract class Tokenizer(protected val str: String, private val posInfo: XmlReader.LocationInfo?, private val useXML11:Boolean = true) {
    /** Flag to determine whether the last token was delimited */
    private var lastWasDelimited: Boolean = true

    private var lastIsPeeked: Boolean = false

    private var lastToken: Token? = null
        set(value) {
            value?.let { lastWasDelimited = it.isDelimiting }
            field = value
        }

    protected var i = 0

    abstract val xpathVersion: XPathVersion

    abstract val isXPath2: Boolean
    abstract val isXPath30: Boolean
    abstract val isXPath31: Boolean

    fun mark(): Mark = Mark(i, lastWasDelimited)

    protected fun parseStringLiteral(): StringLiteral {
        val delim = when (str[i]) {
            '\'' -> '\''
            '"' -> '"'
            else -> parseError("Literal does not start with quote")
        }
        ++i
        var start = i

        val string = StringBuilder()

        while (i < str.length) {
            when (str[i]) {
                delim if (i + 1 < str.length && str[i + 1] == delim) -> {
                    string.append(str, start, i + 1)
                    i += 2 // skip reading the second delimiter (again)
                    start = i // Set the start after the second delimiter
                }

                delim -> break
                else -> ++i
            }
        }
        if (i > start) {
            string.append(str, start, i)
        }
        parseRequire(i < str.length, "Literal string not closed")
        return StringLiteral(string.toString()).also { ++i } // skip delim
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
        val start = i
        val l = str.length
        while (i < l && str[i] in '0'..'9') {
            i += 1
        }
        val s = str.substring(start, i)
        return s
    }

    protected fun parseNumber(): NumberLiteral<*> {
        val start = i

        if (str[i] == '-') i+=1

        parseRequire(i < str.length && str[i].isDigit(), "@$start> '${str.substring(start, i)}' not a number")

        var seenPeriod = false
        var seenExp = false
        while (i < str.length) {
            when (str[i]) {
                '.' -> when {
                    seenPeriod || seenExp -> return DoubleLiteral(str.substring(start, i).toDouble())
                    else -> seenPeriod = true
                }

                'e', 'E' -> when {
                    seenExp -> return DoubleLiteral(str.substring(start, i).toDouble())
                    else -> {
                        seenExp = true
                        // skip signs here
                        if (i + 1 < str.length) when (val c = str[i + 1]) {
                            '+', '-' -> i+=1
                        }
                    }
                }

                !in '0'..'9' -> break
            }
            i += 1
        }
        val substr = str.substring(start, i)

        lastWasDelimited = false

        @OptIn(NeedsXPath2::class)
        // TODO support decimal values without reverting to doubles
        return when {
            seenPeriod || seenExp || !isXPath2 -> DoubleLiteral(substr.toDouble())

            else -> when (val l = substr.toLongOrNull()) {
                null -> IntegerLiteral(XsdInteger(substr))
                else -> LongLiteral(l)
            }
        }
    }

    protected fun parseQNameOrBuiltin(): QNameOrBuiltin {
        ensureDelimited()

        if (peekCurrent("Q{")) return parseEQNameTokenUndelim()
        val prefixOrLocal = parseNCName()
        BuiltinToken.getBuiltin(prefixOrLocal.name)?.let { return saveToken { it } }

        return saveToken {
            when {
                tryCurrent(':') ->
                    QNameToken(null, parseNCName(), prefixOrLocal)

                else -> QNameToken(null, prefixOrLocal, "")
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

    private fun parseNCNameUndelim(): NCName {
        val l = str.length
        require(i < l) { "Expected NCName, found end of input" }

        val start = i
        require(isNameStartChar(str[i])) { "Expected NCName, found '${str[i]}'" }
        i += 1
        while (i < l && (str[i] != ':' && isNameChar11(str[i]))) {
            i += 1
        }

        return saveToken { NCName(str.substring(start, i)) }
    }

    protected fun parseQName(): QNameToken = saveToken {
        ensureDelimited()
        parseQNameUndelim()
    }

    private fun parseQNameUndelim(): QNameToken {
        val prefixOrLocal = parseNCNameUndelim()

        return when {
            tryCurrent(':') ->
                QNameToken(null, parseNCNameUndelim(), prefixOrLocal)

            else -> QNameToken(null, prefixOrLocal, "")
        }
    }

    protected fun parseEQNameTokenDelim(): QNameToken {
        ensureDelimited()

        return parseEQNameTokenUndelim()
    }

    protected fun parseEQNameTokenUndelim(): QNameToken {
        if (!(isXPath30 && tryCurrent("Q{"))) {
            return parseQNameUndelim()
        }
        val nsStart = i
        val l = str.length
        while (i < l && str[i] != '}') {
            i += 1
        }
        val namespace = str.substring(nsStart, i) // note that trimming is not expected

        val localName = parseNCName()
        return saveToken { QNameToken(namespace, localName, null) }
    }


    fun <T> parseRequireNotNull(value: T?, message: String): T {
        return value ?: parseError(message)
    }

    fun parseRequire(condition: Boolean, message: String = "Unexpected token") {
        parseRequire(condition) { message }
    }

    fun parseError(cause: Throwable, startIdx: Int = i): Nothing =
        parseError(null, cause.message ?: "<unknown error>", cause, startIdx)


    inline fun <T> parseRequireNotNull(value: T?, message: () -> String): T {
        return value ?: parseError(message())
    }

    inline fun parseRequire(condition: Boolean, message: () -> String) {
        if (!condition) {
            parseError("Invalid expression", message())
        }
    }

    fun parseError(message: String, startIdx: Int = i): Nothing {
        parseError(null, message, startIdx = startIdx)
    }

    fun parseError(msgPrefix: String?, message: String, cause: Throwable? = null, startIdx: Int = i): Nothing {
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
        // TODO this is disabled for now as XQuery parser still does some things on its own.
//        parseRequire(lastWasDelimited, "Multiple non-delimiting tokens succeeding each other")

    }

    protected fun skipWhitespace() {
        val start = i
        val l = str.length
        while (i < l) {
            val c = str[i]
            when {
                c == '(' -> {
                    if (str.getOrNull(i + 1) != ':') break

                    i += 2
                    parseCommentCont()
                }

                !isXmlWhitespace(c) -> return
                else -> ++i
            }
        }
        if (i > start) lastWasDelimited = true
    }

    private fun parseComment() {
        check(tryCurrent("(:"))
        return parseCommentCont()
    }

    private fun parseCommentCont() {
        val start = i - 2
        while (i < str.length) {
            val c = str[i]
            when (c) {
                ':' if tryCurrent(":)") -> return
                // support nesting
                '(' if str.getOrNull(i+1) ==':' -> parseComment()
                else -> i += 1
            }
        }
        parseError("Comment not closed", start)
    }

    protected fun peekNext(): Int {
        skipWhitespace()
        return if (i <str.length) str[i].code else -1
    }

    protected open fun peekCurrent(s: String): Boolean {
        return str.startsWith(s, i)
    }

    protected open fun tryCurrent(s: String): Boolean {
        if(str.startsWith(s, i)) {
            val delim = Token.isDelim(s.last())
            val newI = i + s.length
            if (!delim && !Token.isDelimOrWS(str.getOrNull(newI))) return false
            i = newI
            lastIsPeeked = delim
            return true
        }
        return false
    }

    protected open fun tryCurrent(char: Char): Boolean {
        if (! (lastWasDelimited || Token.isDelim(char))) {
            return false
        }
        if(i < str.length && str[i] == char) {
            i += 1
            lastIsPeeked = Token.isDelim(char)
            return true
        }
        return false
    }

    protected fun tryCurrentToken(char: Char): Boolean {
        skipWhitespace()
        return tryCurrent(char)
    }

    protected fun peekAnyOf(vararg chars: Char): Boolean {
        return i < str.length && str[i] in chars
    }

    protected fun tryAnyOf(vararg chars: Char): Char {
        if (i < str.length) {
            val ch = str[i]
            if (ch in chars) {
                i += 1
                lastWasDelimited = Token.isDelim(ch)
                return ch
            }
        }
        return '\u0000'
    }

    protected fun tryAnyOf(vararg operators: Operator): Operator? {
        skipWhitespace()
        val delim = lastWasDelimited
        val start = i
        if (i < str.length) {
            val ch = str[i]
            for (op in operators) {
                if (op.minVersion <= xpathVersion && op.literal[0] == ch &&
                    (i + op.literal.length < str.length) &&
                    str.startsWith(op.literal, i) &&
                    op.longer.none { str.startsWith(it.literal, i) }
                ) {
                    if (! op.isDelimiting && ! delim) {
                        parseError("Missing delimiter before non-delimiting operator")
                    }

                    i += op.literal.length
                    return saveToken { op }
                }
            }
        }
        return null
    }


    private inline fun <R: Token> saveToken(crossinline body: () -> R): R {
        return body().also { lastToken = it }
    }

    inner class Mark(private val i: Int, private val lastWasDelimited: Boolean) {
        fun reset() {
            this@Tokenizer.i = i
            this@Tokenizer.lastWasDelimited = lastWasDelimited
        }
    }

}

