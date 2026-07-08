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

package nl.adaptivity.xmlutil.dom2

import nl.adaptivity.xmlutil.dom.PlatformNamedNodeMap
import nl.adaptivity.xmlutil.dom.PlatformNode

public expect interface NamedNodeMap<out T: Node> : PlatformNamedNodeMap, Iterable<T> {

    /**
     * The size function works with collection interfaces rather than the traditional getLength interface.
     */
    public val size: Int

    public fun getLength(): Int /*= size*/

    public fun item(index: Int): T?

    public operator fun get(index: Int): T? //= item((index))

    public fun getNamedItem(qualifiedName: String): T?

    public fun getNamedItemNS(namespace: String?, localName: String): T?

    @IgnorableReturnValue
    public fun setNamedItem(attr: PlatformNode): T?

    @IgnorableReturnValue
    public fun setNamedItemNS(attr: PlatformNode): T?

    @IgnorableReturnValue
    public fun removeNamedItem(qualifiedName: String): T?

    @IgnorableReturnValue
    public fun removeNamedItemNS(namespace: String?, localName: String): T?

    public override operator fun iterator(): Iterator<T>
}

