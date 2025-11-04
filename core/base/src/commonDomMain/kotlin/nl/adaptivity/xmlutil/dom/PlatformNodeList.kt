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
import nl.adaptivity.xmlutil.core.impl.idom.INodeList
import nl.adaptivity.xmlutil.dom.PlatformNodeList
import nl.adaptivity.xmlutil.dom.PlatformNodeList as NodeList1

@Deprecated(
    "Use INodeList that contains extended functions",
    ReplaceWith("INodeList", "nl.adaptivity.xmlutil.core.impl.idom.INodeList")
)
public actual interface PlatformNodeList {
    public fun item(index: Int): PlatformNode?
}

@Suppress("NOTHING_TO_INLINE", "DEPRECATION", "KotlinRedundantDiagnosticSuppress")
@XmlUtilInternal
public actual inline fun NodeList1.getLength(): Int = (this as INodeList).size

@XmlUtilInternal
public actual operator fun PlatformNodeList.get(index: Int): PlatformNode? = item(index)
