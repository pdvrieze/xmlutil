/*
 * Copyright (c) 2024-2025.
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

package nl.adaptivity.xmlutil.core.impl.idom

import nl.adaptivity.xmlutil.dom.PlatformDocument
import nl.adaptivity.xmlutil.dom.PlatformNode

public interface IDocument : INode, PlatformDocument {
    override fun getInputEncoding(): String? = characterSet

    override fun getImplementation(): IDOMImplementation

    override fun getDoctype(): IDocumentType?

    override fun getDocumentElement(): IElement?

    override fun createElement(localName: String): IElement

    override fun createDocumentFragment(): IDocumentFragment

    override fun createTextNode(data: String): IText

    override fun createCDATASection(data: String): ICDATASection

    override fun createComment(data: String): IComment

    override fun createProcessingInstruction(target: String, data: String): IProcessingInstruction

    override fun adoptNode(node: PlatformNode): INode

    override fun createAttribute(localName: String): IAttr

    override fun createAttributeNS(namespace: String?, qualifiedName: String): IAttr

    override fun createElementNS(namespaceURI: String, qualifiedName: String): IElement

    override fun importNode(node: PlatformNode): INode = importNode(node, false)

    override fun importNode(node: PlatformNode, deep: Boolean): INode
}
