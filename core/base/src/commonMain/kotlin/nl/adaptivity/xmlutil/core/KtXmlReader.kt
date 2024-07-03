/*
 * Copyright (c) 2024.
 *
 * This file is part of xmlutil.
 *
 * This file is licenced to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You should have received a copy of the license with the source distribution.
 * Alternatively, you may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package nl.adaptivity.xmlutil.core

import nl.adaptivity.xmlutil.*
import nl.adaptivity.xmlutil.EventType.*
import nl.adaptivity.xmlutil.core.impl.EntityMap
import nl.adaptivity.xmlutil.core.impl.NamespaceHolder
import nl.adaptivity.xmlutil.core.impl.multiplatform.Reader
import nl.adaptivity.xmlutil.core.internal.isNameChar11
import nl.adaptivity.xmlutil.core.internal.isNameCodepoint
import nl.adaptivity.xmlutil.core.internal.isNameStartChar
import kotlin.jvm.JvmInline
import kotlin.jvm.JvmStatic

private const val BUF_SIZE = 4096
private const val ALT_BUF_SIZE = 512
private const val outputBufLeft = 0

@ExperimentalXmlUtilApi
/**
 * @param reader Reader for the input
 * @param encoding The encoding to record, note this doesn't impact the actual parsing (that is handled in the reader)
 * @param relaxed If `true` ignore various syntax and namespace errors
 * @param ignorePos If `true` don't record current position (line/offset)
 */
