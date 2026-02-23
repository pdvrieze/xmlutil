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

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import nl.adaptivity.xmlutil.SerializableQName
import nl.adaptivity.xmlutil.serialization.XmlSerialName

/**
 * An element within an environment that declares a variable that can be referenced within test
 * expressions that use this environment.
 *
 * The value to be bound to the variable is given in the select attribute, which should be a
 * simple XPath expression - typically a literal, or a simple call on a constructor function.
 *
 * The test expression may or may not include a declaration of the variable (so it can be
 * executed if appropriate using XPath). The "declared" attribute will be present with the value
 * "`true`" if the variable is declared in the query prolog. The test driver can add a declaration
 * of the variable to the query prolog if required. If the test expression includes the string
 * "`(:%VARDECL%:)`" then the variable declaration should be added to replace this string; if it
 * does not include this string, the variable declaration can be added at the start.
 */
@Serializable
@XmlSerialName("param", QT3TNS)
class Qt3Param(
    val name: SerializableQName,
    val select: String? = null,
    @SerialName("as") val asType: String? = null,
    val source: String? = null,
    val declared: Boolean = false,
): Qt3Environment.Element
