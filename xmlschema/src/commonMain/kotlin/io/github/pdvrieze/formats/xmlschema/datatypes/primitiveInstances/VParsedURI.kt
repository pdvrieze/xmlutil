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

@Serializable(VAnyURI.Serializer::class)
class VParsedURI(str: String): VAnyAtomicType, CharSequence {
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
            scheme = str.substring(0, next)
            pos = next + 1

            next = pos
            if (str.length >= next + 2 && str[next] == '/' && str[next + 1] == '/') {
                pos+=2
                next = pos
                val delims = arrayOf('/', '?', '#')
                while (next < str.length && str[next].let { it != '/' && it != '?' && it != '#' }) ++next
                authority = str.substring(pos, next)
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
        path = str.substring(pos, next)
        if (next < str.length && str[next] == '?') {
            pos = ++next;
            while (next < str.length && str[next] != '#') ++next
            query = str.substring(pos, next)
        } else {
            query = null
        }
        pos = next

        if (next < str.length) {
            require(str[next] == '#')
            fragment = str.substring(pos + 1)
        } else {
            fragment = null
        }

        check(str == xmlString) { "'$str' != '$xmlString'"}
    }

    val value: String get() = xmlString

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

    companion object Serializer: KSerializer<VAnyURI> {
        override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("xsd.anyURI", PrimitiveKind.STRING)

        override fun deserialize(decoder: Decoder): VAnyURI {
            return VAnyURI(xmlCollapseWhitespace(decoder.decodeString()))
        }


        override fun serialize(encoder: Encoder, value: VAnyURI) {
            encoder.encodeString(value.xmlString)
        }
    }
}

private val ALPHA = BooleanArray(127).also {
    sequence {
        yieldAll('A'..'Z')
        yieldAll('a'..'z')
    }.forEach { c ->
        it[c.code] = true
    }
}
private val SCHEMELETTER = BooleanArray(127).also {
    sequence {
        yieldAll('A'..'Z')
        yieldAll('a'..'z')
        yieldAll('0'..'9')
        yield('+')
        yield('-')
        yield('.')
    }.forEach { c ->
        it[c.code] = true
    }
}

internal fun Char.isAlpha(): Boolean = when {
    code < ALPHA.size -> ALPHA[code]
    else -> false
}

internal fun Char.isSchemeLetter(): Boolean = when {
    code < SCHEMELETTER.size -> SCHEMELETTER[code]
    else -> false
}
