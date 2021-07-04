/*
 * Copyright (c) 2018.
 *
 * This file is part of XmlUtil.
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

package nl.adaptivity.xmlutil

public actual class QName public actual constructor(
    private val namespaceURI: String,
    private val localPart: String,
    private val prefix: String
                                     ) {

    public actual constructor(namespaceURI: String, localPart: String) : this(
        namespaceURI,
        localPart,
        XMLConstants.DEFAULT_NS_PREFIX
                                                                      )

    public actual constructor(localPart: String) : this(XMLConstants.NULL_NS_URI, localPart, XMLConstants.DEFAULT_NS_PREFIX)

    public actual fun getPrefix(): String = prefix

    public actual fun getLocalPart(): String = localPart

    public actual fun getNamespaceURI(): String = namespaceURI

    override fun toString(): String {
        if (namespaceURI == XMLConstants.NULL_NS_URI) return localPart
        return "{$namespaceURI}$localPart"
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || this::class.js != other::class.js) return false

        other as QName

        if (namespaceURI != other.namespaceURI) return false
        if (localPart != other.localPart) return false

        return true
    }

    override fun hashCode(): Int {
        var result = namespaceURI.hashCode()
        result = 31 * result + localPart.hashCode()
        return result
    }
}
