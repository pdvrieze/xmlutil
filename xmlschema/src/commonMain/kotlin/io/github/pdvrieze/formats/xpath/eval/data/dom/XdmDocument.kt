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

@file:MustUseReturnValues

package io.github.pdvrieze.formats.xpath.eval.data.dom

import io.github.pdvrieze.formats.xpath.eval.data.XdmAtomic
import io.github.pdvrieze.formats.xpath.eval.data.XdmAtomicOrSequence
import io.github.pdvrieze.formats.xpath.eval.data.XdmNode
import io.github.pdvrieze.formats.xpath.eval.data.XdmNodeFriend
import io.github.pdvrieze.formats.xpath.eval.type.XdmSchemaType
import io.github.pdvrieze.formats.xpath.eval.type.XdmSingleType
import io.github.pdvrieze.formats.xpath.functions.Fn
import io.github.pdvrieze.formats.xpath.impl.ExprEvalContext
import io.github.pdvrieze.formats.xpath.impl.XPathInternal
import io.github.pdvrieze.xml.schematypes.Collation
import io.github.pdvrieze.xml.schematypes.types.AnySimpleType
import io.github.pdvrieze.xml.schematypes.types.UntypedType
import io.github.pdvrieze.xml.schematypes.values.XsdAtomic
import nl.adaptivity.xmlutil.dom.DOMException
import nl.adaptivity.xmlutil.dom.PlatformDocument
import nl.adaptivity.xmlutil.dom.PlatformDocumentType
import nl.adaptivity.xmlutil.dom.PlatformNode
import nl.adaptivity.xmlutil.dom2.DOMImplementation
import nl.adaptivity.xmlutil.dom2.Document
import nl.adaptivity.xmlutil.dom2.Node
import nl.adaptivity.xmlutil.dom2.impl.AbstractAttrStorage
import nl.adaptivity.xmlutil.dom2.impl.AbstractDocument
import nl.adaptivity.xmlutil.dom2.impl.AbstractNodeList
import nl.adaptivity.xmlutil.dom2.impl.LinearNodeStorage
import nl.adaptivity.xmlutil.dom2.textContent
import nl.adaptivity.xmlutil.isXmlWhitespace

