/*
 * Copyright (c) 2024-2026.
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

package nl.adaptivity.xmlutil.core.impl.wrappingDom

import nl.adaptivity.xmlutil.dom.PlatformNode
import nl.adaptivity.xmlutil.dom2.DOMImplementation
import nl.adaptivity.xmlutil.dom2.Document
import org.w3c.dom.Document as DomDocument
import org.w3c.dom.Node as DomNode

internal class JsWrappedDocument(delegate: DomDocument) : JsWrappedNode<DomDocument>(delegate), Document {
    override val ownerDocument: Nothing? get() = null

    // TODO might need to be added to Document2
    val documentURI: String = delegate.documentURI

    override fun getNodeValue(): Nothing? = null

    override fun getInputEncoding(): String = delegate.inputEncoding

    override fun getImplementation(): DOMImplementation = JsWrappedDOMImplementation

    override fun getDoctype(): JsWrappedDocumentType? = delegate.doctype?.let(::JsWrappedDocumentType)

    override fun getDocumentElement(): JsWrappedElement? = delegate.documentElement?.wrap()
    override fun getOwnerDocument(): Nothing? = null

    override fun createElement(localName: String): JsWrappedElement =
        JsWrappedElement(delegate.createElement(localName))

    override fun createDocumentFragment(): JsWrappedDocumentFragment =
        JsWrappedDocumentFragment(delegate.createDocumentFragment())

    override fun createTextNode(data: String): JsWrappedText = JsWrappedText(delegate.createTextNode(data))

    override fun createCDATASection(data: String): JsWrappedCDATASection {
        return JsWrappedCDATASection(delegate.createCDATASection(data))
    }

    override fun createComment(data: String): JsWrappedComment = JsWrappedComment(delegate.createComment(data))

    override fun createProcessingInstruction(target: String, data: String): JsWrappedProcessingInstruction =
        JsWrappedProcessingInstruction(delegate.createProcessingInstruction(target, data))

    override fun createAttribute(localName: String): JsWrappedAttr =
        JsWrappedAttr(delegate.createAttribute(localName))

    override fun createAttributeNS(namespace: String?, qualifiedName: String): JsWrappedAttr =
        JsWrappedAttr(delegate.createAttributeNS(namespace, qualifiedName))

    override fun createElementNS(namespaceURI: String, qualifiedName: String): JsWrappedElement =
        JsWrappedElement(delegate.createElementNS(namespaceURI, qualifiedName))

    override fun adoptNode(node: PlatformNode): JsWrappedNode<DomNode> =
        delegate.adoptNode(node.unWrap()).wrap()

    override fun importNode(node: PlatformNode, deep: Boolean): JsWrappedNode<DomNode> =
        delegate.importNode(node.unWrap(), deep).wrap()

    override fun getAttributes(): Nothing? = null

    override fun cloneNode(deep: Boolean): JsWrappedDocument {
        return JsWrappedDocument(delegate.cloneNode(deep) as DomDocument)
    }
}
