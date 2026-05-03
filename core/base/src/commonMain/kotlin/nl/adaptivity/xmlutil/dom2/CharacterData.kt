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

import nl.adaptivity.xmlutil.dom.PlatformCharacterData
import nl.adaptivity.xmlutil.dom.PlatformNode

public expect interface CharacterData : Node, PlatformCharacterData {
    public override fun getOwnerDocument(): Document

    override fun getNodeValue(): String

    public fun getData(): String

    public fun setData(data: String)

    public fun substringData(offset: Int, count: Int): String

    public fun appendData(data: String)

    public fun insertData(offset: Int, data: String)

    public fun deleteData(offset: Int, count: Int)

    public fun replaceData(offset: Int, count: Int, data: String)

    @IgnorableReturnValue
    override fun appendChild(node: PlatformNode): Nothing

    @IgnorableReturnValue
    override fun replaceChild(newChild: PlatformNode, oldChild: PlatformNode): Nothing

    @IgnorableReturnValue
    override fun removeChild(node: PlatformNode): Nothing
    override fun getFirstChild(): Nothing?
    override fun getLastChild(): Nothing?
}

@Suppress("EXTENSION_SHADOWED_BY_MEMBER")
public inline var CharacterData.data: String
    get() = getData()
    set(value) { setData(value) }
