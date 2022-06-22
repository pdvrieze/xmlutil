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

package nl.adaptivity.xmlutil.util

import nl.adaptivity.xmlutil.*


/**
 * A class representing an xml fragment compactly.
 * Created by pdvrieze on 06/11/15.2
 */
public actual class CompactFragment : ICompactFragment {

    @Suppress("DEPRECATION")
    public actual class Factory : XmlDeserializerFactory<CompactFragment> {

        override fun deserialize(reader: XmlReader): CompactFragment {
            @Suppress("RedundantCompanionReference")
            return Companion.deserialize(reader)
        }
    }

    override val isEmpty: Boolean
        get() = content.isEmpty()

    override val namespaces: IterableNamespaceContext

    @Transient
    override val content: CharArray

    public actual constructor(namespaces: Iterable<Namespace>, content: CharArray?) {
        this.namespaces = SimpleNamespaceContext.from(namespaces)
        this.content = content ?: CharArray(0)
    }

    /** Convenience constructor for content without namespaces.  */
    public actual constructor(content: String) : this(emptyList<Namespace>(), content.toCharArray())

    public actual constructor(orig: ICompactFragment) {
        namespaces = SimpleNamespaceContext.from(orig.namespaces)
        content = orig.content
    }

    public actual constructor(namespaces: Iterable<Namespace>, content: String) :
            this(namespaces, content.toCharArray())


    @Throws(XmlException::class)
    override fun serialize(out: XmlWriter) {
        XMLFragmentStreamReader.from(this).use { reader ->
            out.serialize(reader)
        }
    }

    override fun getXmlReader(): XmlReader = XMLFragmentStreamReader.from(this)

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || javaClass != other.javaClass) return false

        val that = other as ICompactFragment?

        if (namespaces != that!!.namespaces) return false
        return content.contentEquals(that.content)

    }

    override fun hashCode(): Int {
        var result = namespaces.hashCode()
        result = 31 * result + content.contentHashCode()
        return result
    }

    override fun toString(): String {
        return namespaces.joinToString(
            prefix = "{namespaces=[",
            postfix = "], content=$contentString}"
        ) { "${it.prefix} -> ${it.namespaceURI}" }
    }

    override val contentString: String
        get() = String(content)

    public actual companion object {

        @Suppress("DEPRECATION")
        @JvmStatic
        public val FACTORY: XmlDeserializerFactory<CompactFragment> = Factory()

        @Throws(XmlException::class)
        public actual fun deserialize(reader: XmlReader): CompactFragment {
            return reader.siblingsToFragment()
        }
    }
}
