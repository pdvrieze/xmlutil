/*
 * Copyright (c) 2026.
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

package org.w3.qt3tests

import kotlinx.serialization.Serializable
import nl.adaptivity.xmlutil.serialization.XmlSerialName

/**
 * An element within an environment that declares the value of the context item for a query.
 *
 * The value to be bound to the variable is given in the select attribute, which should be a
 * simple XPath expression - typically a literal, or a simple call on a constructor function.
 *
 * The query is called with the value of the select expression as the context item.
 */
@Serializable
@XmlSerialName("context-item", QT3TNS)
class Qt3ContextItem(
    val select: String? = null
): Qt3Environment.Element
