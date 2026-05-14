/*
 * Copyright (c) 2025-2026.
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

@file:MustUseReturnValues

package nl.adaptivity.xmlutil.dom2

import nl.adaptivity.xmlutil.core.impl.wrappingDom.wrap
import nl.adaptivity.xmlutil.dom.PlatformDocument
import nl.adaptivity.xmlutil.dom.PlatformNode
import org.w3c.dom.DOMConfiguration
import org.w3c.dom.EntityReference
import org.w3c.dom.NodeList
import org.w3c.dom.Node as DomNode

public actual interface Document : Node, PlatformDocument {
    public actual override fun getImplementation(): DOMImplementation
    public actual override fun getDoctype(): DocumentType?
    public actual override fun getDocumentElement(): Element?
    public actual override fun getInputEncoding(): String?
    public actual override fun importNode(node: PlatformNode, deep: Boolean): Node
    public actual override fun adoptNode(node: PlatformNode): Node?
    public actual override fun createAttribute(localName: String): Attr
    public actual override fun createAttributeNS(namespace: String?, qualifiedName: String): Attr
    public actual override fun createElement(localName: String): Element
    public actual override fun createElementNS(namespaceURI: String, qualifiedName: String): Element
    public actual override fun createDocumentFragment(): DocumentFragment
    public actual override fun createTextNode(data: String): Text
    public actual override fun createCDATASection(data: String): CDATASection
    public actual override fun createComment(data: String): Comment
    public actual override fun createProcessingInstruction(target: String, data: String): ProcessingInstruction
    override fun createEntityReference(name: String?): EntityReference? {
        throw UnsupportedOperationException("Entity references are not supported")
    }

    public actual override fun getOwnerDocument(): Nothing?
    public actual override fun getNodeValue(): Nothing?
    override fun setNodeValue(nodeValue: String?) {} // nothing to do

    actual override fun cloneNode(deep: Boolean): Document


    override fun getBaseURI(): String?



    override fun getElementsByTagName(tagname: String?): NodeList? =
        TODO("Note yet implemented")


    override fun getElementsByTagNameNS(
        namespaceURI: String?,
        localName: String
    ): NodeList? = TODO("Note yet implemented")

    override fun getElementById(elementId: String): Element? {
        fun getElementById(parent: Element, id: String): Element? {
            val idAttr = parent.attributes.firstOrNull { it.isId }
            if (idAttr!=null && idAttr.value==id) return parent
            return parent.childNodes.asSequence()
                .filterIsInstance<Element>()
                .map { getElementById(it, id) }
                .firstOrNull()
        }

        return childNodes.asSequence()
            .filterIsInstance<Element>()
            .map { getElementById(it, elementId) }
            .firstOrNull()
    }

    override fun getXmlEncoding(): String? {
        return inputEncoding
    }

    @Deprecated("For now always false")
    override fun getXmlStandalone(): Boolean {
        return false
    }

    @Deprecated("No-op for now")
    override fun setXmlStandalone(xmlStandalone: Boolean) {}

    @Deprecated("1.0 for now")
    override fun getXmlVersion(): String? {
        return "1.0"
    }

    @Deprecated("No-op for now")
    override fun setXmlVersion(xmlVersion: String?) {}

    override fun getStrictErrorChecking(): Boolean {
        return true
    }

    @Deprecated("No-op for now")
    override fun setStrictErrorChecking(strictErrorChecking: Boolean) {}

    override fun getDocumentURI(): String? = null

    actual override fun setDocumentURI(documentURI: String?)

    override fun getDomConfig(): DOMConfiguration? {
        TODO("not implemented")
    }

    @Deprecated("No-op for now")
    override fun normalizeDocument() {
        @Suppress("DEPRECATION")
        normalize()
    }

    override fun renameNode(n: DomNode?, namespaceURI: String?, qualifiedName: String?): DomNode? {
        TODO("not implemented")
    }

    actual override fun getAttributes(): Nothing?

}

public actual fun Document.importNode(
    node: PlatformNode,
    deep: Boolean
): Node {
    return importNode(node.wrap(), deep)
}
