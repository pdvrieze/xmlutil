/*
 * Copyright (c) 2024.
 *
 * This file is part of xmlutil.
 *
 * This file is licenced to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You should have received a copy of the license with the source distribution.
 * Alternatively, you may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

@file:Suppress("NOTHING_TO_INLINE")

package nl.adaptivity.xmlutil.dom

@Suppress(
    "ACTUAL_CLASSIFIER_MUST_HAVE_THE_SAME_MEMBERS_AS_NON_FINAL_EXPECT_CLASSIFIER_WARNING",
    "NON_ACTUAL_MEMBER_DECLARED_IN_EXPECT_NON_FINAL_CLASSIFIER_ACTUALIZATION_WARNING",
)
public actual external interface Attr : Node {
    public val namespaceURI: String?
    public val prefix: String?
    public val localName: String
    public val name: String
    public var value: String
    public val ownerElement: Element?
}

public actual inline fun Attr.getNamespaceURI(): String? = namespaceURI
public actual inline fun Attr.getPrefix(): String? = prefix
public actual inline fun Attr.getLocalName(): String? = localName
public actual inline fun Attr.getName(): String = name
public actual inline fun Attr.getValue(): String = value
public actual inline fun Attr.setValue(value: String) {
    this.value = value
}

public actual inline fun Attr.getOwnerElement(): Element? = ownerElement

