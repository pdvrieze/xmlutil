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
import org.w3c.dom.parsing.XMLSerializer

internal class AppendableXmlWriter(private val target: Appendable, private val delegate: DomWriter) :
    XmlWriter by delegate {

    override fun close() {
        try {
            val xmls = XMLSerializer()
            val domText = xmls.serializeToString(delegate.target.unWrap())
            target.append(domText)
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
