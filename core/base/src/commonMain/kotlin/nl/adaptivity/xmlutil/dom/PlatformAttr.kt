/*
 * Copyright (c) 2024-2025.
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

@file:Suppress("DEPRECATION")

package nl.adaptivity.xmlutil.dom

import nl.adaptivity.xmlutil.XmlUtilInternal

@Deprecated(
    "No longer supported, use dom2 instead",
    ReplaceWith("nl.adaptivity.xmlutil.dom2.Attr", "nl.adaptivity.xmlutil.dom2")
)
public expect interface PlatformAttr : PlatformNode

@XmlUtilInternal
public expect inline fun PlatformAttr.getNamespaceURI(): String?
@XmlUtilInternal
public expect inline fun PlatformAttr.getPrefix(): String?
@XmlUtilInternal
public expect inline fun PlatformAttr.getLocalName(): String?
@XmlUtilInternal
public expect inline fun PlatformAttr.getName(): String

internal expect inline fun PlatformAttr.getValue(): String
internal expect inline fun PlatformAttr.setValue(value: String)
internal expect inline fun PlatformAttr.getOwnerElement(): PlatformElement?

