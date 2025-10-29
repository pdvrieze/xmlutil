/*
 * Copyright (c) 2025.
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

import nl.adaptivity.xmlutil.core.impl.dom.unWrap
import nl.adaptivity.xmlutil.core.impl.multiplatform.Writer
import nl.adaptivity.xmlutil.dom2.Node
import nl.adaptivity.xmlutil.dom2.firstChild
import nl.adaptivity.xmlutil.dom2.nextSibling
import org.w3c.dom.parsing.XMLSerializer

internal class WriterXmlWriter(private val target: Writer, private val delegate: DomWriter) : XmlWriter by delegate {

    private val owner: Node = delegate.currentNode ?: delegate.target

    override fun close() {
        try {
            val xmls = XMLSerializer()

            if (delegate.currentNode != null) {
                val domText = buildString {
                    var c = owner.firstChild
                    while (c != null) {
                        append(xmls.serializeToString(c.unWrap()))
                        c = c.nextSibling
                    }
                }

                val xmlDeclMode = delegate.xmlDeclMode
                if (xmlDeclMode != XmlDeclMode.None) {
                    val encoding = when (xmlDeclMode) {
                        XmlDeclMode.Charset -> delegate.requestedEncoding ?: "UTF-8"
                        else -> when (delegate.requestedEncoding?.lowercase()?.startsWith("utf-")) {
                            false -> delegate.requestedEncoding
                            else -> null
                        }
                    }

                    val xmlVersion = delegate.requestedVersion ?: "1.0"

                    target.write("<?xml version=\"")
                    target.write(xmlVersion)
                    target.write("\"")
                    if (encoding != null) {
                        target.write(" encoding=\"")
                        target.write(encoding)
                        target.write("\"")
                    }
                    target.write("?>")
                    if (delegate.indentSequence.isNotEmpty()) {
                        target.write("\n")
                    }
                }

                target.write(domText)
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
