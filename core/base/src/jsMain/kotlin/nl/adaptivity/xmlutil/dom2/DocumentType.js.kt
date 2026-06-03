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
import nl.adaptivity.xmlutil.dom.PlatformDocumentType
import nl.adaptivity.xmlutil.dom.PlatformNode

public actual interface DocumentType : Node, PlatformDocumentType {

    public actual fun getName(): String
    public actual fun getPublicId(): String
    public actual fun getSystemId(): String

    /* @since DOM Level 2 */
    public actual fun getEntities(): NamedNodeMap<Entity>

    /* @since DOM Level 2 */
    public actual fun getNotations(): NamedNodeMap<Notation>

    actual override fun getNodeValue(): Nothing?

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

    public actual override fun getAttributes(): Nothing?

    public actual override fun cloneNode(deep: Boolean): DocumentType


    @IgnorableReturnValue
    @Deprecated("Binary only", level = DeprecationLevel.HIDDEN)
    @Suppress("UNCHECKED_CAST_TO_EXTERNAL_INTERFACE")
    public override fun appendChild(node: Node): Nothing = appendChild(node.unsafeCast<PlatformNode>())

    @IgnorableReturnValue
    @Deprecated("Binary only", level = DeprecationLevel.HIDDEN)
    @Suppress("UNCHECKED_CAST_TO_EXTERNAL_INTERFACE")
    public override fun insertBefore(newChild: Node, refChild: Node?): Nothing =
        insertBefore(newChild.unsafeCast<PlatformNode>(), refChild?.unsafeCast<PlatformNode>())

    @IgnorableReturnValue
    @Deprecated("Binary only", level = DeprecationLevel.HIDDEN)
    @Suppress("UNCHECKED_CAST_TO_EXTERNAL_INTERFACE")
    public override fun replaceChild(newChild: Node, oldChild: Node): Nothing =
        replaceChild(newChild.unsafeCast<PlatformNode>(), oldChild.unsafeCast<PlatformNode>())

    @IgnorableReturnValue
    @Deprecated("Binary only", level = DeprecationLevel.HIDDEN)
    @Suppress("UNCHECKED_CAST_TO_EXTERNAL_INTERFACE")
    public override fun removeChild(node: Node): Nothing = removeChild(node.unsafeCast<PlatformNode>())

}
