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

package io.github.pdvrieze.formats.xmlschema.datatypes.primitiveInstances

import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import nl.adaptivity.xmlutil.xmlCollapseWhitespace

@Serializable(VParsedURI.Serializer::class)
class VParsedURI(str: String): VAnyURI() {
    constructor(charSequence: CharSequence) : this(charSequence.toString())

    private val scheme: String?
    private val authority: String?
    private val path: String
    private val query: String?
    private val fragment: String?

    init {
        // Parse according to https://www.ietf.org/rfc/rfc3986.txt
        // This can not be AnyURIType as it is used in defining AtomicDataType
        require(str == xmlCollapseWhitespace(str))
        var next = 0
        var pos: Int
        if (':' in str) {
            while (next < str.length && str[next] != ':') {
                val c = str[next]
                when (next) {
                    0 -> require(c.isAlpha()) { "Scheme must start with a letter, not '$c' for uri '$str'" }
                    else -> require(c.isSchemeLetter()) { "Scheme has limited valid values, not '$c' for uri '$str'" }
                }
                require(++next < str.length) { "No scheme end in uri" }
            }
            scheme = str.substring(0, next).also { check(it.isValid(Part.SCHEME)) { "Scheme '$it' is not valid" } }
            pos = next + 1

            next = pos
            if (str.length >= next + 2 && str[next] == '/' && str[next + 1] == '/') {
                pos+=2
                next = pos
                val delims = arrayOf('/', '?', '#')
                while (next < str.length && str[next].let { it != '/' && it != '?' && it != '#' }) ++next
                authority = str.substring(pos, next).also { check(it.isValid(Part.AUTHORITY)) }
                pos = next
            } else {
                authority = null
            }
        } else {
            scheme = null
            authority = null
            pos = 0
        }

        while (next < str.length && str[next].let { it != '?' && it != '#' }) ++next
        path = str.substring(pos, next).also { check(it.isValid(Part.PATH)) { "Path '$it' is not valid" } }
        if (next < str.length && str[next] == '?') {
            pos = ++next;
            while (next < str.length && str[next] != '#') ++next
            query = str.substring(pos, next).also { check(it.isValid(Part.QUERY)) { "Query '$it' is not valid" } }
        } else {
            query = null
        }
        pos = next

        if (next < str.length) {
            require(str[next] == '#')
            fragment = str.substring(pos + 1).also { check(it.isValid(Part.FRAGMENT)) { "Fragment '$it' is not valid" } }
        } else {
            fragment = null
        }

        check(str == xmlString) { "'$str' != '$xmlString'"}
    }

    override val xmlString: String get() = buildString {
        if (scheme != null) {
            append(scheme).append(':')
            if (authority != null) append("//").append(authority)
        }
        append(path)
        if (query != null) append('?').append(query)
        if (fragment != null) append('#').append(fragment)
    }

    override val length: Int get() = xmlString.length

    override fun get(index: Int): Char = xmlString.get(index)

    override fun subSequence(startIndex: Int, endIndex: Int): CharSequence =
        xmlString.subSequence(startIndex, endIndex)

