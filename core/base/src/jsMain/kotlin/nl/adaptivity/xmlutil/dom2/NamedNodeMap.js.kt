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

public actual interface NamedNodeMap : Iterable<Attr> {
    public actual val size: Int

    @Deprecated(
        message = "Use size instead",
        replaceWith = ReplaceWith(expression = "size"),
        level = DeprecationLevel.WARNING
    )
    public actual fun getLength(): Int
    public actual fun item(index: Int): Attr?
    public actual operator fun get(index: Int): Attr?
    public actual fun getNamedItem(qualifiedName: String): Attr?
    public actual fun getNamedItemNS(namespace: String?, localName: String): Attr?
    public actual fun setNamedItem(attr: Attr): Attr?
    public actual fun setNamedItemNS(attr: Attr): Attr?
    public actual fun removeNamedItem(qualifiedName: String): Attr?
    public actual fun removeNamedItemNS(namespace: String?, localName: String): Attr?
    public actual override operator fun iterator(): Iterator<Attr>
}
