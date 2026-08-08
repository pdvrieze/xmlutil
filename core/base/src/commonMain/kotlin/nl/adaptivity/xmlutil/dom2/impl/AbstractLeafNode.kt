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

package nl.adaptivity.xmlutil.dom2.impl

import nl.adaptivity.xmlutil.ExperimentalXmlUtilApi
import nl.adaptivity.xmlutil.XMLConstants
import nl.adaptivity.xmlutil.dom.DOMException
import nl.adaptivity.xmlutil.dom.PlatformNode
import nl.adaptivity.xmlutil.dom2.parentNode

@ExperimentalXmlUtilApi
public abstract class AbstractLeafNode<out N : IAbstractNode<N, P>, out P : IAbstractParentNode<N, P>>(
    ownerDocument: AbstractDocument<N, P>?,
    parentOrOwner: P? = null,
) : AbstractNode<N, P>(ownerDocument, parentOrOwner) {

    final override fun getFirstChild(): Nothing? = null

    final override fun getLastChild(): Nothing? = null

    final override fun appendChild(node: PlatformNode): Nothing {
        throw DOMException.hierarchyRequestErr("Cannot insert a child node into a leaf node")
    }

    @ExperimentalXmlUtilApi
    @IgnorableReturnValue
    final override fun insertBefore(newChild: PlatformNode, refChild: PlatformNode?): Nothing {
        throw DOMException.hierarchyRequestErr("Cannot insert a child node into a leaf node")
    }

    final override fun replaceChild(newChild: PlatformNode, oldChild: PlatformNode): Nothing {
        throw DOMException.hierarchyRequestErr("Cannot insert a child node into a leaf node")
    }

    final override fun removeChild(node: PlatformNode): Nothing {
        throw DOMException.hierarchyRequestErr("Cannot insert a child node into a leaf node")
    }

    final override fun getChildNodes(): AbstractNodeList<N, P> = EmptyNodeList

    override fun getAttributes(): Nothing? = null
    final override fun hasAttributes(): Boolean {
        return false
    }

    final override fun lookupPrefix(namespace: String): String? {
        return parentNode?.lookupPrefix(namespace) ?: when (namespace) {
            XMLConstants.XML_NS_URI -> XMLConstants.XML_NS_PREFIX
            XMLConstants.XMLNS_ATTRIBUTE_NS_URI -> XMLConstants.XMLNS_ATTRIBUTE
            else -> null
        }
    }

    final override fun lookupNamespaceURI(prefix: String): String? {
        return parentNode?.lookupNamespaceURI(prefix) ?: when (prefix) {
            XMLConstants.XML_NS_PREFIX -> XMLConstants.XML_NS_URI
            XMLConstants.XMLNS_ATTRIBUTE -> XMLConstants.XMLNS_ATTRIBUTE_NS_URI
            else -> null
        }
    }

    abstract override fun cloneNode(deep: Boolean): AbstractLeafNode<N, P>

}
