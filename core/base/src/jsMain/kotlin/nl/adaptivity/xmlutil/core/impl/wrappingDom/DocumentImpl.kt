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

internal class DocumentImpl(delegate: DomDocument) : NodeImpl<DomDocument>(delegate), Document {
    override val ownerDocument: Nothing? get() = null

    // TODO might need to be added to Document2
    val documentURI: String = delegate.documentURI

    override fun getNodeValue(): Nothing? = null

    override fun getInputEncoding(): String = delegate.inputEncoding

    override fun getImplementation(): DOMImplementation = DOMImplementationImpl

    override fun getDoctype(): DocumentTypeImpl? = delegate.doctype?.let(::DocumentTypeImpl)

    override fun getDocumentElement(): ElementImpl? = delegate.documentElement?.wrap()
    override fun getOwnerDocument(): Nothing? = null

    override fun createElement(localName: String): ElementImpl =
        ElementImpl(delegate.createElement(localName))

    override fun createDocumentFragment(): DocumentFragmentImpl =
        DocumentFragmentImpl(delegate.createDocumentFragment())

    override fun createTextNode(data: String): TextImpl = TextImpl(delegate.createTextNode(data))

    override fun createCDATASection(data: String): CDATASectionImpl {
        return CDATASectionImpl(delegate.createCDATASection(data))
    }

    override fun createComment(data: String): CommentImpl = CommentImpl(delegate.createComment(data))

    override fun createProcessingInstruction(target: String, data: String): ProcessingInstructionImpl =
        ProcessingInstructionImpl(delegate.createProcessingInstruction(target, data))

    override fun createAttribute(localName: String): AttrImpl =
        AttrImpl(delegate.createAttribute(localName))

    override fun createAttributeNS(namespace: String?, qualifiedName: String): AttrImpl =
        AttrImpl(delegate.createAttributeNS(namespace, qualifiedName))

    override fun createElementNS(namespaceURI: String, qualifiedName: String): ElementImpl =
        ElementImpl(delegate.createElementNS(namespaceURI, qualifiedName))

    override fun adoptNode(node: PlatformNode): NodeImpl<DomNode> =
        delegate.adoptNode(node.unWrap()).wrap()

    override fun importNode(node: PlatformNode, deep: Boolean): NodeImpl<DomNode> =
        delegate.importNode(node.unWrap(), deep).wrap()
}
