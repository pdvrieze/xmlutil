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
import nl.adaptivity.xmlutil.dom.PlatformNode
import nl.adaptivity.xmlutil.dom.PlatformProcessingInstruction

public actual interface ProcessingInstruction : Node, PlatformProcessingInstruction {

    actual override fun getOwnerDocument(): Document

    public actual fun getTarget(): String
    public actual fun getData(): String
    public actual fun setData(data: String)

    public actual override fun getNodeValue(): String

    public actual override fun cloneNode(deep: Boolean): ProcessingInstruction

    @IgnorableReturnValue
    actual override fun appendChild(node: PlatformNode): Nothing

    @ExperimentalXmlUtilApi
    @IgnorableReturnValue
    actual override fun insertBefore(newChild: PlatformNode, refChild: PlatformNode?): Nothing

    @IgnorableReturnValue
    actual override fun replaceChild(newChild: PlatformNode, oldChild: PlatformNode): Nothing

    @IgnorableReturnValue
    actual override fun removeChild(node: PlatformNode): Nothing

}

internal fun addProcessingInstructionPropertiesToPrototype(prototype: dynamic, inherit: Boolean = true) {
    if (inherit) addNodePropertiesToPrototype(prototype)
    val props = js("{}")
    props.target = jsProperty<ProcessingInstruction> { getTarget() }
    props.data = jsProperty<ProcessingInstruction>(getter = { getData() }, setter = { setData(it) })
    js("Object").defineProperties(prototype, props)
}