public class KtXmlReader internal constructor(
    private val reader: Reader,
    encoding: String?,
    public val relaxed: Boolean = false,
) : XmlReader {

    public constructor(reader: Reader, relaxed: Boolean = false) : this(reader, null, relaxed)

    private var line: Int = 1
    private val column: Int get() = offset - lastColumnStart + 1
    private var lastColumnStart = 0
    private var offset: Int = 0

    private var _eventType: EventType? = null //START_DOCUMENT // Already have this state
    public override val eventType: EventType
        get() = _eventType ?: throw IllegalStateException("Not yet started")

    override val isStarted: Boolean
        get() = state != State.BEFORE_START

    private var entityName: String? = null

    public override val localName: String
        get() = when (_eventType) {
            ENTITY_REF -> entityName ?: throw XmlException("Missing entity name")
            START_ELEMENT, END_ELEMENT -> elementStack[depth - 1].localName ?: throw XmlException("Missing local name")
            else -> throw IllegalStateException("Local name not accessible outside of element tags")
        }

    public override val namespaceURI: String
        get() = when (_eventType) {
            START_ELEMENT, END_ELEMENT -> elementStack[depth - 1].namespace ?: throw XmlException(
                "Missing namespace",
                extLocationInfo
            )

            else -> throw IllegalStateException("Local name not accessible outside of element tags")
        }

    public override val prefix: String
        get() = when (_eventType) {
            START_ELEMENT, END_ELEMENT -> elementStack[depth - 1].prefix ?: ""

            else -> throw IllegalStateException("Local name not accessible outside of element tags")
        }

    private var isSelfClosing = false

    override var attributeCount: Int = 0
        private set

    private var attrData: Array<String?> = arrayOfNulls(16)

    private var attributes: AttributesCollection = AttributesCollection()

    override var encoding: String? = encoding
        private set

    public override var version: String? = null
        private set

    public override var standalone: Boolean? = null
        private set


    private var srcBufPos: Int = 0
    private var srcBufCount: Int = 0

    //    private val srcBuf = CharArray(8192)
    private var bufLeft = CharArray(BUF_SIZE)
    private var bufRight: CharArray// = CharArray(BUF_SIZE)

    init { // Read the first buffers on creation, rather than delayed
        var cnt = reader.readUntilFullOrEOF(bufLeft)
        require(cnt >= 0) { "Trying to parse an empty file (that is not valid XML)" }
        if (cnt < BUF_SIZE) {
            bufRight = CharArray(0)
            srcBufCount = cnt
        } else {
            val newRight = CharArray(BUF_SIZE)
            bufRight = newRight
            cnt = reader.readUntilFullOrEOF(newRight).coerceAtLeast(0) // in case the EOF is exactly at the boundary
            srcBufCount = BUF_SIZE + cnt
        }

        if (bufLeft[0].code == 0x0feff) {
            srcBufPos = 1 /* drop BOM */
            offset = 1 // but also contain the BOM in the offset
            lastColumnStart = 1
        }
    }

    private var entityMap = EntityMap()

    private val namespaceHolder = NamespaceHolder()

    public override val depth: Int
        get() = namespaceHolder.depth

    private var elementStack: ElementStack = ElementStack()

    // variables so we don't need readCName to return a pair
    private var readPrefix: String? = null
    private var readLocalname: String? = null

    /** Target buffer for storing incoming text (including aggregated resolved entities)  */
    private var outputBuf = CharArray(ALT_BUF_SIZE)

    /** Write position   */
    private var outputBufRight = 0

    private var isWhitespace = false

    //    private int stackMismatch = 0;
    private var error: String? = null

    private var unresolved = false

    private var state = State.BEFORE_START

    override val namespaceDecls: List<Namespace>
        get() = namespaceHolder.namespacesAtCurrentDepth

    override val namespaceContext: IterableNamespaceContext
        get() = namespaceHolder.namespaceContext


    override fun close() {
        //NO-Op
    }

    private fun incCol() {
        offset += 1
    }

    private fun incLine(offsetAdd: Int = 1) {
        val newOffset = offset + offsetAdd
        offset = newOffset
        lastColumnStart = newOffset
        line += 1
    }

    private fun adjustNsp(prefix: String?, localName: String): Boolean {
        var hasActualAttribute = false

        // Loop through all attributes to collect namespace attributes and split name into prefix/localName.
        // Namespaces will not be set yet (as the namespace declaring attribute may be afterwards)
        var attrIdx = 0
        while (attrIdx < attributeCount) {
            val attr = attribute(attrIdx++)

            val aLocalName: String = attr.localName!!
            val aPrefix: String? = attr.prefix

            if (aPrefix == "xmlns") {
                namespaceHolder.addPrefixToContext(aLocalName, attr.value)
                if (attr.value == "") error("illegal empty namespace")

                attr.localName = null // mark for deletion
            } else if (aPrefix == null && aLocalName == "xmlns") {
                namespaceHolder.addPrefixToContext("", attr.value)
                attr.localName = null // mark for deletion
            } else {
                hasActualAttribute = true
            }
        }
        if (hasActualAttribute) {
            var attrInIdx = 0
            var attrOutIdx = 0

            // This gradually copies the attributes to remove namespace declarations
            // use while loop as we need the final size afterwards
            while (attrInIdx < attributeCount) {
                val attrIn = attribute(attrInIdx++)
                val attrLocalName = attrIn.localName
                if (attrLocalName != null) {
                    val attrOut = attribute(attrOutIdx++)

                    if (attrIn != attrOut) {
                        attributes.copyNotNs(attrIn.index, attrOut.index)
                    }

                    val attrPrefix = attrIn.prefix

                    if (attrPrefix == "") {
                        error("illegal attribute name: ${fullname(attrPrefix, attrLocalName)} at $this")
                        attrOut.namespace = "" // always true for null namespace
                    } else if (attrPrefix != null) {
                        val attrNs = namespaceHolder.getNamespaceUri(attrPrefix)
                        if (attrNs == null) error("Undefined Prefix: $attrPrefix in $this")
                        attrOut.namespace = attrNs
                    } else {
                        attrOut.namespace = ""
                    }
                }
            }

            if (attrInIdx != attrOutIdx) {
                attributes.shrink(attrOutIdx)
            }

        } else {
            attributes.shrink(0)
        }

        val ns = when {
            PROCESS_NAMESPACES -> namespaceHolder.getNamespaceUri(prefix ?: "")
                ?: XMLConstants.NULL_NS_URI.also { if (prefix != null) error("undefined prefix: $prefix") }

            else -> XMLConstants.NULL_NS_URI
        }

        val d = depth - 1
        elementStack[d].prefix = prefix
        elementStack[d].localName = localName
        elementStack[d].namespace = ns

        return hasActualAttribute
    }

    private fun error(desc: String) {
        if (relaxed) {
            if (error == null) error = "ERR: $desc"
        } else exception(desc)
    }

    private fun exception(desc: String): Nothing {
        throw XmlException(
            when {
                desc.length < 100 -> desc
                else -> "${desc.substring(0, 100)}\n"
            },
            this
        )
    }

    private fun parseUnexpectedOrWS(eventType: EventType) {
        when (eventType) {
            START_DOCUMENT -> {
                error("Unexpected START_DOCUMENT in state $state")
                parseStartTag(true) // parse it to ignore it
            }

            START_ELEMENT -> {
                error("Unexpected start tag after document body")
                parseStartTag(false)
            }

            END_ELEMENT -> {
                error("Unexpected end tag outside of body")
                parseEndTag()
            }

            ATTRIBUTE,
            IGNORABLE_WHITESPACE,
            COMMENT -> throw UnsupportedOperationException("Comments/WS are always allowed - they may start the document tough")

            TEXT -> {
                pushText('<')
                when {
                    isWhitespace -> _eventType = IGNORABLE_WHITESPACE
                    else -> error("Non-whitespace text where not expected: '${text}'")
                }
            }

            CDSECT -> {
                error("CData sections are not supported outside of the document body")
                parseCData()
            }

            DOCDECL -> {
                error("Document declarations are not supported outside the preamble")
                parseDoctype()
            }

            END_DOCUMENT -> {
                error("End of document before end of document element")
            }

            ENTITY_REF -> {
                error("Entity reference outside document body")
                pushEntity()
            }

            PROCESSING_INSTRUCTION -> {
                error("Processing instruction inside document body")
                parsePI()
            }
        }
    }

    private fun nextImplDocStart() {
        val eventType = peekType()
        if (eventType == START_DOCUMENT) {
            readAssert('<') // <
            readAssert('?') // ?
            parseStartTag(true)
            if (attributeCount < 1 || "version" != attribute(0).localName) error("version expected")
            version = attribute(0).value
            var pos = 1
            if (pos < attributeCount && "encoding" == attribute(1).localName) {
                encoding = attribute(1).value
                pos++
            }
            if (pos < attributeCount && "standalone" == attribute(pos).localName) {
                when (val st = attribute(pos).value) {
                    "yes" -> standalone = true
                    "no" -> standalone = false
                    else -> error("illegal standalone value: $st")
                }
                pos++
            }
            if (pos != attributeCount) error("illegal xmldecl")
            isWhitespace = true
        } // if it is not a doc start synthesize an event.
        _eventType = START_DOCUMENT
        state = State.START_DOC
        return
    }

    /**
     * common base for next and nextToken. Clears the state, except from
     * txtPos and whitespace. Does not set the type variable  */
    private fun nextImplPreamble() {
        error?.let { e ->
            push(e)

            this.error = null
            _eventType = COMMENT
            return
        }

        val eventType = peekType()
        _eventType = eventType
        when (eventType) {
            PROCESSING_INSTRUCTION -> parsePI()

            START_ELEMENT -> {
                state = State.BODY // this must start the body
                readAssert('<')
                parseStartTag(false)
            }

            DOCDECL -> {
                read("DOCTYPE")
                parseDoctype()
            }

            COMMENT -> parseComment()

            else -> parseUnexpectedOrWS(eventType)
        }
    }

    /**
     * common base for next and nextToken. Clears the state, except from
     * txtPos and whitespace. Does not set the type variable  */
    private fun nextImplBody() {
        // Depth is only decreased *after* the end element.
        if (_eventType == END_ELEMENT) namespaceHolder.decDepth()

        // degenerated needs to be handled before error because of possible
        // processor expectations(!)
        if (isSelfClosing) {
            isSelfClosing = false
            _eventType = END_ELEMENT
            if (depth == 1) state = State.POST
            return
        }

        error?.let { e ->
            push(e)

            this.error = null
            _eventType = COMMENT
            return
        }

        val eventType = peekType()
        _eventType = eventType
        when (eventType) {

            COMMENT -> parseComment()

            ENTITY_REF -> pushEntity()

            START_ELEMENT -> {
                readAssert('<')
                parseStartTag(false)
            }

            END_ELEMENT -> {
                parseEndTag()
                if (depth == 1) state = State.POST
            }

            TEXT -> {
                pushText('<')
                if (isWhitespace) _eventType = IGNORABLE_WHITESPACE
            }

            CDSECT -> parseCData()

            else -> parseUnexpectedOrWS(eventType)

        }
    }

    /**
     * Parse only the post part of the document. *misc* = Comment | PI | S
     */
    private fun nextImplPost() {
        if (_eventType == END_ELEMENT) namespaceHolder.decDepth()

        // degenerated needs to be handled before error because of possible
        // processor expectations(!)
        if (isSelfClosing) {
            isSelfClosing = false
            _eventType = END_ELEMENT
            return
        }
        error?.let { e ->
            push(e)

            this.error = null
            _eventType = COMMENT
            return
        }

        val eventType = peekType()
        _eventType = eventType
        when (eventType) {
            PROCESSING_INSTRUCTION -> parsePI()

            COMMENT -> parseComment()

            END_DOCUMENT -> {
                state = State.EOF
                return
            }

            else -> parseUnexpectedOrWS(eventType)
        }
    }

    private fun readTagContentUntil(delim: Char) {
        var c: Char
        do {
            c = readAndPush()
        } while (c != delim || peek() != '>'.code)
        popOutput()
        readAssert('>') // '>'
        return
    }

    private fun parsePI() {
        readAssert('<') // <
        readAssert('?') // '?'
        resetOutputBuffer()
        readTagContentUntil('?')
    }

    private fun parseComment() {
        readAssert('<') // <
        readAssert('!') // '!'
        readAssert('-') // '-
        read('-')

        resetOutputBuffer()
        var c: Char
        do {
            c = readAndPush()
        } while (c != '-' || peek() != '-'.code)
        if (peek(1) != '>'.code) {
            error("illegal comment delimiter: --->")
        }
        popOutput() // '-'
        readAssert('-') // '-'
        readAssert('>') // '>'

        return
    }

    private fun parseCData() {
        readAssert('<') // <
        readAssert('!') // '['
        read("[CDATA[")

        resetOutputBuffer()
        var c: Char
        do {
            c = readAndPush()
        } while (c != ']' || peek() != ']'.code || peek(1) != '>'.code)
        popOutput() // ']'
        readAssert(']') // ']'
        readAssert('>') // '>'
        return
    }

    /** precondition: &lt! consumed  */
    private fun parseDoctype() {
        var nesting = 1
        var quote: Char? = null

        while (true) {
            val i = read()
            when (i) {
                '\''.code,
                '"'.code -> when (quote) {
                    null -> quote = i.toChar()
                    i.toChar() -> quote = null
                }

                '-'.code -> if (quote == '!') {
                    pushChar('-')

                    var c = read()
                    pushChar(c)
                    if (c != '-'.code) continue

                    c = read()
                    pushChar(c)
                    if (c != '>'.code) continue

                    quote = null
                }

                '['.code -> if (quote == null && nesting == 1) ++nesting

                ']'.code -> if (quote == null) {
                    pushChar(']')
                    val c = read()
                    pushChar(c)
                    if (c != '>'.code) continue
                    if (nesting != 2) error("Invalid nesting of document type declaration: $nesting")
                    return
                }

                '<'.code -> if (quote == null) {
                    if (nesting < 2) error("Doctype with internal subset must have an opening '['")

                    pushChar('<')
                    var c = read()
                    pushChar(c)
                    if (c != '!'.code) {
                        nesting++; continue
                    }

                    c = read()
                    pushChar(c)
                    if (c != '-'.code) {
                        nesting++; continue
                    }

                    c = read()
                    pushChar(c)
                    if (c != '-'.code) {
                        nesting++; continue
                    }
                    quote = '!' // marker for comment
                }

                '>'.code -> if (quote == null) {
                    when (--nesting) {
                        1 -> error("Missing closing ']' for doctype")
                        0 -> return
                    }
                }
            }
            pushChar(i)
        }
    }

    /* precondition: &lt;/ consumed */
    private fun parseEndTag() {
        if (depth == 0) {
            error("element stack empty")
            _eventType = COMMENT
            return
        }

        readAssert('<') // '<'
        readAssert('/') // '/'

        resetOutputBuffer()
        val spIdx = depth - 1
        val expectedPrefix = elementStack[spIdx].prefix //?: exception("Missing prefix")
        val expectedLocalName = elementStack[spIdx].localName ?: exception("Missing localname")
        val expectedLength = (expectedPrefix?.run { length + 1 } ?: 0) + expectedLocalName.length

        val expectedEnd = srcBufPos + expectedLength
        if (expectedEnd>srcBufCount) exception(UNEXPECTED_EOF)
        if (expectedEnd < BUF_SIZE) { // fast path implementation that just verifies the tags
            // (rather than parsing them directly without that knowledge of expectation)
            val left2: Int
            if (expectedPrefix != null) {
                val left = srcBufPos
                for (i in expectedPrefix.indices)
                if (bufLeft[left + i] != expectedPrefix[i]) {
                    val expectedFullName = fullname(expectedPrefix, expectedLocalName)
                    error("expected: $expectedFullName read: ${readName()}")
                }
                left2 = left + expectedPrefix.length + 1
            } else {
                left2 = srcBufPos
            }

            for (i in expectedLocalName.indices) {
                if (bufLeft[left2 + i] != expectedLocalName[i]) {
                    val expectedFullName = fullname(expectedPrefix, expectedLocalName)
                    error("expected: $expectedFullName read: ${readName()}")
                }
            }
            srcBufPos = left2 + expectedLocalName.length
            skip()
            read('>')
            return
        }

        readCName()
        skip()
        read('>')
        if (!relaxed) {

            if (readPrefix != expectedPrefix || readLocalname != expectedLocalName) {
                val expectedFullName = fullname(expectedPrefix, expectedLocalName)
                val fullName = fullname(readPrefix, readLocalname!!)
                error("expected: ${expectedFullName} read: $fullName")
            }
        }
    }

    private fun peekType(): EventType {
        return when (peek()) {
            -1 -> END_DOCUMENT
            '&'.code -> ENTITY_REF
            '<'.code -> when (peek(1)) {
                '/'.code -> END_ELEMENT
                '?'.code -> when {
                    // order backwards to ensure
                    peek(2) == 'x'.code && peek(3) == 'm'.code &&
                            peek(4) == 'l'.code && !isNameCodepoint(peek(5)) ->
                        START_DOCUMENT

                    else -> PROCESSING_INSTRUCTION
                }

                '!'.code -> when (peek(2)) {
                    '-'.code -> COMMENT
                    '['.code -> CDSECT
                    else -> DOCDECL
                }

                else -> START_ELEMENT
            }

            else -> TEXT
        }
    }

    private fun get(): String {
        return outputBuf.concatToString(outputBufLeft, outputBufRight)
    }

    private fun popOutput() {
        --outputBufRight
    }

    private fun resetOutputBuffer() {
        // Do not reset it for speed reasons
        // if (outputBuf.size != ALT_BUF_SIZE) {
        //     outputBuf = CharArray(ALT_BUF_SIZE)
        // }
        outputBufRight = 0
    }

    private fun pushRange(buffer: CharArray, start: Int, endExcl: Int) {
        val count = endExcl - start
        val outRight = outputBufRight
        val minSizeNeeded = outRight + count
        if (minSizeNeeded >= outputBuf.size) {
            growOutputBuf(minSizeNeeded)
        }

        buffer.copyInto(outputBuf, outRight, start, endExcl)
        outputBufRight = outRight + count
    }

    private fun push(s: String) {
        val minSizeNeeded = outputBufRight + s.length
        if (minSizeNeeded > outputBuf.size) {
            growOutputBuf(minSizeNeeded)
        }

        for (c in s) {
            outputBuf[outputBufRight++] = c
        }
    }

    private fun pushChar(c: Char) {
        val newPos = outputBufRight

        // +1 for surrogates; if we don't have enough space in
        if (newPos >= outputBuf.size) growOutputBuf()

        outputBuf[outputBufRight++] = c
    }

    private fun pushChar(cp: Int) {
        when {
            cp < 0 -> error(UNEXPECTED_EOF)
            else -> pushChar(cp.toChar())
        }
    }

    private fun pushCodePoint(c: Int) {
        if (c < 0) error("UNEXPECTED EOF")

        val newPos = outputBufRight

        if (newPos + 1 >= outputBuf.size) { // +1 for surrogates; if we don't have enough space in
            growOutputBuf()
        }

        if (c > 0xffff) { // This comparison works as surrogates are in the 0xd800-0xdfff range
            // write high Unicode value as surrogate pair
            val offset = c - 0x010000
            outputBuf[outputBufRight++] = ((offset ushr 10) + 0xd800).toChar() // high surrogate
            outputBuf[outputBufRight++] = ((offset and 0x3ff) + 0xdc00).toChar() // low surrogate
        } else {
            outputBuf[outputBufRight++] = c.toChar()
        }
    }

    /** Sets name and attributes  */
    private fun parseStartTag(xmldecl: Boolean) {
        val prefix: String?
        val localName: String
        resetOutputBuffer()
        if (xmldecl) {
            prefix = null
            localName = readName()
        } else {
            readCName()
            prefix = readPrefix
            localName = readLocalname!!
        }
        attributes.clear()
        while (true) {
            skip()
            when (val c = peek(0)) {
                '?'.code -> {
                    if (!xmldecl) error("? found outside of xml declaration")
                    readAssert('?')
                    read('>')
                    return
                }

                '/'.code -> {
                    if (xmldecl) error("/ found to close xml declaration")
                    isSelfClosing = true
                    readAssert('/')
                    if (isXmlWhitespace(peek().toChar())) {
                        error("ERR: Whitespace between empty content tag closing elements")
                        while (isXmlWhitespace(peek().toChar())) read()
                    }
                    read('>')
                    break
                }

                '>'.code -> {
                    if (xmldecl) error("xml declaration must be closed by '?>', not '>'")
                    readAssert('>')
                    break
                }

                -1 -> {
                    error(UNEXPECTED_EOF)
                    return
                }

                ' '.code, '\t'.code, '\n'.code, '\r'.code -> {
                    next() // ignore whitespace
                }

                else -> when {
                    isNameStartChar(c.toChar()) -> {
                        resetOutputBuffer()
                        readCName()
                        val aLocalName = readLocalname!!

                        if (aLocalName.isEmpty()) {
                            error("attr name expected")
                            break
                        }
                        skip()
                        if (peek() != '='.code) {
                            val fullname = fullname(readPrefix, aLocalName)
                            error("Attr.value missing in $fullname '='. Found: ${peek(0).toChar()}")

                            attributes.addNsUnresolved(readPrefix, aLocalName, fullname)
                        } else {
                            read('=')
                            skip()
                            when (val delimiter = peek()) {
                                '\''.code, '"'.code -> {
                                    readAssert(delimiter.toChar())
                                    // This is an attribute, we don't care about whitespace content
                                    resetOutputBuffer()
                                    pushRegularText(delimiter.toChar(), resolveEntities = true)
                                    readAssert(delimiter.toChar())
                                }

                                else -> {
                                    error("attr value delimiter missing!")
                                    resetOutputBuffer()
                                    pushWSDelimAttrValue()
                                }
                            }

                            attributes.addNsUnresolved(readPrefix, aLocalName, get())
                        }
                    }

                    else -> {
                        val fullName = fullname(prefix, localName)
                        error("unexpected character in tag($fullName): '${c.toChar()}'")
                        readAssert(c.toChar())
                    }
                }
            }

        }

        val d = depth
        namespaceHolder.incDepth()
        elementStack.ensureCapacity(depth)

        if (PROCESS_NAMESPACES) {
            adjustNsp(prefix, localName)
        } else {
            elementStack[d].namespace = ""
        }
    }

    /**
     * result: if the setName parameter is set,
     * the name of the entity is stored in "name"
     */
    private fun pushEntity() {
        readAssert('&')
        val first = peek(0)

        when {
            first == '#'.code -> pushCharEntity()
            first < 0 -> error(UNEXPECTED_EOF)
            else -> pushRefEntity()
        }
    }

    private fun pushRefEntity() {
        val first = read()
        val codeBuilder = StringBuilder(8)

        if (!isNameStartChar(first.toChar())) {
            error("Entity reference does not start with name char &${get()}${first.toChar()}")
            return
        }
        codeBuilder.append(first.toChar())

        while (true) {
            val c = peek(0)
            if (c == ';'.code) {
                readAssert(';')
                break
            }
            if (!isNameChar11(c.toChar())) {
                error("unterminated entity ref")

                return
            }
            codeBuilder.append(read().toChar())
        }

        val code = codeBuilder.toString()//get(pos)
        if (_eventType == ENTITY_REF) {
            entityName = code
        }

        val result = entityMap[code]
        unresolved = result == null
        if (result != null) {
            push(result)
        }
    }

    private fun pushCharEntity() {
        readAssert('#') // #
        val codeBuilder = StringBuilder(8)

        var isHex = false

        when (val first = read()) {
            'x'.code -> isHex = true // hex char refs
            in '0'.code..'9'.code -> {
                codeBuilder.append(first.toChar())
            }

            else -> error("Unexpected start of numeric entity reference '&${first.toChar()}'")
        }

        while (true) {
            when (val c = peek(0)) {
                -1 -> error(UNEXPECTED_EOF)
                ';'.code -> {
                    readAssert(';')
                    break
                }

                in 'a'.code..'f'.code, // allow hex
                in 'A'.code..'F'.code, // allow hex
                in '0'.code..'9'.code -> codeBuilder.append(read().toChar())

                else -> {
                    error("Unexpected content in numeric entity reference: ${c.toChar()} (in $codeBuilder")
                    break
                }
            }
        }
        val code = codeBuilder.toString()//get(pos)

        if (_eventType == ENTITY_REF) entityName = code

        val cp = if (isHex) code.toInt(16) else code.toInt()
        pushCodePoint(cp)
        return
    }

    /**
     * General text push/parse algorithm.
     * Content:
     * '<': parse to any token (for nextToken ())
     * ']': CDATA section
     * Attributes:
     * '"': parse to quote
     * NO LONGER SUPPORTED - use pushTextWsDelim ' ': parse to whitespace or '>'
     */
    private fun pushText(delimiter: Char) {
        var bufCount = srcBufCount
        var innerLoopEnd = minOf(bufCount, BUF_SIZE)
        var curPos = srcBufPos

        while (curPos < innerLoopEnd) {
            when (bufLeft[curPos]) {
                ' ', '\t', '\n', '\r' -> break // whitespace

                else -> return pushRegularText(delimiter, resolveEntities = false)
            }
        }

        var left: Int = curPos
        var right: Int = -1
        var notFinished = true

        outer@ while (curPos < bufCount && notFinished) { // loop through all buffer iterations
            var continueInNonWSMode = false
            inner@ while (curPos < innerLoopEnd) {
                when (bufLeft[curPos]) {
                    '\r' -> {
                        // pushRange doesn't do normalization, so use push the preceding chars,
                        // then handle the CR separately
                        if (right > left + 1) pushRange(bufLeft, left, right)
                        right = -1
                        val peekChar = when (curPos + 1) {
                            bufCount -> '\u0000'
                            BUF_SIZE -> bufRight[0]
                            else -> bufLeft[curPos + 1]
                        }
                        if (peekChar != '\n') {
                            pushChar('\n')
                            incLine() // Increase positions here
                        } else {
                            ++offset // just ignore this character, but add it to the offset
                        }
                        left = curPos + 1
                        ++curPos
                    }

                    '\n' -> {
                        incLine()
                        ++curPos
                    }

                    ' ', '\t' -> {
                        incCol()
                        ++curPos
                    }

                    delimiter -> {
                        notFinished = false
                        right = curPos
                        break@inner // outer will actually give the result.
                    }

                    else -> {
                        continueInNonWSMode = true
                        right = curPos
                        break@inner
                    }
                }
            }

            if (curPos == innerLoopEnd) right = curPos

            if (right > left) {
                pushRange(bufLeft, left, right) // ws delimited is never WS
                right = -1
            }

            if (curPos == BUF_SIZE) { // swap the buffers
                srcBufPos = curPos
                swapInputBuffer()
                curPos = srcBufPos
                bufCount = srcBufCount
                innerLoopEnd = minOf(bufCount, BUF_SIZE)
            }

            if (continueInNonWSMode) {
                srcBufPos = curPos
                return pushRegularText(delimiter, resolveEntities = false)
            }

            left = curPos
        }

        // We didn't return through pushNonWSText, so it is WS
        isWhitespace = true
        srcBufPos = curPos
    }

    /**
     * Specialisation of pushText that does not recognize whitespace (thus able to be used at that point)
     * @param delimiter The "stopping" delimiter
     * @param resolveEntities Whether entities should be resolved directly (in attributes) or exposed as entity
     *                        references (content text).
     */
    private fun pushRegularText(delimiter: Char, resolveEntities: Boolean) {
        var bufCount = srcBufCount
        var innerLoopEnd = minOf(bufCount, BUF_SIZE)
        var curPos = srcBufPos

        var left: Int = curPos
        var right: Int = -1
        var cbrCount = 0
        var notFinished = true

        outer@ while (curPos < bufCount && notFinished) { // loop through all buffer iterations
            inner@ while (curPos < innerLoopEnd) {
                when (bufLeft[curPos]) {
                    delimiter -> {
                        notFinished = false
                        right = curPos
                        break@inner // outer will actually give the result.
                    }

                    '\r' -> {
                        pushRange(bufLeft, left, curPos)

                        val nextIsCR = when (val next = curPos + 1) {
                            bufCount -> false // EOF
                            BUF_SIZE -> bufRight[0] == '\n' // EOB, look at right buffer
                            else -> bufLeft[next] == '\n'
                        }

                        if (nextIsCR) {
                            incLine(2)
                            curPos += 2
                        } else {
                            incLine()
                            curPos += 1
                        }
                        pushChar('\n')
                        right = -1
                    }

                    ' ', '\t' -> {
                        incCol()
                        ++curPos
                    }

                    '\n' -> {
                        incLine()
                        ++curPos
                    }

                    '&' -> when {
                        !resolveEntities -> {
                            right = curPos
                            notFinished = false
                            break@inner
                        }

                        left == curPos -> { // start with entity
                            srcBufPos = curPos
                            pushEntity()
                            curPos = srcBufPos
                            left = curPos
                        }

                        else -> { // read all items before entity (then after it will hit the other case)
                            right = curPos
                            break@inner
                        }
                    }

                    ']' -> {
                        incCol()
                        ++cbrCount
                        ++curPos
                    }

                    '>' -> {
                        incCol()
                        if (cbrCount >= 2) error("Illegal ]]>")
                        ++curPos
                    }

                    else -> {
                        incCol()
                        ++curPos
                    }
                }
            }

            if (curPos == innerLoopEnd) {
                right = curPos
            }

            if (right > 0) {
                pushRange(bufLeft, left, right) // ws delimited is never WS
                right = -1
            }

            if (curPos >= BUF_SIZE) { // swap the buffers, use ge to allow for extra '\n' after '\r'
                srcBufPos = curPos
                swapInputBuffer()
                curPos = srcBufPos
                bufCount = srcBufCount
                innerLoopEnd = minOf(bufCount, BUF_SIZE)
            }
            left = curPos

        }
        isWhitespace = false
        srcBufPos = curPos
    }

    /** Push attribute delimited by whitespace */
    private fun pushWSDelimAttrValue() {
        var bufCount = srcBufCount
        var leftEnd = minOf(bufCount, BUF_SIZE)
        var left: Int
        var right: Int
        var curPos = srcBufPos
        var notFinished = true

        outer@ while (curPos < bufCount && notFinished) { // loop through all buffer iterations
            left = curPos
            right = -1

            inner@ while (curPos < leftEnd) {
                when (bufLeft[curPos]) {
                    '\r' -> {
                        srcBufPos = curPos
                        if (peek() == '\n'.code) {
                            ++srcBufPos
                            ++offset
                        }
                        right = curPos
                        curPos = srcBufPos
                        notFinished = false
                        break@inner
                    }

                    ' ', '\t', '\n', '>' -> {
                        right = curPos
                        ++curPos
                        notFinished = false
                        break@inner
                    }

                    '&' -> when (left) {
                        curPos -> { // start with entity
                            pushEntity()
                            curPos = srcBufPos
                        }

                        else -> { // read all items before entity (then after it will hit the other case)
                            right = curPos
                            break@inner
                        }
                    }

                    else -> ++curPos
                }
            }
            if (right > 0) {
                pushRange(bufLeft, left, right) // ws delimited is never WS
            }

            if (curPos == BUF_SIZE) { // swap the buffers
                srcBufPos = curPos
                swapInputBuffer()
                curPos = srcBufPos
                bufCount = srcBufCount
                leftEnd = minOf(bufCount, BUF_SIZE)
            }
        }
        srcBufPos = curPos
    }

    private fun read(s: String) {
        for (c in s) {
            val d = read()
            if (c.code != d) error("Found unexpected character '$d' while parsing '$s'")
        }
    }

    private fun read(c: Char) {
        val a = read()
        if (a != c.code) error("expected: '" + c + "' actual: '" + a.toChar() + "'")
    }

    private fun readAssert(c: Char) {
        /*val a = */read()
//        assert(a == c.code) { "This should have parsed as '$c', but was '${a.toChar()}'" }
    }

    private fun read(): Int {
        val pos = srcBufPos
        if (pos >= srcBufCount) return -1
        if (pos + 2 >= BUF_SIZE) return readAcross()

        val next = pos + 1
        when (val ch = bufLeft[pos]) {
            '\r' -> {
                if (next < srcBufCount && bufLeft[next] == '\n') {
                    srcBufPos = next + 1
                    incLine(2)
                } else {
                    srcBufPos = next
                    incLine()
                }
                return '\n'.code
            }

            '\n' -> {
                srcBufPos = next
                incLine()
                return '\n'.code
            }

            else -> {
                incCol()

                srcBufPos = next
                return ch.code
            }
        }
    }

    private fun readAndPush(): Char {
        val pos = srcBufPos
        if (pos >= srcBufCount) exception(UNEXPECTED_EOF)

        val nextSrcPos = pos + 1
        if (nextSrcPos >= BUF_SIZE) { // +1 to also account for CRLF across the boundary
            return readAcross().also(::pushChar).toChar() // use the slow path for this case
        }

        var outRight = outputBufRight
        if (outRight >= outputBuf.size) {
            growOutputBuf(outRight - outputBufLeft)
        }

        val bufLeft = bufLeft

        val result: Char
        when (val ch = bufLeft[pos]) {
            '\r' -> {
                srcBufPos = when {
                    nextSrcPos < srcBufCount && bufLeft[nextSrcPos] == '\n' -> {
                        incLine(2)
                        nextSrcPos + 1
                    }

                    else -> {
                        incLine()
                        nextSrcPos
                    }
                }

                outputBuf[outRight++] = '\n'
                result = '\n'
            }

            '\n' -> {
                srcBufPos = nextSrcPos
                incLine()
                outputBuf[outRight++] = '\n' // it is
                result = '\n'
            }

            else -> {
                incCol()
                srcBufPos = nextSrcPos
                outputBuf[outRight++] = ch
                result = ch
            }
        }
        outputBufRight = outRight
        return result
    }

    private fun growOutputBuf(minNeeded: Int = outputBufRight) {
        val newSize = maxOf(outputBuf.size * 2, (minNeeded * 5) / 4)
        outputBuf = outputBuf.copyOf(newSize)
    }

    private fun swapInputBuffer() {
        val oldLeft = bufLeft
        bufLeft = bufRight
        bufRight = oldLeft
        srcBufPos -= BUF_SIZE
        val rightBufCount = srcBufCount - BUF_SIZE
        if (rightBufCount >= BUF_SIZE) {
            val newRead = reader.readUntilFullOrEOF(bufRight)
            srcBufCount = when {
                newRead < 0 -> rightBufCount
                else -> rightBufCount + newRead
            }
        } else {
            srcBufCount = rightBufCount
        }
    }

    private fun readAcross(): Int {
        var pos = srcBufPos
        if (pos >= BUF_SIZE) {
            swapInputBuffer()
            pos -= BUF_SIZE
        }

        val next = pos + 1
        when (val ch = bufLeft[pos]) {
            '\u0000' -> { // should not happen at end of file (or really generally at all)
                srcBufPos = next
                return readAcross() // just recurse
            }

            '\r' -> {
                bufLeft[srcBufPos] = '\n'
                if (next < srcBufCount && getBuf(next) == '\n') {
                    setBuf(next, '\u0000')
                    srcBufPos = next + 1
                    incLine(2)
                } else {
                    srcBufPos = next
                    incLine()
                }
                return '\n'.code
            }

            '\n' -> {
                srcBufPos = next
                incLine()
                return '\n'.code
            }

            else -> {
                incCol()
                srcBufPos = next
                return ch.code
            }
        }
    }

    /** Does never read more than needed  */
    private fun peek(pos: Int): Int {
        // In this case we *may* need the right buffer, otherwise not
        // optimize this implementation for the "happy" path
        if (srcBufPos + (pos shl 2 + 1) >= BUF_SIZE) return peekAcross(pos)
        var current = srcBufPos
        var peekCount = pos

        while (current < srcBufCount) {
            var chr: Char = bufLeft[current]
            when (chr) {
                '\r' -> {
                    chr = '\n' // update the char
                    bufLeft[current] = '\n' // replace it with LF (\n)
                    if (bufLeft[current + 1] == '\r') {
                        // Note also as we are separated from the edge of the buffer setting this is valid even
                        // beyond the end of the file
                        bufLeft[current++] = '\u0000' // 0 is not a valid XML CHAR, so we can skip it
                    }
                }

                else -> ++current
            }
            if (peekCount-- == 0) return chr.code
        }
        return -1
    }

    /** Does never read more than needed  */
    private fun peek(): Int {
        // In this case we *may* need the right buffer, otherwise not
        // optimize this implementation for the "happy" path
        val current = srcBufPos
        if (current >= srcBufCount) return -1
        if (current >= BUF_SIZE) return peekAcross(0)

        return when (val chr: Char = bufLeft[current]) {
            '\r' -> '\n'.code
            else -> chr.code
        }
    }

    /**
     * Pessimistic implementation of peek that allows checks across into the "right" buffer
     */
    private fun peekAcross(pos: Int): Int {
        var current = srcBufPos
        var peekCount = pos

        while (current < srcBufCount) {
            var chr: Char = getBuf(current)
            when (chr) {
                '\r' -> {
                    chr = '\n' // update the char
                    if (current + 1 < srcBufCount && getBuf(current + 1) == '\n') {
                        current += 2
                    } else {
                        ++current
                    }
                }

                else -> ++current
            }
            if (peekCount-- == 0) return chr.code
        }
        return -1
    }

    private fun getBuf(pos: Int): Char {
        val split = pos - BUF_SIZE
        return when {
            split < 0 -> bufLeft[pos]
            else -> bufRight[split]
        }
    }

    private fun setBuf(pos: Int, value: Char) {
        val split = pos - BUF_SIZE
        when {
            split < 0 -> bufLeft[pos] = value
            else -> bufRight[split] = value
        }
    }

    @Suppress("DuplicatedCode")
    private fun readName(): String {
        var left = srcBufPos

        var bufEnd: Int
        run {
            val cnt = srcBufCount
            if (BUF_SIZE < cnt) {
                if (left == BUF_SIZE) {
                    swapInputBuffer()
                    left = 0
                    bufEnd = minOf(BUF_SIZE, srcBufCount)
                } else {
                    bufEnd = BUF_SIZE
                }
            } else {
                if (left >= cnt) exception(UNEXPECTED_EOF)
                bufEnd = cnt
            }
        }

        var srcBuf = bufLeft

        if (!isNameStartChar(srcBuf[left])) error("name expected, found: $srcBuf[left]")

        var right = left + 1

        while (true) {
            if (right == bufEnd) {
                pushRange(srcBuf, left, right)
                if (bufEnd >= srcBufCount) error(UNEXPECTED_EOF)
                srcBufPos = right // this is not technically needed, but this should be infrequent anytime
                swapInputBuffer()
                bufEnd = minOf(BUF_SIZE, srcBufCount)
                if (bufEnd == 0) break // end of file
                left = 0
                right = 0
                srcBuf = bufLeft
            }
            when {
                isNameChar11(srcBuf[right]) -> right += 1
                else -> {
                    pushRange(srcBuf, left, right)
                    break
                }
            }
        }
        srcBufPos = right
        return get()
    }

    @Suppress("DuplicatedCode")
    private fun readCName() {
        var left = srcBufPos

        var bufEnd: Int
        run {
            val cnt = srcBufCount
            if (BUF_SIZE < cnt) {
                if (left == BUF_SIZE) {
                    swapInputBuffer()
                    left = 0
                    bufEnd = minOf(BUF_SIZE, srcBufCount)
                } else {
                    bufEnd = BUF_SIZE
                }
            } else {
                if (left >= cnt) exception(UNEXPECTED_EOF)
                bufEnd = cnt
            }
        }

        var srcBuf = bufLeft

        srcBuf[left].let { c ->
            if (c == ':' || !isNameStartChar(c)) error("name expected, found: $c")
        }

        var right = left + 1

        var prefix: String? = null

        while (true) {
            if (right == bufEnd) {
                pushRange(srcBuf, left, right)
                if (bufEnd >= srcBufCount) error(UNEXPECTED_EOF)
                srcBufPos = right // this is not technically needed, but this should be infrequent anytime
                swapInputBuffer()
                bufEnd = minOf(BUF_SIZE, srcBufCount)
                if (bufEnd == 0) break // end of file
                left = 0
                right = 0
                srcBuf = bufLeft
            }
            when (val c = srcBuf[right]) {
                ':' -> if (PROCESS_NAMESPACES) {
                    pushRange(srcBuf, left, right)
                    right += 1
                    left = right
                    prefix = get()
                    resetOutputBuffer()
                } else {
                    right += 1
                }

                else -> when {
                    isNameChar11(c) -> right += 1
                    else -> {
                        pushRange(srcBuf, left, right)
                        break
                    }
                }
            }
        }
        srcBufPos = right
        readPrefix = prefix
        readLocalname = get()
    }

    private fun skip() {
        while (true) {
            val c = peek()
            if (c == -1 || !isXmlWhitespace(c.toChar())) break // More sane

            readAssert(c.toChar())
        }
    }


    override fun getNamespacePrefix(namespaceUri: String): String? {
        return namespaceHolder.getPrefix(namespaceUri)
    }

    override fun getNamespaceURI(prefix: String): String? {
        return namespaceHolder.getNamespaceUri(prefix)
    }

    private fun getPositionDescription(): String {
        val et = this._eventType ?: return ("<!--Parsing not started yet-->")

        val buf = StringBuilder(et.name)
        buf.append(' ')
        when {
            et == START_ELEMENT || et == END_ELEMENT -> {
                if (isSelfClosing) buf.append("(empty) ")
                buf.append('<')
                if (et == END_ELEMENT) buf.append('/')
                if (elementStack[depth].prefix != null) buf.append("{$namespaceURI}$prefix:")
                buf.append(name)

                for (x in 0 until attributeCount) {
                    buf.append(' ')
                    val a = attribute(x)
                    if (a.namespace != null) {
                        buf.append('{').append(a.namespace).append('}').append(a.prefix).append(':')
                    }
                    buf.append("${a.localName}='${a.value}'")
                }

                buf.append('>')
            }

            et == IGNORABLE_WHITESPACE -> {}

            et != TEXT -> buf.append(text)

            isWhitespace -> buf.append(
                "(whitespace)"
            )

            else -> { // nonwhitespace text
                var textCpy = text
                if (textCpy.length > 16) textCpy = textCpy.substring(0, 16) + "..."
                buf.append(textCpy)
            }
        }
        if (offset >= 0) {
            buf.append("@$line:$column [$offset] in ")
        }
        buf.append(reader.toString())
        return buf.toString()
    }

    override fun toString(): String {
        return "KtXmlReader [${getPositionDescription()}]"
    }

    @Deprecated(
        "Use extLocationInfo as that allows more detailed information",
        replaceWith = ReplaceWith("extLocationInfo?.toString()")
    )
    override val locationInfo: String
        get() = if (offset >= 0) "$line:$column" else "<unknown>"

    override val extLocationInfo: XmlReader.LocationInfo
        get() = XmlReader.ExtLocationInfo(col = column, line = line, offset = offset)

    public fun getLineNumber(): Int {
        return line
    }

    public fun getColumnNumber(): Int {
        return column
    }

    override fun isWhitespace(): Boolean = when (eventType) {
        TEXT, IGNORABLE_WHITESPACE -> isWhitespace
        CDSECT -> false
        else -> exception(ILLEGAL_TYPE)
    }

    override val text: String
        get() = when {
            eventType.isTextElement -> get()
            else -> throw XmlException("The element is not text, it is: $eventType")
        }

    override val piTarget: String
        get() {
            check(eventType == PROCESSING_INSTRUCTION)
            return get().substringBefore(' ')
        }

    override val piData: String
        get() {
            check(eventType == PROCESSING_INSTRUCTION)
            return get().substringAfter(' ', "")
        }

    public fun isEmptyElementTag(): Boolean {
        if (_eventType != START_ELEMENT) exception(ILLEGAL_TYPE)
        return isSelfClosing
    }

    override fun getAttributeNamespace(index: Int): String {
        return attribute(index).namespace!!
    }

    override fun getAttributeLocalName(index: Int): String {
        return attribute(index).localName!!
    }

    override fun getAttributePrefix(index: Int): String {
        return attribute(index).prefix ?: ""
    }

    override fun getAttributeValue(index: Int): String {
        return attribute(index).value!!
    }

    override fun getAttributeValue(nsUri: String?, localName: String): String? {
        for (attrIdx in 0 until attributeCount) {
            val attr = attribute(attrIdx)
            if (attr.localName == localName && (nsUri == null || attr.namespace == nsUri)) {
                return attr.value
            }
        }
        return null
    }

    override fun next(): EventType {
        isWhitespace = true

        // reset the output buffer
        resetOutputBuffer()

        when (state) {
            State.BEFORE_START -> nextImplDocStart()

            State.START_DOC,
            State.DOCTYPE_DECL -> nextImplPreamble()

            State.BODY -> nextImplBody()
            State.POST -> nextImplPost()
            State.EOF -> error("Reading past end of file")
        }
//        assert((offset - srcBufPos) % BUF_SIZE == 0) { "Offset error: ($offset - $srcBufPos) % $BUF_SIZE != 0" }
        return eventType
    }

    override fun hasNext(): Boolean {
        return _eventType != END_DOCUMENT
    }

    override fun nextTag(): EventType {
        do {
            next()
        } while (_eventType?.isIgnorable == true || (_eventType == TEXT && isWhitespace))

        if (_eventType != END_ELEMENT && _eventType != START_ELEMENT) exception("unexpected type")
        return eventType
    }

    override fun require(type: EventType, namespace: String?, name: String?) {
        if (type != this._eventType || (namespace != null && namespace != elementStack[depth - 1].namespace)
            || (name != null && name != elementStack[depth - 1].localName)
        ) {
            exception("expected: $type {$namespace}$name, found: $_eventType {$namespaceURI}$localName")
        }
    }

    private companion object {
        const val UNEXPECTED_EOF = "Unexpected EOF"
        const val ILLEGAL_TYPE = "Wrong event type"

        const val PROCESS_NAMESPACES = true

        @JvmStatic
        private fun fullname(prefix: String?, localName: String): String = when (prefix) {
            null -> localName
            else -> "$prefix:$localName"
        }

        @JvmStatic
        private fun Reader.readUntilFullOrEOF(buffer: CharArray): Int {
            val bufSize = buffer.size
            var totalRead: Int = read(buffer, 0, bufSize)
            if (totalRead < 0) return -1
            while (totalRead < bufSize) {
                val lastRead = read(buffer, totalRead, bufSize - totalRead)
                if (lastRead < 0) return totalRead
                totalRead += lastRead
            }
            return totalRead
        }
    }

    private var elementData: Array<String?> = arrayOfNulls(48)

    private inner class ElementStack {

        operator fun get(idx: Int) = element(idx)

        fun ensureCapacity(required: Int) {
            val requiredCapacity = required * 3 // three slots per element
            if (elementData.size >= requiredCapacity) return

            elementData = elementData.copyOf(requiredCapacity + 12)
        }

    }

    private fun element(idx: Int) = ElementDelegate(idx)

    @JvmInline
    private value class ElementDelegate(val index: Int)

    private var ElementDelegate.namespace: String?
        get() {
            if (index >= depth) throw IndexOutOfBoundsException()
            return elementData[index * 3]
        }
        set(value) {
            elementData[index * 3] = value
        }

    private var ElementDelegate.prefix: String?
        get() {
            if (index >= depth) throw IndexOutOfBoundsException()
            return elementData[index * 3 + 1]
        }
        set(value) {
            elementData[index * 3 + 1] = value
        }

    private var ElementDelegate.localName: String?
        get() {
            if (index >= depth) throw IndexOutOfBoundsException()
            return elementData[index * 3 + 2]
        }
        set(value) {
            elementData[index * 3 + 2] = value
        }

    private inner class AttributesCollection {

        fun clear() {
            val oldSize = attributeCount
            if (oldSize > 0) {
                attrData.fill(null, 0, oldSize * 4)
            }
            attributeCount = 0
        }

        fun shrink(newSize: Int) {
            attrData.fill(null, newSize * 4, attributeCount * 4)
            attributeCount = newSize
        }

        fun ensureCapacity(required: Int) {
            val requiredSize = required * 4
            val oldData = attrData
            if (oldData.size >= requiredSize) return

            attrData = oldData.copyOf(requiredSize + 16)
        }

        fun addNsUnresolved(attrPrefix: String?, attrLocalName: String, attrValue: String) {
            val oldSize = attributeCount
            val newSize = if (oldSize < 0) 1 else oldSize + 1
            attributeCount = newSize

            ensureCapacity(newSize)
            var i = newSize * 4 - 4

            val d = attrData
            d[i++] = null
            d[i++] = attrPrefix
            d[i++] = attrLocalName
            d[i] = attrValue
        }

        fun copyNotNs(fromIdx: Int, toIdx: Int) {
            attrData.copyInto(attrData, toIdx * 4 + 1, fromIdx * 4 + 1, fromIdx * 4 + 4)
        }
    }

    @JvmInline
    private value class AttributeDelegate(val index: Int)

    private fun attribute(index: Int): AttributeDelegate = AttributeDelegate(index)

    private var AttributeDelegate.namespace: String?
        get() {
            if (index >= attributeCount) throw IndexOutOfBoundsException()
            return attrData[index * 4]
        }
        set(value) {
            attrData[index * 4] = value
        }

    private var AttributeDelegate.prefix: String?
        get() {
            if (index >= attributeCount) throw IndexOutOfBoundsException()
            return attrData[index * 4 + 1]
        }
        set(value) {
            attrData[index * 4 + 1] = value
        }

    private var AttributeDelegate.localName: String?
        get() {
            if (index >= attributeCount) throw IndexOutOfBoundsException()
            return attrData[index * 4 + 2]
        }
        set(value) {
            attrData[index * 4 + 2] = value
        }

    private var AttributeDelegate.value: String?
        get() {
            if (index >= attributeCount) throw IndexOutOfBoundsException()
            return attrData[index * 4 + 3]
        }
        set(value) {
            attrData[index * 4 + 3] = value
        }


    private enum class State {
        /** Parsing hasn't started yet */
        BEFORE_START,

        /** At or past parsing the xml header */
        START_DOC,

        /** At or past parsing the document type definition */
        DOCTYPE_DECL,

        /** Parsing the main document element */
        BODY,

        /** At end of main document element end tag, or after it*/
        POST,

        /** At end of file */
        EOF
    }

}
