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

package nl.adaptivity.xmlutil.dom2

import nl.adaptivity.xmlutil.ExperimentalXmlUtilApi
import nl.adaptivity.xmlutil.dom.PlatformAttr
import nl.adaptivity.xmlutil.dom.PlatformNode

public expect interface Attr : Node, PlatformAttr {
    //region dom 1

    public fun getName(): String
    public fun getValue(): String
    public fun setValue(value: String)
    //TODO public fun isSpecified(): Boolean

    //region overrides
    public override fun getOwnerDocument(): Document

    public override fun appendChild(node: PlatformNode): Nothing

    @ExperimentalXmlUtilApi
    @IgnorableReturnValue
    override fun insertBefore(newChild: PlatformNode, refChild: PlatformNode?): Nothing

    public override fun replaceChild(newChild: PlatformNode, oldChild: PlatformNode): Nothing
    public override fun removeChild(node: PlatformNode): Nothing
    public override fun getFirstChild(): Nothing?
    public override fun getLastChild(): Nothing?

    @ExperimentalXmlUtilApi
    public override fun getAttributes(): Nothing?

    @ExperimentalXmlUtilApi
    override fun getNodeValue(): String

    @ExperimentalXmlUtilApi
    override fun cloneNode(deep: Boolean): Attr

    //endregion
    //endregion

    //region dom 2
    public fun getOwnerElement(): Element?

    //endregion

    //region dom 3
    //TODO public fun getSchemaTypeInfo(): TypeInfo?
    @ExperimentalXmlUtilApi
    public fun isId(): Boolean
    //endregion

}

@Suppress("EXTENSION_SHADOWED_BY_MEMBER")
public val Attr.namespaceURI: String? get() = getNamespaceURI()

@Suppress("EXTENSION_SHADOWED_BY_MEMBER")
public val Attr.prefix: String? get() = getPrefix()

@Suppress("EXTENSION_SHADOWED_BY_MEMBER")
public val Attr.localName: String? get() = getLocalName()

@Suppress("EXTENSION_SHADOWED_BY_MEMBER")
public val Attr.name: String get() = getName()

@Suppress("EXTENSION_SHADOWED_BY_MEMBER")
public var Attr.value: String
    get() = getValue()
    set(value) { setValue(value) }

@Suppress("EXTENSION_SHADOWED_BY_MEMBER")
public val Attr.ownerElement: Element? get() = getOwnerElement()
