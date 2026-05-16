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

import nl.adaptivity.xmlutil.dom.PlatformDocument
import nl.adaptivity.xmlutil.dom.PlatformNode
import nl.adaptivity.xmlutil.dom2.Document
import org.w3c.dom.DOMConfiguration

internal class DocumentImpl(delegate: PlatformDocument) : AbstractNodeImpl<PlatformDocument>(delegate), Document {
    override fun getInputEncoding(): String? = delegate.inputEncoding

    override fun getImplementation(): DOMImplementationImpl = DOMImplementationImpl

    override fun getOwnerDocument(): Nothing? = null

    override fun getNodeValue(): Nothing? = null

    override fun setNodeValue(value: String?) {
        delegate.nodeValue = value
    }

    override fun getDoctype(): DocumentTypeImpl? = delegate.doctype?.let(::DocumentTypeImpl)

    override fun getDocumentElement(): ElementImpl? = delegate.documentElement?.wrap()

    override fun getXmlEncoding(): String = delegate.xmlEncoding

    @Deprecated("For now always false")
    override fun getXmlStandalone(): Boolean = delegate.xmlStandalone

    @Deprecated("No-op for now")
    override fun setXmlStandalone(xmlStandalone: Boolean) {
        delegate.xmlStandalone = xmlStandalone
    }

    @Deprecated("1.0 for now")
    override fun getXmlVersion(): String = delegate.xmlVersion

    @Deprecated("No-op for now")
    override fun setXmlVersion(xmlVersion: String?) {
        delegate.xmlVersion = xmlVersion
    }

    override fun getStrictErrorChecking(): Boolean = delegate.strictErrorChecking

    @Deprecated("No-op for now")
    override fun setStrictErrorChecking(strictErrorChecking: Boolean) {
        delegate.strictErrorChecking = strictErrorChecking
    }

    override fun getDocumentURI(): String = delegate.documentURI

    @Deprecated("No-op for now")
    override fun setDocumentURI(documentURI: String?) {
        delegate.documentURI = documentURI
    }

    override fun getDomConfig(): DOMConfiguration {
        return delegate.domConfig
    }

    override fun getBaseURI(): String {
        return delegate.baseURI!!
    }

    override fun getAttributes(): Nothing? = null

    override fun getElementsByTagName(qualifiedName: String): WrappingNodeList {
        return WrappingNodeList(delegate.getElementsByTagName(qualifiedName))
    }

    override fun getElementsByTagNameNS(namespace: String?, localName: String): WrappingNodeList {
        return WrappingNodeList(delegate.getElementsByTagNameNS(namespace, localName))
    }

    override fun getElementById(elementId: String): ElementImpl? = delegate.getElementById(elementId)?.wrap()

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

    override fun createAttribute(localName: String): AttrImpl = AttrImpl(delegate.createAttribute(localName))

    override fun createAttributeNS(namespace: String?, qualifiedName: String): AttrImpl =
        AttrImpl(delegate.createAttributeNS(namespace, qualifiedName))

    override fun createElementNS(namespaceURI: String, qualifiedName: String): ElementImpl =
        ElementImpl(delegate.createElementNS(namespaceURI, qualifiedName))

    override fun createEntityReference(name: String?): EntityReferenceImpl {
        return EntityReferenceImpl(delegate.createEntityReference(name))
    }

    @Deprecated("No-op for now")
    override fun normalizeDocument() {
        return delegate.normalizeDocument()
    }

    override fun cloneNode(deep: Boolean): DocumentImpl {
        return DocumentImpl(delegate.cloneNode(deep) as PlatformDocument)
    }

    fun renameNode(n: PlatformNode, namespaceURI: String?, qualifiedName: String): AbstractNodeImpl<*> =
        delegate.renameNode(n.unWrap(), namespaceURI, qualifiedName).wrap()

    override fun adoptNode(node: PlatformNode): AbstractNodeImpl<*> = delegate.adoptNode(node.unWrap()).wrap()

    override fun importNode(node: PlatformNode, deep: Boolean): AbstractNodeImpl<*> =
        delegate.importNode(node.unWrap(), deep).wrap()
}