@XPathInternal
class XdmDocument private constructor(doctype: XdmDocumentType?) :
    AbstractDocument<XdmNode<*>, XdmParentNode<*>>({
        NodeStorage(it as XdmDocument)
    }), XdmParentNode<XdmDocument>, Document {

    init {
        if (doctype?.getOwnerDocument() != null) throw DOMException.wrongDocumentErr("Document type already used for a different document")

        @OptIn(XdmNodeFriend::class)
        doctype?.setOwnerDocument(this)
    }

    private val _doctype = doctype
    override val self: XdmDocument get() = this

    override fun asT(): XdmDocument = this

    private val docId = nextDocId()
    override var posInParent: Int get() = -1
        @XdmNodeFriend set(value) {
            if (value >= 0) throw DOMException.hierarchyRequestErr("Cannot set position of a document")
        }

    // TODO this is not necessarily valid. Probably just maps to the document element.
    override var staticType: XdmSingleType = XdmSchemaType(UntypedType.Instance)
        private set

    private var _dynamicType: XdmSingleType? = null
    override val dynamicType: XdmSingleType
        get() = _dynamicType ?: staticType


    internal constructor(doctype1: PlatformDocumentType?) : this(doctype = doctype1?.let(XdmDocumentType::coerce))

    @XdmNodeFriend
    override fun setOwnerDocument(ownerDocument: XdmDocument): Nothing {
        throw UnsupportedOperationException("Cannot set owner document of a document")
    }

    override fun getDoctype(): XdmDocumentType? = _doctype

    override fun getImplementation(): DOMImplementation = XdmDOMImplementation

    private var _documentElement: XdmElement? = null
    override fun getDocumentElement(): XdmElement? = _documentElement

    private var _inputEncoding: String = "UTF-8"

    override fun getInputEncoding(): String = _inputEncoding

    override fun adoptNode(node: PlatformNode): XdmNode<*> {
        if (node !is XdmNode<*>) throw DOMException.notSupportedErr("node is of a different implementation and cannot be adopted")

        return adoptNodeImpl(node)
    }

    override fun createDocumentFragment(): XdmDocumentFragment {
        return XdmDocumentFragment(this)
    }

    override fun createElement(localName: String): XdmElement {
        if (localName.isEmpty()) throw DOMException.invalidCharacterErr("Element name cannot be empty")
        if (localName.indexOf(':') >= 0) throw DOMException.namespaceErr("Prefix in name without namespace uri")
        return XdmElement(this, null, localName, null)
    }

    override fun createElementNS(namespaceURI: String, qualifiedName: String): XdmElement {
        val cPos = qualifiedName.indexOf(':')

        val localName = if (cPos < 0) qualifiedName else qualifiedName.substring(cPos + 1)

        if (localName.isEmpty()) throw DOMException.invalidCharacterErr("Element name cannot be empty")
        val prefix = when {
            cPos < 0 -> null
            else -> {
                qualifiedName.substring(0, cPos).also {
                    if (namespaceURI.isEmpty()) throw DOMException.namespaceErr("Missing namespace in presence of a prefix")
                }
            }
        }

        return XdmElement(this, namespaceURI, localName, prefix)
    }

    override fun createAttribute(localName: String): XdmAttr {
        return XdmAttr(
            ownerDocument = this,
            namespaceURI = null,
            localName = localName,
            prefix = null,
            value = "",
        )
    }

    override fun createAttributeNS(namespace: String?, qualifiedName: String): XdmAttr {
        val localName = qualifiedName.substringAfterLast(':', qualifiedName)
        val prefix = qualifiedName.substringBeforeLast(':', "").takeUnless { it.isEmpty() }
        return XdmAttr(
            ownerDocument = this,
            namespaceURI = namespace,
            localName = localName,
            prefix = prefix,
            value = ""
        )
    }

    override fun createTextNode(data: String): XdmText {
        return XdmText(this, data)
    }

    override fun createCDATASection(data: String): XdmCDATASection {
        return XdmCDATASection(this, data)
    }

    override fun createComment(data: String): XdmComment {
        return XdmComment(this, data)
    }

    override fun createProcessingInstruction(target: String, data: String): XdmProcessingInstruction {
        return XdmProcessingInstruction(this, target, data)
    }

    override fun importNode(
        node: PlatformNode,
        deep: Boolean
    ): XdmNode<*> {
        return super<AbstractDocument>.importNode(node, deep)
    }

    override fun cloneNode(deep: Boolean): XdmDocument {
        return XdmDocument(doctype = _doctype?.cloneNode(deep)).also { d ->
            if (deep) {
                for (c in getChildNodes()) d.appendChild(
                    c.cloneNode(deep)
                )
            }
        }
    }

    context(ctx: ExprEvalContext)
    override fun atomize(): XdmAtomicOrSequence<XdmAtomic<XsdAtomic>> {
        return Fn.string(this).atomize() as XdmAtomic<*>
    }

    context(ctx: ExprEvalContext)
    override fun isNodeEqual(rightNode: Node, collation: Collation): Boolean {
        return collation.equals(textContent ?: return false, rightNode.textContent ?: return false)

    }

    context(ctx: ExprEvalContext)
    override fun isDeepEqual(other: XdmNode<*>, collation: Collation): Boolean {
        return this === other
//        if (nodeType != other.nodeType) return false
//        return collation.equals(this.getTextContent() ?: return false, other.getTextContent() ?: return false)
    }

    override fun toString(): String = when (val e = _documentElement) {
        null -> "<Empty Document>"
        else -> "document<$docId>"
//        else -> e.toString()
    }

    companion object {
        private var nextDocId: Int = 1

        private fun nextDocId(): Int = nextDocId++

        internal fun coerce(document: PlatformDocument): XdmDocument {
            return (document as? XdmDocument) ?: throw DOMException.notSupportedErr("Documents can not be adopted")
        }
    }

    private class NodeStorage(private val document: XdmDocument): LinearNodeStorage<XdmNode<*>, XdmParentNode<*>>(StorageAdapter(document)), AbstractNodeList<XdmNode<*>, XdmParentNode<*>> {
        private var docElem: XdmElement?
            get() = document._documentElement
            set(value) { document._documentElement = value}

        override fun appendChild(parent: XdmParentNode<*>, node: XdmNode<*>) {
            when (node) {
                is XdmElement -> when (docElem) {
                    null -> docElem = node
                    else -> throw DOMException.hierarchyRequestErr("Documents may only have one root element")

                }

                is XdmCDATASection -> throw DOMException.hierarchyRequestErr("CDATA sections cannot be added directly to a document")

                is XdmText if (! isXmlWhitespace(node.getData())) ->
                    throw DOMException.hierarchyRequestErr("Non-whitespace text nodes cannot be added directly to a document")
            }
            super.appendChild(parent, node)
        }

        override fun removeChild(parent: XdmParentNode<*>, node: XdmNode<*>): XdmNode<*> {
            if (node === docElem) docElem = null
            return super.removeChild(parent, node)
        }

        override fun replaceChild(parent: XdmParentNode<*>, newChild: XdmNode<*>, oldChild: XdmNode<*>): XdmNode<*> {
            if (oldChild === docElem) docElem = null

            when (newChild) {
                is XdmElement -> when (docElem) {
                    null -> docElem = newChild
                    else -> throw DOMException.hierarchyRequestErr("Document may only have one root element")
                }

                is XdmCDATASection -> throw DOMException.hierarchyRequestErr("CDATA sections cannot be added directly to a document")

                is XdmText if (! isXmlWhitespace(newChild.getData())) ->
                    throw DOMException.hierarchyRequestErr("Non-whitespace text nodes cannot be added directly to a document")
            }

            return super.replaceChild(parent, newChild, oldChild)
        }
    }

    internal val storageAdapter: StorageAdapter get() = (nodeStorage as NodeStorage).adapter as StorageAdapter

    internal class StorageAdapter(private val ownerDocument: XdmDocument): LinearNodeStorage.Adapter<XdmNode<*>, XdmParentNode<*>>, AbstractAttrStorage.Adapter<XdmAttr> {
        override fun setParentAndUpdateChildPos(
            parent: XdmParentNode<*>?,
            node: XdmNode<*>,
            newPos: Int
        ) {
            val oldParent = node.getParentNode()
            super.setParentAndUpdateChildPos(parent, node, newPos)
            @OptIn(XdmNodeFriend::class)
            node.posInParent = newPos

            if (parent != null) {
                if (parent != oldParent) {

                    @OptIn(XdmNodeFriend::class)
                    when (node) { // attributes and character data have the type of the parent
                        is XdmAttr -> node.staticType = parent.staticType

                        is XdmCharacterData if (parent.staticType.let {
                            it is XdmSchemaType && it.schemaType.derivesFrom(
                                AnySimpleType.Instance
                            )
                        }) -> node.staticType = parent.staticType

                        is XdmCharacterData -> {
                            // TODO update the type from the position + schema
                        }

                        is XdmElement -> {
                            // TODO update the type from the position + schema
                        }
                    }

                }
            }
        }

        override fun getChildPosHint(node: XdmNode<*>): Int {
            return node.posInParent
        }

        override fun checkTypeAndOwner(node: PlatformNode): XdmNode<*> = when (node) {
            !is XdmNode<*> -> throw DOMException.wrongDocumentErr("Unexpected node implementation, try importing")
            else if node.getOwnerDocument() != ownerDocument -> throw DOMException.wrongDocumentErr("Node not owned by this document")
            else -> node
        }

        override fun checkAttr(a: PlatformNode): XdmAttr {
            return when (a) {
                is XdmAttr -> a
                else -> throw DOMException.wrongDocumentErr("Unexpected node implementation, try importing")
            }
        }
    }

}
