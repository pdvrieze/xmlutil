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

@file:MustUseReturnValues

package nl.adaptivity.xmlutil.dom2

import nl.adaptivity.xmlutil.ExperimentalXmlUtilApi
import nl.adaptivity.xmlutil.dom.PlatformAttr
import nl.adaptivity.xmlutil.dom.PlatformNode
import org.w3c.dom.TypeInfo

public actual interface Attr : Node, PlatformAttr {
    public actual override fun getName(): String
    public actual override fun getValue(): String
    public actual override fun setValue(value: String)
    public actual override fun getOwnerElement(): Element?

    actual override fun getAttributes(): Nothing?

    @IgnorableReturnValue
    public actual override fun appendChild(node: PlatformNode): Nothing

    @ExperimentalXmlUtilApi
    @IgnorableReturnValue
    actual override fun insertBefore(newChild: PlatformNode, refChild: PlatformNode?): Nothing

    @IgnorableReturnValue
    public actual override fun replaceChild(newChild: PlatformNode, oldChild: PlatformNode): Nothing

    @IgnorableReturnValue
    public actual override fun removeChild(node: PlatformNode): Nothing

    public actual override fun getFirstChild(): Nothing?
    public actual override fun getLastChild(): Nothing?

    actual override fun getNodeValue(): String

    actual override fun cloneNode(deep: Boolean): Attr

    /**
     * Always true for processing that does not have defaults
     */
    override fun getSpecified(): Boolean = true

    override fun getSchemaTypeInfo(): TypeInfo? {
        return null
    }

    actual override fun isId(): Boolean

    actual override fun getOwnerDocument(): Document

    @IgnorableReturnValue
    @Deprecated("Binary only", level = DeprecationLevel.HIDDEN)
    public override fun appendChild(node: Node): Nothing = appendChild(node as PlatformNode)

    @IgnorableReturnValue
    @Deprecated("Binary only", level = DeprecationLevel.HIDDEN)
    public override fun insertBefore(newChild: Node, refChild: Node?): Nothing =
        insertBefore(newChild as PlatformNode, refChild as PlatformNode?)

    @IgnorableReturnValue
    @Deprecated("Binary only", level = DeprecationLevel.HIDDEN)
    public override fun replaceChild(newChild: Node, oldChild: Node): Nothing =
        replaceChild(newChild as PlatformNode, oldChild as PlatformNode)

    @IgnorableReturnValue
    @Deprecated("Binary only", level = DeprecationLevel.HIDDEN)
    public override fun removeChild(node: Node): Nothing = removeChild(node as PlatformNode)

}
