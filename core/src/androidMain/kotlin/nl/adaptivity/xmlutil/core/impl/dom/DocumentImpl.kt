/*
 * Copyright (c) 2023.
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

package nl.adaptivity.xmlutil.core.impl.dom

import nl.adaptivity.xmlutil.core.impl.idom.*
import org.w3c.dom.DOMConfiguration
import org.w3c.dom.EntityReference
import org.w3c.dom.NodeList
import nl.adaptivity.xmlutil.dom2.Node as Node2
import org.w3c.dom.Document as DomDocument
import org.w3c.dom.Node as DomNode

internal class DocumentImpl(delegate: DomDocument) : NodeImpl<DomDocument>(delegate), IDocument {
    override fun getInputEncoding(): String? = delegate.inputEncoding

    override fun getImplementation(): IDOMImplementation = DOMImplementationImpl(delegate.implementation)

    override fun getDoctype(): IDocumentType? = delegate.doctype?.let(::DocumentTypeImpl)

    override fun getDocumentElement(): IElement? = delegate.documentElement?.wrap()

    override fun getXmlEncoding(): String = delegate.xmlEncoding

    override fun getXmlStandalone(): Boolean = delegate.xmlStandalone

    override fun setXmlStandalone(xmlStandalone: Boolean) {
        delegate.xmlStandalone = xmlStandalone
    }

    override fun getXmlVersion(): String = delegate.xmlVersion

    override fun setXmlVersion(xmlVersion: String?) {
        delegate.xmlVersion = xmlVersion
    }

    override fun getStrictErrorChecking(): Boolean = delegate.strictErrorChecking

    override fun setStrictErrorChecking(strictErrorChecking: Boolean) {
        delegate.strictErrorChecking = strictErrorChecking
    }

    override fun getDocumentURI(): String = delegate.documentURI

    override fun setDocumentURI(documentURI: String?) {
        delegate.documentURI = documentURI
    }

    override fun getDomConfig(): DOMConfiguration {
        return delegate.domConfig
    }

    override fun getElementsByTagName(tagname: String): INodeList {
        return WrappingNodeList(delegate.getElementsByTagName(tagname))
    }

    override fun getElementsByTagNameNS(namespaceURI: String?, localName: String): NodeList {
        return WrappingNodeList(delegate.getElementsByTagNameNS(namespaceURI, localName))
    }

    override fun getElementById(elementId: String): IElement = delegate.getElementById(elementId).wrap()

    override fun createElement(localName: String): IElement =
        ElementImpl(delegate.createElement(localName))

    override fun createDocumentFragment(): IDocumentFragment =
        DocumentFragmentImpl(delegate.createDocumentFragment())

    override fun createTextNode(data: String): IText = TextImpl(delegate.createTextNode(data))

    override fun createCDATASection(data: String): ICDATASection {
        return CDATASectionImpl(delegate.createCDATASection(data))
    }

    override fun createComment(data: String): IComment = CommentImpl(delegate.createComment(data))

    override fun createProcessingInstruction(target: String, data: String): IProcessingInstruction =
        ProcessingInstructionImpl(delegate.createProcessingInstruction(target, data))

    override fun createAttribute(localName: String): IAttr = AttrImpl(delegate.createAttribute(localName))

    override fun createAttributeNS(namespace: String?, qualifiedName: String): IAttr =
        AttrImpl(delegate.createAttributeNS(namespace, qualifiedName))

    override fun createElementNS(namespaceURI: String, qualifiedName: String): IElement =
        ElementImpl(delegate.createElementNS(namespaceURI, qualifiedName))

    override fun createEntityReference(name: String?): EntityReference {
        return delegate.createEntityReference(name)
    }

    override fun normalizeDocument() {
        return delegate.normalizeDocument()
    }

    override fun renameNode(n: DomNode, namespaceURI: String?, qualifiedName: String): INode =
        delegate.renameNode(n.unWrap(), namespaceURI, qualifiedName).wrap()

    override fun adoptNode(node: DomNode): INode = delegate.adoptNode(node.unWrap()).wrap()

    override fun adoptNode(node: Node2): INode = delegate.adoptNode(node.unWrap()).wrap()

    override fun importNode(node: DomNode, deep: Boolean): INode =
        delegate.importNode(node.unWrap(), deep).wrap()

    override fun importNode(node: Node2, deep: Boolean): INode =
        delegate.importNode(node.unWrap(), deep).wrap()
}
