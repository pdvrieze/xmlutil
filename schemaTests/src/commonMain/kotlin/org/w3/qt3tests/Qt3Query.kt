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
import nl.adaptivity.xmlutil.serialization.XmlValue

/**
 * The content of the element is an XQuery expression to be evaluated. This should return a
 * sequence equivalent to the contents of a sequence which can used as input to test cases.
 * Specifically used in fn:collection. The scope of the &lt;query&gt; element is the parent
 * &lt;collection&gt; element in which it appears.
 *
 * For example: `unparsed-text-lines('xxx')!parse-json()`
 */
@Serializable
@XmlSerialName("query", QT3TNS)
class Qt3Query(@XmlValue val xQueryExpression: String) {

}

