/*
 * Copyright (c) 2026.
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

package nl.adaptivity.xml.serialization

import nl.adaptivity.xmlutil.dom.PlatformDOMImplementation
import nl.adaptivity.xmlutil.dom2.Document
import org.xml.sax.InputSource
import java.io.StringReader
import javax.xml.namespace.QName
import javax.xml.parsers.DocumentBuilderFactory

actual fun PlatformDOMImplementation.parse(input: String): Document {
    return DocumentBuilderFactory
        .newInstance()
        .apply { this.isNamespaceAware = true }
        .newDocumentBuilder()
        .parse(InputSource(StringReader(input)))
        .let {
            nl.adaptivity.xmlutil.util.impl.createDocument(QName("XX")).also { document ->
                document.replaceChild(document.importNode(it.documentElement, true), document.getDocumentElement()!!)
            }
        }
}
