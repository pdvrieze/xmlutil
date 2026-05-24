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
import nl.adaptivity.xmlutil.dom.PlatformDocumentType
import nl.adaptivity.xmlutil.dom.PlatformNode

public expect interface DocumentType : Node, PlatformDocumentType {
    public fun getName(): String
    public fun getPublicId(): String
    public fun getSystemId(): String

    /* @since DOM Level 2 */
    public fun getEntities(): NamedNodeMap<Entity>

    /* @since DOM Level 2 */
    public fun getNotations(): NamedNodeMap<Notation>

    public override fun appendChild(node: PlatformNode): Nothing
    @ExperimentalXmlUtilApi
    @IgnorableReturnValue
    override fun insertBefore(newChild: PlatformNode, refChild: PlatformNode?): Nothing
    public override fun replaceChild(newChild: PlatformNode, oldChild: PlatformNode): Nothing
    public override fun removeChild(node: PlatformNode): Nothing
    override fun getFirstChild(): Nothing?
    override fun getLastChild(): Nothing?

    override fun getNodeValue(): Nothing?

    override fun getAttributes(): Nothing?
    override fun cloneNode(deep: Boolean): DocumentType
}

@Suppress("EXTENSION_SHADOWED_BY_MEMBER")
public val DocumentType.name: String get() = getName()

@Suppress("EXTENSION_SHADOWED_BY_MEMBER")
public val DocumentType.publicId: String get() = getPublicId()

@Suppress("EXTENSION_SHADOWED_BY_MEMBER")
public val DocumentType.systemId: String get() = getSystemId()
