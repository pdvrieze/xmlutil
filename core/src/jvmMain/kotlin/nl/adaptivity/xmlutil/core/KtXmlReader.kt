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
import java.io.InputStream
import java.io.InputStreamReader
import java.io.Reader

@ExperimentalXmlUtilApi
public class KtXmlReader private constructor(
    private var reader: Reader,
    encoding: String?,
    srcBuf: CharArray,
    srcBufPos: Int = 0,
    srcBufCount: Int = 0,
): XmlReader {

    public constructor(reader: Reader) : this(
        reader,
        null,
        CharArray(if (Runtime.getRuntime().freeMemory() >= 1048576) 8192 else 128)
    )

    private var line = 1
    private var column = 0

    private var _eventType: EventType = START_DOCUMENT // Already have this state
    public override val eventType: EventType
        get() = _eventType

    override var isStarted: Boolean = false
        private set

    private var _name: String? = null
    public override val localName: String
        get() = _name ?: throw XmlException("Missing name")

    private var _namespace: String? = null
    public override val namespaceURI: String
        get() = _namespace ?: throw XmlException("Missing namespace")

    private var isSelfClosing = false

    override var attributeCount: Int = -1
        private set

    private var attributes = arrayOfNulls<String>(16)

    override var encoding: String? = encoding
        private set

    public override var version: String? = null

    public override var standalone: Boolean? = null

    private var srcBuf = srcBuf

    private var srcBufPos: Int = srcBufPos

    private var srcBufCount: Int = srcBufCount

    /**
     * A separate peek buffer seems simpler than managing
     * wrap around in the first level read buffer
     */ // TODO review this
    private val peek = IntArray(2)
    private var peekCount = 0

    private var entityMap = HashMap<String, String>().also {
        it["amp"] = "&"
        it["apos"] = "'"
        it["gt"] = ">"
        it["lt"] = "<"
        it["quot"] = "\""
    }

    private fun setInput(reader: Reader) {
        this.reader = reader
        line = 1
        column = 0
        _eventType = START_DOCUMENT
        _name = null
        _namespace = null
        isSelfClosing = false
        attributeCount = -1
        encoding = null
        version = null
        standalone = null

        srcBufPos = 0
        srcBufCount = 0
        peekCount = 0

        entityMap = HashMap()
        entityMap["amp"] = "&"
        entityMap["apos"] = "'"
        entityMap["gt"] = ">"
        entityMap["lt"] = "<"
        entityMap["quot"] = "\""
    }

    private var location: Any? = null


    private val namespaceHolder = NamespaceHolder()

    public override val depth: Int
        get() = namespaceHolder.depth

    private var elementStack = arrayOfNulls<String>(16)




    /** Target buffer for storing incoming text (including aggregated resolved entities)  */
    private var txtBuf = CharArray(128)

    /** Write position   */
    private var txtBufPos = 0

    private var isWhitespace = false

    private var _prefix: String? = null
    public override val prefix: String
        get() = _prefix ?: throw XmlException("Missing prefix")

    //    private int stackMismatch = 0;
    private var error: String? = null

    private var wasCR = false

    private var unresolved = false
    private var token = false

    override val namespaceDecls: List<Namespace>
        get() = namespaceHolder.namespacesAtCurrentDepth

    override val namespaceContext: IterableNamespaceContext
        get() = namespaceHolder.namespaceContext


    public fun setInput(inputStream: InputStream, encoding: String? = null) {
        srcBufPos = 0
        srcBufCount = 0
        var enc = encoding

        try {
            if (enc == null) {
                // read four bytes
                var chk = 0
                while (srcBufCount < 4) {
                    val i: Int = inputStream.read()
                    if (i == -1) break
                    chk = chk shl 8 or i
                    srcBuf[srcBufCount++] = i.toChar()
                }
                if (srcBufCount == 4) {
                    when (chk) {
                        0x00000FEFF -> {
                            enc = "UTF-32BE"
                            srcBufCount = 0
                        }
                        -0x20000 -> {
                            enc = "UTF-32LE"
                            srcBufCount = 0
                        }
                        0x03c -> {
                            enc = "UTF-32BE"
                            srcBuf[0] = '<'
                            srcBufCount = 1
                        }
                        0x03c000000 -> {
                            enc = "UTF-32LE"
                            srcBuf[0] = '<'
                            srcBufCount = 1
                        }
                        0x0003c003f -> {
                            enc = "UTF-16BE"
                            srcBuf[0] = '<'
                            srcBuf[1] = '?'
                            srcBufCount = 2
                        }
                        0x03c003f00 -> {
                            enc = "UTF-16LE"
                            srcBuf[0] = '<'
                            srcBuf[1] = '?'
                            srcBufCount = 2
                        }
                        0x03c3f786d -> {
                            while (true) {
                                val i: Int = inputStream.read()
                                if (i == -1) break
                                srcBuf[srcBufCount++] = i.toChar()
                                if (i == '>'.code) {
                                    val s = String(srcBuf, 0, srcBufCount)
                                    var i0 = s.indexOf("encoding")
                                    if (i0 != -1) {
                                        while (s[i0] != '"'
                                            && s[i0] != '\''
                                        ) i0++
                                        val deli = s[i0++]
                                        val i1 = s.indexOf(deli, i0)
                                        enc = s.substring(i0, i1)
                                    }
                                    break
                                }
                            }
                            if (chk and -0x10000 == -0x1010000) {
                                enc = "UTF-16BE"
                                srcBuf[0] = (srcBuf[2].code shl 8 or srcBuf[3].code).toChar()
                                srcBufCount = 1
                            } else if (chk and -0x10000 == -0x20000) {
                                enc = "UTF-16LE"
                                srcBuf[0] = (srcBuf[3].code shl 8 or srcBuf[2].code).toChar()
                                srcBufCount = 1
                            } else if (chk and -0x100 == -0x10444100) {
                                enc = "UTF-8"
                                srcBuf[0] = srcBuf[3]
                                srcBufCount = 1
                            }
                        }
                        else -> if (chk and -0x10000 == -0x1010000) {
                            enc = "UTF-16BE"
                            srcBuf[0] = (srcBuf[2].code shl 8 or srcBuf[3].code).toChar()
                            srcBufCount = 1
                        } else if (chk and -0x10000 == -0x20000) {
                            enc = "UTF-16LE"
                            srcBuf[0] = (srcBuf[3].code shl 8 or srcBuf[2].code).toChar()
                            srcBufCount = 1
                        } else if (chk and -0x100 == -0x10444100) {
                            enc = "UTF-8"
                            srcBuf[0] = srcBuf[3]
                            srcBufCount = 1
                        }
                    }
                }
            }
            if (enc == null) enc = "UTF-8"
            val sc = srcBufCount
            setInput(InputStreamReader(inputStream, enc))
            this.encoding = encoding
            srcBufCount = sc
        } catch (e: java.lang.Exception) {
            throw XmlException(
                "Invalid stream or encoding: " + e.toString(),
                this,
                e
            )
        }
    }


    override fun close() {
        //NO-Op
    }

    private fun adjustNsp(fullName: String): Boolean {
        var hasActualAttributeWithPrefix = false
        var i = 0
        while (i < (attributeCount * 4)) {

            // * 4 - 4; i >= 0; i -= 4) {
            var attrName: String? = attributes[i + 2]
            val cIndex = attrName!!.indexOf(':')
            var prefix: String
            if (cIndex >= 0) {
                prefix = attrName.substring(0, cIndex)
                attrName = attrName.substring(cIndex + 1)
            } else if (attrName == "xmlns") {
                prefix = attrName
                attrName = null
            } else {
                attributes[i] = XMLConstants.NULL_NS_URI // the namespace for the attribute must be the null namespace
                attributes[i + 1] = XMLConstants.DEFAULT_NS_PREFIX
                i += 4
                continue
            }
            if (prefix != "xmlns") {
                hasActualAttributeWithPrefix = true
            } else {
                namespaceHolder.addPrefixToContext(attrName, attributes[i+3])
                if (attrName != null && attributes[i + 3] == "") error("illegal empty namespace")

                //  prefixMap = new PrefixMap (prefixMap, attrName, attr.getValue ());

                //System.out.println (prefixMap);
                System.arraycopy(
                    attributes,
                    i + 4,
                    attributes,
                    i,
                    (--attributeCount shl 2) - i
                )
                i -= 4
            }
            i += 4
        }
        if (hasActualAttributeWithPrefix) {
            var i = (attributeCount - 1) * 4
            while (i >= 0) {
                var attrName = attributes[i + 2]!!
                val cIndex = attrName.indexOf(':')
                if (cIndex == 0 && !relaxed) {
                    throw java.lang.RuntimeException("illegal attribute name: $attrName at $this")
                } else if (cIndex != -1) {
                    val attrPrefix = attrName.substring(0, cIndex)
                    attrName = attrName.substring(cIndex + 1)
                    val attrNs = namespaceHolder.getNamespaceUri(attrPrefix)
                    if (attrNs == null && !relaxed) throw java.lang.RuntimeException(
                        "Undefined Prefix: $attrPrefix in $this"
                    )
                    attributes[i] = attrNs
                    attributes[i + 1] = attrPrefix
                    attributes[i + 2] = attrName

                }
                i -= 4
            }
        }
        val cut = fullName.indexOf(':') // TODO store name temporarilly
        if (cut == 0) error("illegal tag name: $name")
        val prefix: String
        val localName: String
        if (cut != -1) {
            prefix = fullName.substring(0, cut)
            localName = fullName.substring(cut + 1)
        } else {
            prefix = ""
            localName = fullName
        }
        _prefix = prefix
        _name = localName
        _namespace = namespaceHolder.getNamespaceUri(prefix)
        if (_namespace == null) {
            if (cut >= 0) error("undefined prefix: $prefix")
            _namespace = XMLConstants.NULL_NS_URI
        }
        return hasActualAttributeWithPrefix
    }

    private fun ensureCapacity(arr: Array<String?>, required: Int): Array<String?> {
        if (arr.size >= required) return arr
        val bigger = arrayOfNulls<String>(required + 16)
        System.arraycopy(arr, 0, bigger, 0, arr.size)
        return bigger
    }

    private fun error(desc: String) {
        if (relaxed) {
            if (error == null) error = "ERR: $desc"
        } else exception(desc)
    }

    private fun exception(desc: String) {
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
        isStarted = true
        while (true) {
            attributeCount = -1

            // degenerated needs to be handled before error because of possible
            // processor expectations(!)
            if (isSelfClosing) {
                isSelfClosing = false
                _eventType = END_ELEMENT
                return
            }
            error?.let { e -> // TODO Error should be different
                for (element in e) push(element.code)

                this.error = null
                _eventType = COMMENT
                return
            }

            _prefix = null
            _name = null
            _namespace = null
            //            text = null;
            _eventType = peekType()
            when (_eventType) {
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
                    if (depth == 0) {
                        if (isWhitespace) _eventType = IGNORABLE_WHITESPACE
                        // make exception switchable for instances.chg... !!!!
                        //	else
                        //    exception ("text '"+getText ()+"' not allowed outside root element");
                    }
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
        var req = ""
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
                    if (attributeCount < 1 || "version" != attributes[2]) error("version expected")
                    version = attributes[3]
                    var pos = 1
                    if (pos < attributeCount
                        && "encoding" == attributes[2 + 4]
                    ) {
                        encoding = attributes[3 + 4]
                        pos++
                    }
                    if (pos < attributeCount
                        && "standalone" == attributes[4 * pos + 2]
                    ) {
                        val st = attributes[3 + 4 * pos]
                        if ("yes" == st) standalone = true else if ("no" == st) standalone =
                            false else error("illegal standalone value: $st")
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
                        int */term = '?'.code
            result = PROCESSING_INSTRUCTION
        } else if (c == '!'.code) {
            if (peek(0) == '-'.code) {
                result = COMMENT
                req = "--"
                term = '-'.code
            } else if (peek(0) == '['.code) {
                result = CDSECT
                req = "[CDATA["
                term = ']'.code
                localPush = true
            } else {
                result = DOCDECL
                req = "DOCTYPE"
                term = -1
            }
        } else {
            error("illegal: <$c")
            return COMMENT
        }
        for (i in 0 until req.length) read(req[i])
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
        val fullName = readName() // TODO store local before handling namespaces
        skip()
        read('>')
        val sp = depth - 1 shl 2
        if (depth == 0) {
            error("element stack empty")
            _eventType = COMMENT
            return
        }
        if (!relaxed) {
            if (!fullName.equals(elementStack[sp + 3])) {
                error("expected: /" + elementStack[sp + 3] + " read: " + fullName)
            }
            _namespace = elementStack[sp]
            _prefix = elementStack[sp + 1]
            _name = elementStack[sp + 2]
        }
    }

    @Throws(java.io.IOException::class)
    private fun peekType(): EventType {
        return when (peek(0)) {
            -1 -> END_DOCUMENT
            '&'.code -> ENTITY_REF
            '<'.code -> when (peek(1).toChar()) {
                '/' -> END_ELEMENT
                '?' -> PROCESSING_INSTRUCTION
                '!' -> COMMENT
                else -> START_ELEMENT
            }
            else -> TEXT
        }
    }

    private operator fun get(pos: Int): String {
        return String(txtBuf, pos, txtBufPos - pos)
    }

    private fun push(c: Int) {
        isWhitespace = isWhitespace and (c <= ' '.code)
        if (txtBufPos + 1 >= txtBuf.size) { // +1 to have enough space for 2 surrogates, if needed
            val bigger = CharArray(txtBufPos * 4 / 3 + 4)
            System.arraycopy(txtBuf, 0, bigger, 0, txtBufPos)
            txtBuf = bigger
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
        attributeCount = 0
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
            if (attrName.length == 0) {
                error("attr name expected")
                //type = COMMENT;
                break
            }
            var i = attributeCount++ shl 2
            attributes = ensureCapacity(attributes, i + 4)
            attributes[i++] = ""
            attributes[i++] = null
            attributes[i++] = attrName
            skip()
            if (peek(0) != '='.code) {
                if (!relaxed) {
                    error("Attr.value missing f. $attrName")
                }
                attributes[i] = attrName
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
                attributes[i] = get(p)
                txtBufPos = p
                if (delimiter != ' '.code) read() // skip endquote
            }
        }
        val sp = depth * 4 // Determine this before increasing the depth
        namespaceHolder.incDepth()
        elementStack = ensureCapacity(elementStack, sp + 4)
        elementStack[sp + 3] = fullName

        if (processNsp) adjustNsp(fullName) else { _namespace = ""; _prefix= ""; _name = fullName }
        elementStack[sp] = _namespace
        elementStack[sp + 1] = _prefix
        elementStack[sp + 2] = _name
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
            _name = code
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
            if (delimiter == ' '.code) if (next <= ' '.code || next == '>'.code) break
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

    @Throws(java.io.IOException::class)
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
    @Throws(java.io.IOException::class)
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

    @Throws(java.io.IOException::class)
    private fun skip() {
        while (true) {
            val c = peek(0)
            if (c > ' '.code || c == -1) break // More sane
            read()
        }
    }


    private fun defineEntityReplacementText(entity: String, value: String) {
        entityMap[entity] = value
    }

    override fun getNamespacePrefix(namespaceUri: String): String? {
        return namespaceHolder.getPrefix(namespaceUri)
    }

    override fun getNamespaceURI(prefix: String): String? {
        return namespaceHolder.getNamespaceUri(prefix)
    }

    private fun getPositionDescription(): String {
        val et = this._eventType ?: return ("<!--Parsing not started yet-->")

        val buf: StringBuilder = StringBuilder(et.name)
        buf.append(' ')
        if (et == START_ELEMENT || et == END_ELEMENT) {
            if (isSelfClosing) buf.append("(empty) ")
            buf.append('<')
            if (et == END_ELEMENT) buf.append('/')
            if (_prefix != null) buf.append("{$_namespace}$prefix:")
            buf.append(name)
            val cnt = attributeCount shl 2
            var i = 0
            while (i < cnt) {
                buf.append(' ')
                if (attributes[i + 1] != null) buf.append(
                    "{" + attributes[i] + "}" + attributes[i + 1] + ":"
                )
                buf.append(attributes[i + 2].toString() + "='" + attributes[i + 3] + "'")
                i += 4
            }
            buf.append('>')
        } else if (et == IGNORABLE_WHITESPACE) ; else if (et != TEXT) buf.append(text) else if (isWhitespace) buf.append(
            "(whitespace)"
        ) else {
            var textCpy = text
            if (textCpy.length > 16) textCpy = textCpy.substring(0, 16) + "..."
            buf.append(textCpy)
        }
        buf.append("@$line:$column")
        if (location != null) {
            buf.append(" in ")
            buf.append(location)
        } else {
            buf.append(" in ")
            buf.append(reader.toString())
        }
        return buf.toString()
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
            else -> throw XmlException("The element is not text")
        }

/*
    fun getTextCharacters(poslen: IntArray): CharArray? {
        if (type >= TEXT) {
            if (type == ENTITY_REF) {
                poslen[0] = 0
                poslen[1] = name!!.length()
                return name!!.toCharArray()
            }
            poslen[0] = 0
            poslen[1] = txtPos
            return txtBuf
        }
        poslen[0] = -1
        poslen[1] = -1
        return null
    }
*/

    public fun isEmptyElementTag(): Boolean {
        if (_eventType != START_ELEMENT) exception(ILLEGAL_TYPE)
        return isSelfClosing
    }

    private fun getAttributeType(index: Int): String {
        return "CDATA"
    }

    private fun isAttributeDefault(index: Int): Boolean {
        return false
    }

    override fun getAttributeNamespace(index: Int): String {
        if (index >= attributeCount) throw java.lang.IndexOutOfBoundsException()
        return attributes[index shl 2]!!
    }

    override fun getAttributeLocalName(index: Int): String {
        if (index >= attributeCount) throw java.lang.IndexOutOfBoundsException()
        return attributes[(index shl 2) + 2]!!
    }

    override fun getAttributePrefix(index: Int): String {
        if (index >= attributeCount) throw java.lang.IndexOutOfBoundsException()
        return attributes[(index shl 2) + 1]!!
    }

    override fun getAttributeValue(index: Int): String {
        if (index >= attributeCount) throw java.lang.IndexOutOfBoundsException()
        return attributes[(index shl 2) + 3]!!
    }

    override fun getAttributeValue(nsUri: String?, localName: String): String? {
        var i = (attributeCount shl 2) - 4
        while (i >= 0) {
            if (attributes[i + 2] == localName && (nsUri == null || attributes[i] == nsUri)) return attributes[i + 3]
            i -= 4
        }
        return null
    }

/*
    private fun nextUnignored(): EventType {
        txtPos = 0
        isWhitespace = true
        var minType = 9999
        token = false
        do {
            nextImpl()
            if (_eventType._eventType < minType) minType = _eventType
            //	    if (curr <= TEXT) type = curr;
        } while (minType > ENTITY_REF // ignorable
            || minType >= TEXT && peekType() >= TEXT
        )
        _eventType = minType
        if (_eventType > TEXT) _eventType = TEXT
        return _eventType
    }
*/

    override fun next(): EventType {
        isWhitespace = true
        txtBufPos = 0
        token = true
        nextImpl()
        return eventType
    }

    override fun hasNext(): Boolean {
        return _eventType != END_DOCUMENT // TODO handle this better
    }
//
    // utility methods to make XML parsing easier ...

    //
    // utility methods to make XML parsing easier ...
    override fun nextTag(): EventType {
        do {
            next()
        } while (_eventType == IGNORABLE_WHITESPACE || (_eventType == TEXT && isWhitespace))

        if (_eventType != END_ELEMENT && _eventType != START_ELEMENT) exception("unexpected type")
        return eventType
    }

    override fun require(type: EventType, namespace: String?, name: String?) {
        if (type != this._eventType || namespace != null && namespace != _namespace
            || name != null && name != this._name
        ) exception("expected: $type {$namespace}$name")
    }

    private fun nextText(): String? {
        if (_eventType != START_ELEMENT) exception("precondition: START_ELEMENT")
        next()
        val result: String?
        if (_eventType == TEXT) {
            result = text
            next()
        } else result = ""
        if (_eventType != END_ELEMENT) exception("END_ELEMENT expected")
        return result
    }

    private companion object {
        const val UNEXPECTED_EOF = "Unexpected EOF"
        const val ILLEGAL_TYPE = "Wrong event type"
        const val LEGACY = 999
        const val XML_DECL = 998

        const val processNsp = true
        const val relaxed = false

    }
}
