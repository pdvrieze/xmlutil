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

import nl.adaptivity.xmlutil.dom.PlatformDocumentType
import nl.adaptivity.xmlutil.dom.PlatformNode

public actual interface DocumentType : Node, PlatformDocumentType {
    public actual override fun getName(): String
    public actual override fun getPublicId(): String
    public actual override fun getSystemId(): String
    actual override fun getNodeValue(): Nothing?
    override fun getOwnerDocument(): Document?

    @IgnorableReturnValue
    public actual override fun appendChild(node: PlatformNode): Nothing

    @IgnorableReturnValue
    public actual override fun replaceChild(newChild: PlatformNode, oldChild: PlatformNode): Nothing

    @IgnorableReturnValue
    public actual override fun removeChild(node: PlatformNode): Nothing

    public actual override fun getFirstChild(): Nothing?
    public actual override fun getLastChild(): Nothing?
}
