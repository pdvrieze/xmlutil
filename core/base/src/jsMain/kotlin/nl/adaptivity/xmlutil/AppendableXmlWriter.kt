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

package nl.adaptivity.xmlutil

import nl.adaptivity.xmlutil.core.impl.wrappingDom.JsWrappedDocument
import nl.adaptivity.xmlutil.core.impl.wrappingDom.unWrap
import nl.adaptivity.xmlutil.dom2.CDATASection
import nl.adaptivity.xmlutil.dom2.Comment
import nl.adaptivity.xmlutil.dom2.Document
import nl.adaptivity.xmlutil.dom2.DocumentFragment
import nl.adaptivity.xmlutil.dom2.DocumentType
import nl.adaptivity.xmlutil.dom2.Element
import nl.adaptivity.xmlutil.dom2.Node
import nl.adaptivity.xmlutil.dom2.ProcessingInstruction
import nl.adaptivity.xmlutil.dom2.Text
import nl.adaptivity.xmlutil.dom2.attributes
import org.w3c.dom.parsing.XMLSerializer

internal class AppendableXmlWriter(private val target: Appendable, private val delegate: DomWriter) :
    XmlWriter by delegate {

    private fun appendToTarget(node: Node) {
        when (node) {
            is Document -> for (c in node.childNodes) appendToTarget(c)
            is DocumentFragment -> for (c in node.childNodes) appendToTarget(c)
            is Comment -> target.append("<!--${node.textContent}-->")
            is CDATASection -> target.append("<![CDATA[${node.textContent}]]>")
            is Text -> target.append(node.textContent)
            is ProcessingInstruction -> target.append("<?${node.target} ${node.data}?>")
            is DocumentType -> target.append("<!DOCTYPE ${node.name} ${node.publicId} ${node.systemId}>")
            is Element -> {
                target.append("<${node.nodeName}")
                for (attr in node.attributes) {
                    target.append(" ${attr.nodeName}=\"${attr.nodeValue}\"")
                }
                if (! node.hasChildNodes()) {
                    target.append("/>")
                } else {
                    target.append(">")
                    for (child in node.childNodes) {
                        appendToTarget(child)
                    }
                    target.append("</${node.nodeName}>")
                }
            }
        }
    }

    override fun close() {
        try {
            when (val t = delegate.target) {
                is JsWrappedDocument -> {
                    val xmls = XMLSerializer()
                    val domText = xmls.serializeToString(delegate.target.unWrap())
                    target.append(domText)
                }

                else -> appendToTarget(t)
            }
        } finally {
            delegate.close()
        }
    }

    override fun flush() {
        delegate.flush()
    }

    override var indentString: String
        get() = delegate.indentString
        set(value) {
            delegate.indentString = value
        }

    override fun namespaceAttr(namespace: Namespace) {
        delegate.namespaceAttr(namespace)
    }

    override fun processingInstruction(target: String, data: String) {
        delegate.processingInstruction(target, data)
    }
}
