/*
 * Copyright (c) 2024-2025.
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

/**
 * Created by pdvrieze on 13/04/16.
 */
@file:JvmName("XmlUtil")
@file:MustUseReturnValues

package nl.adaptivity.xmlutil

import nl.adaptivity.xmlutil.XMLConstants.DEFAULT_NS_PREFIX
import nl.adaptivity.xmlutil.XMLConstants.NULL_NS_URI
import kotlin.jvm.JvmName

/** Determine whether the character is xml whitespace. */
public fun isXmlWhitespace(char: Char): Boolean = when(char) {
    '\u000A',
    '\u0009',
    '\u000d',
    ' ' -> true

    else -> false
}

public fun isXmlWhitespace(data: CharArray): Boolean = data.all { isXmlWhitespace(it) }

public fun isXmlWhitespace(data: CharSequence): Boolean = data.all { isXmlWhitespace(it) }

public fun xmlCollapseWhitespace(original: String): String = buildString(original.length) {
    var last = ' ' // Start with space, to trim start of symbol
    for (c in original) {
        if (c == '\t' || c == '\n' || c == '\r' || c == ' ') {
            if (last != ' ') append(' ')
            last = ' '
        } else {
            last = c
            append(c)
        }
    }
    if (last == ' ' && isNotEmpty()) this.deleteAt(this.length - 1) // make sure to trim
}

public fun xmlTrimWhitespace(original: String): String = buildString(original.length) {
    var start = -1
    for (i in original.indices) {
        when (original[i]) {
            '\t', '\n', '\r', ' ' -> {}
            else -> {
                start = i; break
            }
        }
    }
    if (start < 0) return "" // loop finished
    var end = -1
    for (i in original.indices.reversed()) {
        when (original[i]) {
            '\t', '\n', '\r', ' ' -> {}
            else -> { end = i; break }
        }
    }
    for(i in start..end) {
        when (val c = original[i]) {
            '\t', '\n', '\r', ' ' -> append(' ')

            else -> append(c)
        }
    }
}


public fun qname(namespaceUri: String?, localname: String, prefix: String? = DEFAULT_NS_PREFIX): QName =
    QName(
        namespaceUri ?: NULL_NS_URI,
        localname,
        prefix ?: DEFAULT_NS_PREFIX
    )

/**
 * Convert the string as fqn literal to an actual qname
 */
public fun CharSequence.toQname(): QName {
    val split = indexOf('}')
    val localname: String
    val nsUri: String
    if (split >= 0) {
        if (this[0] != '{') throw IllegalArgumentException("Not a valid qname literal")
        nsUri = substring(1, split)
        localname = substring(split + 1)
    } else {
        nsUri = NULL_NS_URI
        localname = toString()
    }
    return QName(nsUri, localname)
}

/**
 * Convert the string as fqn literal to actual name, but use the namespace parameter to fill in namespace (but not prefix)
 */
public fun CharSequence.toQname(namespace: Namespace): QName {
    val split = indexOf('}')
    val localname: String
    val nsUri: String
    if (split >= 0) {
        if (this[0] != '{') throw IllegalArgumentException("Not a valid qname literal")
        nsUri = substring(1, split)
        localname = substring(split + 1)
    } else {
        nsUri = namespace.namespaceURI // We don't need the prefix
        localname = toString()
    }
    return QName(nsUri, localname)
}

public fun QName.toCName(): String {
    if (NULL_NS_URI == getPrefix()) return getLocalPart()
    return "${getPrefix()}:${getLocalPart()}"
}


/**
 * Convert a prefixed element name (CNAME) to a qname. If there is no prefix, the default prefix is used.
 * @receiver The namespace context to use to resolve the name.
 *
 * @param name The name to resolve
 *
 * @return A resolved qname.
 */
public fun NamespaceContext.asQName(name: String): QName {
    val reference: NamespaceContext = this
    val colPos = name.indexOf(':')
    return if (colPos >= 0) {
        val prefix = name.substring(0, colPos)
        QName(
            reference.getNamespaceURI(prefix) ?: NULL_NS_URI, name.substring(colPos + 1),
            prefix
        )
    } else {
        QName(
            reference.getNamespaceURI(DEFAULT_NS_PREFIX) ?: NULL_NS_URI, name,
            DEFAULT_NS_PREFIX
        )
    }

}

/**
 * Function that attempts to parse the entire file and returns true if ths does not throw an exception.
 */
@Deprecated("Unintended function, that has poor semantics")
public fun XmlReader.isXml(): Boolean {
    runCatching {
        while (hasNext()) {
            val _ = next()
        }
    }.onFailure { return false }
    return true
}

public fun CharSequence.xmlEncode(): String = buildString(length.let { it + (it shr 4) }) {
    for (c in this@xmlEncode) {
        when (c) {
            '<' -> append("&lt;")
            '>' -> append("&gt;")
            '&' -> append("&amp;")
            else -> append(c)
        }
    }
}
