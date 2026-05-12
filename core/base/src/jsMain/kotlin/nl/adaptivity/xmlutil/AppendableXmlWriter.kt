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

import nl.adaptivity.xmlutil.core.XmlVersion
import nl.adaptivity.xmlutil.core.impl.wrappingDom.JsWrappedDocument
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

    private fun writeXmlDecl() {
        val xmlDeclMode = delegate.xmlDeclMode.resolve(delegate.requestedVersion?.let { XmlVersion.fromStringOrNull(it) })
        if (xmlDeclMode != XmlDeclMode.None) {
            val encoding = when (xmlDeclMode) {
                XmlDeclMode.Charset -> delegate.requestedEncoding ?: "UTF-8"
                else -> when (delegate.requestedEncoding?.lowercase()?.startsWith("utf-")) {
                    false -> delegate.requestedEncoding
                    else -> null
                }
            }

            val xmlVersion = delegate.requestedVersion ?: "1.0"

            target.append("<?xml version=\"")
            target.append(xmlVersion)
            target.append("\"")
            if (encoding != null) {
                target.append(" encoding=\"")
                target.append(encoding)
                target.append("\"")
            }
            target.append("?>")
            if (delegate.indentSequence.isNotEmpty()) {
                target.append("\n")
            }
        }

    }

    override fun close() {
        try {
            when (val doc = delegate.target) {
                is JsWrappedDocument -> {
                    val xmls = XMLSerializer()
                    val domText = xmls.serializeToString(doc.delegate)
                    target.append(domText)
                }

                else -> {
                    writeXmlDecl()
                    doc.appendToTarget(target)
                }
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

internal fun Node.appendToTarget(target: Appendable) {
    when (this) {
        is Document -> for (c in childNodes) c.appendToTarget(target)
        is DocumentFragment -> for (c in childNodes) c.appendToTarget(target)
        is Comment -> target.append("<!--${textContent}-->")
        is CDATASection -> target.append("<![CDATA[${textContent}]]>")
        is Text -> target.append(textContent?.xmlEncode())
        is ProcessingInstruction -> target.append("<?${target} ${data}?>")
        is DocumentType -> target.append("<!DOCTYPE ${name} ${publicId} ${systemId}>")
        is Element -> {
            target.append("<${nodeName}")
            for (attr in attributes) {
                target.append(" ${attr.nodeName}=\"${attr.nodeValue}\"")
            }
            if (!hasChildNodes()) {
                target.append("/>")
            } else {
                target.append(">")
                for (child in childNodes) {
                    child.appendToTarget(target)
                }
                target.append("</${nodeName}>")
            }
        }
    }
}

