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

package nl.adaptivity.xmlutil.dom

@JsName("Node")
public actual external interface PlatformNode {

    @JsName("lookupPrefix")
    public fun lookupPrefix(namespace: String): String?

    @JsName("lookupNamespaceURI")
    public fun lookupNamespaceURI(prefix: String): String?

}

public actual val PlatformNode.ownerDocument: PlatformDocument? get() = asDynamic().ownerDocument
public val PlatformNode.baseURI: String get() = asDynamic().baseURI
public actual val PlatformNode.nodeType: Short get() = asDynamic().nodeType

internal actual fun PlatformNode.asPlatformAttr(): PlatformAttr = asDynamic()
internal actual fun PlatformNode.asPlatformElement(): PlatformElement = asDynamic()
internal actual fun PlatformNode.asPlatformDocumentFragment(): PlatformDocumentFragment = asDynamic()
internal actual fun PlatformNode.asPlatformCharacterData(): PlatformCharacterData = asDynamic()
internal actual fun PlatformNode.asPlatformText(): PlatformText = asDynamic()
internal actual fun PlatformNode.asPlatformProcessingInstruction(): PlatformProcessingInstruction = asDynamic()