    override fun toString(): String = xmlString

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || this::class != other::class) return false

        other as VParsedURI

        if (scheme != other.scheme) return false
        if (authority != other.authority) return false
        if (path != other.path) return false
        if (query != other.query) return false
        if (fragment != other.fragment) return false

        return true
    }

    override fun hashCode(): Int {
        var result = scheme?.hashCode() ?: 0
        result = 31 * result + (authority?.hashCode() ?: 0)
        result = 31 * result + path.hashCode()
        result = 31 * result + (query?.hashCode() ?: 0)
        result = 31 * result + (fragment?.hashCode() ?: 0)
        return result
    }

    internal enum class Part { SCHEME, AUTHORITY, PATH, QUERY, FRAGMENT }

    companion object Serializer: KSerializer<VParsedURI> {
        override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("xsd.anyURI", PrimitiveKind.STRING)

        override fun deserialize(decoder: Decoder): VParsedURI {
            return VParsedURI(xmlCollapseWhitespace(decoder.decodeString()))
        }


        override fun serialize(encoder: Encoder, value: VParsedURI) {
            encoder.encodeString(value.xmlString)
        }


        private val ALPHA = BooleanArray(127)
        private val UNRESERVED = BooleanArray(127)
        private val SCHEMELETTER = BooleanArray(127)
        private val HEXLETTER = BooleanArray(127)
        private val SUBDELIM = BooleanArray(127)

        init {
            for (i in ('A'.code..'Z'.code)+('a'.code..'z'.code)) {
                ALPHA[i] = true
                SCHEMELETTER[i] = true
                UNRESERVED[i] = true
            }
            for (i in ('A'.code..'F'.code)+('a'.code..'f'.code)) {
                HEXLETTER[i] = true
            }
            for (i in '0'.code..'9'.code) {
                SCHEMELETTER[i] = true
                UNRESERVED[i] = true
                HEXLETTER[i] = true
            }
            SCHEMELETTER['+'.code] = true
            SCHEMELETTER['-'.code] = true
            SCHEMELETTER['.'.code] = true
            UNRESERVED['-'.code] = true
            UNRESERVED['.'.code] = true
            UNRESERVED['_'.code] = true
            UNRESERVED['~'.code] = true
            for (c in arrayOf('!', '$', '&', '\'', '(',')', '*', '+', ',', ';', '=')) {
                SUBDELIM[c.code] = true
            }
        }

        internal fun CharSequence.isValid(part: Part): Boolean {

            when (part) {
                Part.SCHEME -> return length>0 && all { it.isSchemeLetter() }
                Part.AUTHORITY -> { // somewhat partial
                    var hasUser = false
                    var hasPortOrIp6 = false
                    var i = 0
                    while(i<length) {
                        when(val c = get(i)) {
                            '@' -> if (!hasUser && !hasPortOrIp6) { hasUser = true } else return false
                            '%' -> if (hasPortOrIp6 || (i+2 >= length && get(i+1).isHexLetter() && get(i+2).isHexLetter())) i+=2 else return false
                            ':' -> hasPortOrIp6 = true//;if (!hasPort) { hasPort = true } else return false
                            in '0'..'9' -> {} //always fine
                            else -> {
                                if (hasPortOrIp6 && !c.isHexLetter()) return false
                                if (!c.isUnreserved() && !c.isSubDelim()) return false
                            }
                        }
                        ++i
                    }
                }
                Part.PATH -> {
                    var lastSegmentLength = 1
                    var currentSegmentLength = 0
                    var i = 0
                    while(i <length) {
                        val c = get(i)
                        when(c) {
                            '/' -> when {
                                lastSegmentLength == 0 -> return false // disallow consecutive

                                else -> {
                                    lastSegmentLength=if (i==0) 1 else currentSegmentLength
                                    currentSegmentLength = 0
                                }
                            }
                            '%' -> when {
                                i+2 <= length && get(i+1).isHexLetter() && get(i+2).isHexLetter() -> {
                                    i+=2
                                    ++currentSegmentLength
                                }
                                else -> return false
                            }
                            ':', '@', '<', '>', '"' -> ++currentSegmentLength // allow last 3 for test compatibility (technically invalid)
                            else -> {
                                if (!c.isUnreserved() && !c.isSubDelim()) return false
                                ++currentSegmentLength
                            }

                        }
                        ++i
                    }
                }
                Part.QUERY, Part.FRAGMENT -> {
                    var i = 0
                    while(i <length) {
                        val c = get(i)
                        when(c) {
                            '%' -> if (i + 2 >= length && get(i + 1).isHexLetter() && get(i + 2).isHexLetter()) i += 2 else return false
                            '/', '?', ':', '@' -> {}
                            else -> if (!c.isUnreserved() && !c.isSubDelim()) return false

                        }
                        ++i
                    }
                }
            }
            return true
        }

        internal fun Char.isAlpha(): Boolean = when {
            code < ALPHA.size -> ALPHA[code]
            else -> false
        }

        internal fun Char.isHexLetter(): Boolean = when {
            code < HEXLETTER.size -> HEXLETTER[code]
            else -> false
        }

        internal fun Char.isSchemeLetter(): Boolean = when {
            code < SCHEMELETTER.size -> SCHEMELETTER[code]
            else -> false
        }

        internal fun Char.isSubDelim(): Boolean = when {
            code < SUBDELIM.size -> SUBDELIM[code]
            else -> false
        }

        internal fun Char.isUnreserved(): Boolean = when {
            code < UNRESERVED.size -> UNRESERVED[code]
            else -> this.isLetterOrDigit()
        }

    }
}

internal fun <T: CharSequence> T.checkValidUriChars(): T = also {
    for (c in it) {
        c.checkValidUriChar()
    }
}

private const val POS_SCHEME=0
private const val POS_AUTHORITY=1

private fun Char.checkValidUriChar(pos: Int = 0): Unit {
    when (code) {
        in 0x80f..0xffff -> error("$this is not Ascii")
        ':'.code, '/'.code, '?'.code, '#'.code, '['.code, ']'.code -> error("Character '$this' is a delimeter")
        '@'.code -> if (pos != POS_AUTHORITY) error("@ outside of authority")
    }
}
