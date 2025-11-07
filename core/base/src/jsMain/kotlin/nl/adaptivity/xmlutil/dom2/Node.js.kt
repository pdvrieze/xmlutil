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

package nl.adaptivity.xmlutil.dom2

import kotlinx.serialization.Serializable

@Serializable(with = NodeSerializer::class)
public actual interface Node {
    public actual fun getNodetype(): NodeType
    public actual fun getNodeName(): String
    public actual fun getOwnerDocument(): Document
    public actual fun getParentNode(): Node?
    public actual fun getTextContent(): String?
    public actual fun setTextContent(value: String)
    public actual fun getChildNodes(): NodeList
    public actual fun getFirstChild(): Node?
    public actual fun getLastChild(): Node?
    public actual fun getPreviousSibling(): Node?
    public actual fun getNextSibling(): Node?
    public actual fun getParentElement(): Element?
    public actual fun lookupPrefix(namespace: String): String?
    public actual fun lookupNamespaceURI(prefix: String): String?
    public actual fun appendChild(node: Node): Node
    public actual fun replaceChild(oldChild: Node, newChild: Node): Node
    public actual fun removeChild(node: Node): Node
}
