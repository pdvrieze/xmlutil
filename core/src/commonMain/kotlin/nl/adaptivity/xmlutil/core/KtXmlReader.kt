/*
 * Copyright (c) 2021.
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
import nl.adaptivity.xmlutil.core.impl.NamespaceHolder
import nl.adaptivity.xmlutil.core.impl.isXmlWhitespace
import nl.adaptivity.xmlutil.core.impl.multiplatform.Reader
import kotlin.jvm.JvmInline
import kotlin.jvm.JvmStatic

@ExperimentalXmlUtilApi
public class KtXmlReader internal constructor(
    private val reader: Reader,
    encoding: String?,
    public val relaxed: Boolean = false
) : XmlReader {

    public constructor(reader: Reader, relaxed: Boolean = false) : this(reader, null, relaxed)

    private var line = 1
    private var column = 0

    private var _eventType: EventType? = null //START_DOCUMENT // Already have this state
    public override val eventType: EventType
        get() = _eventType ?: throw IllegalStateException("Not yet started")

    override val isStarted: Boolean
        get() = _eventType != null

    private var entityName: String? = null

    public override val localName: String
        get() = when (_eventType) {
            ENTITY_REF -> entityName ?: throw XmlException("Missing entity name")
            START_ELEMENT, END_ELEMENT -> elementStack[depth - 1].localName ?: throw XmlException("Missing local name")
            else -> throw IllegalStateException("Local name not accessible outside of element tags")
        }

    public override val namespaceURI: String
        get() = when (_eventType) {
            START_ELEMENT, END_ELEMENT -> elementStack[depth - 1].namespace ?: throw XmlException("Missing namespace")
            else -> throw IllegalStateException("Local name not accessible outside of element tags")
        }

    public override val prefix: String
        get() = when (_eventType) {
            START_ELEMENT, END_ELEMENT -> elementStack[depth - 1].prefix ?: throw XmlException("Missing prefix")
            else -> throw IllegalStateException("Local name not accessible outside of element tags")
        }

    private var isSelfClosing = false

    override val attributeCount: Int
        get() = attributes.size

    private var attributes: AttributesCollection = AttributesCollection()

    override var encoding: String? = encoding
        private set

    public override var version: String? = null

    public override var standalone: Boolean? = null

    private val srcBuf = CharArray(8192)

    private var srcBufPos: Int = 0

    private var srcBufCount: Int = 0

    /**
     * A separate peek buffer seems simpler than managing
     * wrap around in the first level read buffer
     */
    private val peek = IntArray(2)
    private var peekCount = 0

    private var entityMap = HashMap<String, String>().also {
        it["amp"] = "&"
        it["apos"] = "'"
        it["gt"] = ">"
        it["lt"] = "<"
        it["quot"] = "\""
    }

    private val namespaceHolder = NamespaceHolder()

    public override val depth: Int
        get() = namespaceHolder.depth

    private var elementStack: ElementStack = ElementStack()

    /** Target buffer for storing incoming text (including aggregated resolved entities)  */
    private var txtBuf = CharArray(128)

    /** Write position   */
    private var txtBufPos = 0

    private var isWhitespace = false

    //    private int stackMismatch = 0;
    private var error: String? = null

    private var wasCR = false

    private var unresolved = false
    private var token = false

    override val namespaceDecls: List<Namespace>
        get() = namespaceHolder.namespacesAtCurrentDepth

    override val namespaceContext: IterableNamespaceContext
        get() = namespaceHolder.namespaceContext

    override fun close() {
        //NO-Op
    }

    private fun adjustNsp(fullName: String): Boolean {
        var hasActualAttributeWithPrefix = false

        // Loop through all attributes to collect namespace attributes and split name into prefix/localName.
        // Namespaces will not be set yet (as the namespace declaring attribute may be afterwards)
        var attrIdx = 0
        while (attrIdx < (attributes.size)) {
            val attr = attributes[attrIdx]

            var attrName: String? = attr.localName
            val cIndex = attrName!!.indexOf(':')
            var prefix: String
            if (cIndex >= 0) {
                prefix = attrName.substring(0, cIndex)
                attrName = attrName.substring(cIndex + 1)
            } else if (attrName == "xmlns") {
                prefix = attrName
                attrName = null
            } else {
                attr.namespace = XMLConstants.NULL_NS_URI // the namespace for the attribute must be the null namespace
                attr.prefix = XMLConstants.DEFAULT_NS_PREFIX
                attrIdx += 1
                continue
            }
            if (prefix != "xmlns") {
                hasActualAttributeWithPrefix = true
                attrIdx += 1
            } else {
                namespaceHolder.addPrefixToContext(attrName, attributes[attrIdx].value)
                if (attrName != null && attributes[attrIdx].value == "") error("illegal empty namespace")

                //  prefixMap = new PrefixMap (prefixMap, attrName, attr.getValue ());
                attributes.removeAttr(attr)
            }
        }
        if (hasActualAttributeWithPrefix) {
            var i = attributes.size - 1
            while (i >= 0) {
                var attrName = attributes[i].localName!!
                val cIndex = attrName.indexOf(':')
                if (cIndex == 0 && !relaxed) {
                    throw RuntimeException("illegal attribute name: $attrName at $this")
                } else if (cIndex != -1) {
                    val attrPrefix = attrName.substring(0, cIndex)
                    attrName = attrName.substring(cIndex + 1)
                    val attrNs = namespaceHolder.getNamespaceUri(attrPrefix)
                    if (attrNs == null && !relaxed) throw RuntimeException("Undefined Prefix: $attrPrefix in $this")
                    attributes[i].namespace = attrNs
                    attributes[i].prefix = attrPrefix
                    attributes[i].localName = attrName

                }
                i -= 1
            }
        }

        val cIdx = fullName.indexOf(':')
        if (cIdx == 0) error("illegal tag name: $fullName")
        val prefix: String
        val localName: String
        if (cIdx != -1) {
            prefix = fullName.substring(0, cIdx)
            localName = fullName.substring(cIdx + 1)
        } else {
            prefix = ""
            localName = fullName
        }

        val ns = namespaceHolder.getNamespaceUri(prefix) ?: run {
            if (cIdx >= 0) error("undefined prefix: $prefix")
            XMLConstants.NULL_NS_URI
        }
        val d = depth - 1
        elementStack[d].prefix = prefix
        elementStack[d].localName = localName
        elementStack[d].namespace = ns

        return hasActualAttributeWithPrefix
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

    /**
     * common base for next and nextToken. Clears the state, except from
     * txtPos and whitespace. Does not set the type variable  */
    private fun nextImpl() {
        if (_eventType == END_ELEMENT) namespaceHolder.decDepth()

        while (true) {
            attributes.clear()

            // degenerated needs to be handled before error because of possible
            // processor expectations(!)
            if (isSelfClosing) {
                isSelfClosing = false
                _eventType = END_ELEMENT
                return
            }
            error?.let { e ->
                for (element in e) push(element.code)

                this.error = null
                _eventType = COMMENT
                return
            }

            //            text = null;
            _eventType = peekType()
            when (_eventType) {
                START_DOCUMENT -> return // just return, no special things here
                ENTITY_REF -> {
                    pushEntity()
                    return
                }
                START_ELEMENT -> {
                    parseStartTag(false)
                    return
                }
                END_ELEMENT -> {
                    parseEndTag()
                    return
                }
                END_DOCUMENT -> return
                TEXT -> {
                    pushText('<'.code, !token)
                    if (isWhitespace) _eventType = IGNORABLE_WHITESPACE
/*
                    if (depth == 0) {
                        // make exception switchable for instances.chg... !!!!
                        //	else
                        //    exception ("text '"+getText ()+"' not allowed outside root element");
                    }
*/
                    return
                }
                else -> {
                    _eventType = parseLegacy(token)
                    if (_eventType != START_DOCUMENT) return
                }
            }
        }
    }

    private fun parseLegacy(push: Boolean): EventType {
        var localPush = push
        val expected: String
        val term: Int
        val result: EventType
        var prev = 0
        read() // <
        var c = read()
        if (c == '?'.code) {
            if ((peek(0) == 'x'.code || peek(0) == 'X'.code)
                && (peek(1) == 'm'.code || peek(1) == 'M'.code)
            ) {
                if (localPush) {
                    push(peek(0))
                    push(peek(1))
                }
                read()
                read()
                if ((peek(0) == 'l'.code || peek(0) == 'L'.code) && peek(1) <= ' '.code) {
                    if (line != 1 || column > 4) error("PI must not start with xml")
                    parseStartTag(true)
                    if (attributeCount < 1 || "version" != attributes[0].localName) error("version expected")
                    version = attributes[0].value
                    var pos = 1
                    if (pos < attributeCount && "encoding" == attributes[1].localName) {
                        encoding = attributes[1].value
                        pos++
                    }
                    if (pos < attributeCount && "standalone" == attributes[pos].localName
                    ) {
                        val st = attributes[pos].value
                        when (st) {
                            "yes" -> standalone = true
                            "no" -> standalone = false
                            else -> error("illegal standalone value: $st")
                        }
                        pos++
                    }
                    if (pos != attributeCount) error("illegal xmldecl")
                    isWhitespace = true
                    txtBufPos = 0
                    return START_DOCUMENT
                }
            }

            /*            int c0 = read ();
                        int c1 = read ();
                        int */
            term = '?'.code
            result = PROCESSING_INSTRUCTION
            expected = ""
        } else if (c == '!'.code) {
            if (peek(0) == '-'.code) {
                result = COMMENT
                expected = "--"
                term = '-'.code
            } else if (peek(0) == '['.code) {
                result = CDSECT
                expected = "[CDATA["
                term = ']'.code
                localPush = true
            } else {
                result = DOCDECL
                expected = "DOCTYPE"
                term = -1
            }
        } else {
            error("illegal: <$c")
            return COMMENT
        }
        for (ch in expected) read(ch)
        if (result == DOCDECL) parseDoctype(localPush) else {
            while (true) {
                c = read()
                if (c == -1) {
                    error(UNEXPECTED_EOF)
                    return COMMENT
                }
                if (localPush) push(c)
                if ((term == '?'.code || c == term)
                    && peek(0) == term && peek(1) == '>'.code
                ) break
                prev = c
            }
            if (term == '-'.code && prev == '-'.code && !relaxed) error("illegal comment delimiter: --->")
            read()
            read()
            if (localPush && term != '?'.code) txtBufPos--
        }
        return result
    }

    /** precondition: &lt! consumed  */
    private fun parseDoctype(push: Boolean) {
        var nesting = 1
        var quoted = false

        // read();
        while (true) {
            val i = read()
            when (i) {
                -1 -> {
                    error(UNEXPECTED_EOF)
                    return
                }
                '\''.code -> quoted = !quoted
                '<'.code -> if (!quoted) nesting++
                '>'.code -> if (!quoted) {
                    if (--nesting == 0) return
                }
            }
            if (push) push(i)
        }
    }

    /* precondition: &lt;/ consumed */

    /* precondition: &lt;/ consumed */
    private fun parseEndTag() {
        read() // '<'
        read() // '/'
        val fullName = readName()
        skip()
        read('>')
        val spIdx = depth - 1
        if (depth == 0) {
            error("element stack empty")
            _eventType = COMMENT
            return
        }
        if (!relaxed) {
            val expectedPrefix = elementStack[spIdx].prefix ?: exception("Missing prefix")
            val expectedLocalName = elementStack[spIdx].localName ?: exception("Missing localname")
            val expectedFullname = when {
                expectedPrefix.isEmpty() -> expectedLocalName
                else -> "$expectedPrefix:$expectedLocalName"
            }

            if (fullName != expectedFullname) {
                error("expected: /${elementStack[spIdx].fullName} read: $fullName")
            }
        }
    }

    private fun peekType(): EventType {
        if (_eventType==null) return START_DOCUMENT
        return when (peek(0)) {
            -1 -> END_DOCUMENT
            '&'.code -> ENTITY_REF
            '<'.code -> when (peek(1)) {
                '/'.code -> END_ELEMENT
                '?'.code -> PROCESSING_INSTRUCTION
                '!'.code -> COMMENT
                else -> START_ELEMENT
            }
            else -> TEXT
        }
    }

    private operator fun get(pos: Int): String {
        return txtBuf.concatToString(pos, pos + (txtBufPos - pos))
    }

    private fun push(c: Int) {
        isWhitespace = isWhitespace and c.isXmlWhitespace()
        if (txtBufPos + 1 >= txtBuf.size) { // +1 to have enough space for 2 surrogates, if needed
            txtBuf = txtBuf.copyOf(txtBufPos * 4 / 3 + 4)
        }
        if (c > 0xffff) {
            // write high Unicode value as surrogate pair
            val offset = c - 0x010000
            txtBuf[txtBufPos++] = ((offset ushr 10) + 0xd800).toChar() // high surrogate
            txtBuf[txtBufPos++] = ((offset and 0x3ff) + 0xdc00).toChar() // low surrogate
        } else {
            txtBuf[txtBufPos++] = c.toChar()
        }
    }

    /** Sets name and attributes  */
    private fun parseStartTag(xmldecl: Boolean) {
        if (!xmldecl) read()
        val fullName = readName()
        attributes.clear(0)
        while (true) {
            skip()
            val c = peek(0)
            if (xmldecl) {
                if (c == '?'.code) {
                    read()
                    read('>')
                    return
                }
            } else {
                if (c == '/'.code) {
                    isSelfClosing = true
                    read()
                    skip()
                    read('>')
                    break
                }
                if (c == '>'.code && !xmldecl) {
                    read()
                    break
                }
            }
            if (c == -1) {
                error(UNEXPECTED_EOF)
                //type = COMMENT;
                return
            }
            val attrName = readName()
            if (attrName.isEmpty()) {
                error("attr name expected")
                //type = COMMENT;
                break
            }
            skip()
            if (peek(0) != '='.code) {
                if (!relaxed) {
                    error("Attr.value missing f. $attrName")
                }
                attributes.addNoNS(attrName, attrName)
            } else {
                read('=')
                skip()
                var delimiter = peek(0)
                if (delimiter != '\''.code && delimiter != '"'.code) {
                    if (!relaxed) {
                        error("attr value delimiter missing!")
                    }
                    delimiter = ' '.code
                } else read()
                val p = txtBufPos
                pushText(delimiter, true)
                attributes.addNoNS(attrName, get(p))

                txtBufPos = p
                if (delimiter != ' '.code) read() // skip endquote
            }
        }

        val d = depth
        namespaceHolder.incDepth()
        elementStack.ensureCapacity(depth)

        elementStack[d].fullName = fullName

        if (processNsp) {
            adjustNsp(fullName)
        } else {
            elementStack[d].namespace = ""
            elementStack[d].prefix = ""
            elementStack[d].localName = fullName
        }
    }

    /**
     * result: isWhitespace; if the setName parameter is set,
     * the name of the entity is stored in "name"
     */
    private fun pushEntity() {
        push(read()) // &
        val pos = txtBufPos
        while (true) {
            val c = peek(0)
            if (c == ';'.code) {
                read()
                break
            }
            if (c < 128 && (c < '0'.code || c > '9'.code)
                && (c < 'a'.code || c > 'z'.code)
                && (c < 'A'.code || c > 'Z'.code)
                && c != '_'.code && c != '-'.code && c != '#'.code
            ) {
                if (!relaxed) {
                    error("unterminated entity ref")
                }
                println("broken entitiy: " + get(pos - 1))

                return
            }
            push(read())
        }
        val code = get(pos)
        txtBufPos = pos - 1
        if (token && _eventType == ENTITY_REF) {
            entityName = code
        }
        if (code[0] == '#') {
            val c = if (code[1] == 'x') code.substring(2).toInt(16) else code.substring(1).toInt()
            push(c)
            return
        }
        val result = entityMap.get(code)
        unresolved = result == null
        if (result == null) {
            if (!token) error("unresolved: &$code;")
        } else {
            for (i in 0 until result.length) push(result[i].code)
        }
    }

    /** types:
     * '<': parse to any token (for nextToken ())
     * '"': parse to quote
     * ' ': parse to whitespace or '>'
     */
    private fun pushText(delimiter: Int, resolveEntities: Boolean) {
        var next = peek(0)
        var cbrCount = 0
        while (next != -1 && next != delimiter) { // covers eof, '<', '"'
            if (delimiter == ' '.code) if (next.isXmlWhitespace() || next == '>'.code) break
            if (next == '&'.code) {
                if (!resolveEntities) break
                pushEntity()
            } else if (next == '\n'.code && _eventType == START_ELEMENT) {
                read()
                push(' '.code)
            } else push(read())
            if (next == '>'.code && cbrCount >= 2 && delimiter != ']'.code) error("Illegal: ]]>")
            if (next == ']'.code) cbrCount++ else cbrCount = 0
            next = peek(0)
        }
    }

    private fun read(c: Char) {
        val a = read()
        if (a != c.code) error("expected: '" + c + "' actual: '" + a.toChar() + "'")
    }

    private fun read(): Int {
        val result: Int
        if (peekCount == 0) result = peek(0) else {
            result = peek[0]
            peek[0] = peek[1]
        }

        peekCount--
        column++
        if (result == '\n'.code) {
            line++
            column = 1
        }
        return result
    }

    /** Does never read more than needed  */
    private fun peek(pos: Int): Int {
        while (pos >= peekCount) {
            var nw: Int
            when {
                srcBuf.size <= 1 -> nw = reader.read()
                srcBufPos < srcBufCount -> nw = srcBuf[srcBufPos++].code
                else -> {
                    srcBufCount = reader.read(srcBuf, 0, srcBuf.size)
                    nw = if (srcBufCount <= 0) -1 else srcBuf[0].code
                    srcBufPos = 1
                }
            }
            if (nw == '\r'.code) {
                wasCR = true
                peek[peekCount++] = '\n'.code
            } else {
                if (nw == '\n'.code) {
                    if (!wasCR) peek[peekCount++] = '\n'.code
                } else peek[peekCount++] = nw
                wasCR = false
            }
        }
        return peek[pos]
    }

    private fun readName(): String {
        val pos = txtBufPos
        var c = peek(0)
        if ((c < 'a'.code || c > 'z'.code)
            && (c < 'A'.code || c > 'Z'.code)
            && c != '_'.code && c != ':'.code && c < 0x0c0 && !relaxed
        ) error("name expected")
        do {
            push(read())
            c = peek(0)
        } while (c >= 'a'.code && c <= 'z'.code
            || c >= 'A'.code && c <= 'Z'.code
            || c >= '0'.code && c <= '9'.code
            || c == '_'.code || c == '-'.code || c == ':'.code || c == '.'.code || c >= 0x0b7
        )
        val result = get(pos)
        txtBufPos = pos
        return result
    }

    private fun skip() {
        while (true) {
            val c = peek(0)
            if (c > ' '.code || c == -1) break // More sane
            read()
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

                for (x in 0 until attributes.size) {
                    buf.append(' ')
                    val a = attributes[x]
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
            else -> {
                var textCpy = text
                if (textCpy.length > 16) textCpy = textCpy.substring(0, 16) + "..."
                buf.append(textCpy)
            }
        }
        buf.append("@$line:$column in ")
        buf.append(reader.toString())
        return buf.toString()
    }

    override fun toString(): String {
        return "KtXmlReader [${getPositionDescription()}]"
    }

    override val locationInfo: String
        get() = "$line:$column"

    public fun getLineNumber(): Int {
        return line
    }

    public fun getColumnNumber(): Int {
        return column
    }

    override fun isWhitespace(): Boolean {
        val et = eventType
        if (et != TEXT && et != IGNORABLE_WHITESPACE && et != CDSECT) exception(ILLEGAL_TYPE)
        return isWhitespace
    }

    override val text: String
        get() = when {
            eventType.isTextElement -> get(0)
            else -> throw XmlException("The element is not text, it is: $eventType")
        }

    public fun isEmptyElementTag(): Boolean {
        if (_eventType != START_ELEMENT) exception(ILLEGAL_TYPE)
        return isSelfClosing
    }

    override fun getAttributeNamespace(index: Int): String {
        return attributes[index].namespace!!
    }

    override fun getAttributeLocalName(index: Int): String {
        return attributes[index].localName!!
    }

    override fun getAttributePrefix(index: Int): String {
        return attributes[index].prefix!!
    }

    override fun getAttributeValue(index: Int): String {
        return attributes[index].value!!
    }

    override fun getAttributeValue(nsUri: String?, localName: String): String? {
        for (attrIdx in 0 until attributes.size) {
            val attr = attributes[attrIdx]
            if (attr.localName == localName && (nsUri == null || attr.namespace == nsUri)) {
                return attr.value
            }
        }
        return null
    }

    override fun next(): EventType {
        isWhitespace = true
        txtBufPos = 0
        token = true
        nextImpl()
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
        if (type != this._eventType || (namespace != null && namespace != elementStack[depth-1].namespace)
            || (name != null && name != elementStack[depth-1].localName)
        ) {
            exception("expected: $type {$namespace}$name, found: $_eventType {$namespaceURI}$localName")
        }
    }

    private companion object {
        const val UNEXPECTED_EOF = "Unexpected EOF"
        const val ILLEGAL_TYPE = "Wrong event type"

        const val processNsp = true
    }

    private class ElementStack {
        var data: Array<String?> = arrayOfNulls(16)
            private set

        operator fun get(idx: Int) = ElementDelegate(idx)

        fun ensureCapacity(required: Int) {
            val requiredCapacity = required * 4
            if (data.size >= requiredCapacity) return

            data = data.copyOf(requiredCapacity + 16)
        }

    }

    @JvmInline
    private value class ElementDelegate(val index: Int)

    private var ElementDelegate.namespace: String?
        get() {
            if (index !in 0..depth) throw IndexOutOfBoundsException()
            return elementStack.data[index * 4]
        }
        set(value) {
            elementStack.data[index * 4] = value
        }

    private var ElementDelegate.prefix: String?
        get() {
            if (index !in 0..depth) throw IndexOutOfBoundsException()
            return elementStack.data[index * 4 + 1]
        }
        set(value) {
            elementStack.data[index * 4 + 1] = value
        }

    private var ElementDelegate.localName: String?
        get() {
            if (index !in 0..depth) throw IndexOutOfBoundsException()
            return elementStack.data[index * 4 + 2]
        }
        set(value) {
            elementStack.data[index * 4 + 2] = value
        }

    private var ElementDelegate.fullName: String?
        get() {
            if (index !in 0..depth) throw IndexOutOfBoundsException()
            return elementStack.data[index * 4 + 3]
        }
        set(value) {
            elementStack.data[index * 4 + 3] = value
        }

    private class AttributesCollection {
        var data: Array<String?> = arrayOfNulls(16)
            private set

        var size: Int = 0
            private set

        operator fun get(index: Int): AttributeDelegate = AttributeDelegate(index)

        fun removeAttr(attr: AttributeDelegate) {
            data.copyInto(data, attr.index * 4, attr.index * 4 + 4, ((size--) * 4))
            data.fill(null, size * 4, size * 4 + 4)
        }

        fun clear(newSize: Int = -1) {
            if (size > 0) {
                data.fill(null, 0, size * 4)
            }
            size = newSize
        }

        fun ensureCapacity(required: Int) {
            val requiredSize = required * 4
            if (data.size >= requiredSize) return

            data = data.copyOf(requiredSize + 16)
        }

        fun addNoNS(attrName: String, attrValue: String) {
            size = if (size < 0) 1 else size + 1

            ensureCapacity(size)
            var i = size * 4 - 4

            data[i++] = ""
            data[i++] = null
            data[i++] = attrName
            data[i] = attrValue
        }
    }

    @JvmInline
    private value class AttributeDelegate(val index: Int)

    private var AttributeDelegate.namespace: String?
        get() {
            if (index !in 0..attributes.size) throw IndexOutOfBoundsException()
            return attributes.data[index * 4]
        }
        set(value) {
            attributes.data[index * 4] = value
        }

    private var AttributeDelegate.prefix: String?
        get() {
            if (index !in 0..attributes.size) throw IndexOutOfBoundsException()
            return attributes.data[index * 4 + 1]
        }
        set(value) {
            attributes.data[index * 4 + 1] = value
        }

    private var AttributeDelegate.localName: String?
        get() {
            if (index !in 0..attributes.size) throw IndexOutOfBoundsException()
            return attributes.data[index * 4 + 2]
        }
        set(value) {
            attributes.data[index * 4 + 2] = value
        }

    private var AttributeDelegate.value: String?
        get() {
            if (index !in 0..attributes.size) throw IndexOutOfBoundsException()
            return attributes.data[index * 4 + 3]
        }
        set(value) {
            attributes.data[index * 4 + 3] = value
        }


}
