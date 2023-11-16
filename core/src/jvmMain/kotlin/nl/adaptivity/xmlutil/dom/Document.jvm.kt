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

package nl.adaptivity.xmlutil.dom

public actual typealias Document = org.w3c.dom.Document

@Suppress("EXTENSION_SHADOWED_BY_MEMBER")
public actual fun Document.createElement(localName: String): Element = createElement(localName)

@Suppress("EXTENSION_SHADOWED_BY_MEMBER")
public actual fun Document.createElementNS(namespaceURI: String, qualifiedName: String): Element =
    createElementNS(namespaceURI, qualifiedName)

@Suppress("EXTENSION_SHADOWED_BY_MEMBER")
public actual fun Document.createDocumentFragment(): DocumentFragment =
    createDocumentFragment()

@Suppress("EXTENSION_SHADOWED_BY_MEMBER")
public actual fun Document.createTextNode(data: String): Text =
    createTextNode(data)

@Suppress("EXTENSION_SHADOWED_BY_MEMBER")
public actual fun Document.createCDATASection(data: String): CDATASection =
    createCDATASection(data)

@Suppress("EXTENSION_SHADOWED_BY_MEMBER")
public actual fun Document.createComment(data: String): Comment =
    createComment(data)

@Suppress("EXTENSION_SHADOWED_BY_MEMBER")
public actual fun Document.createProcessingInstruction(target: String, data: String): ProcessingInstruction =
    createProcessingInstruction(target, data)
