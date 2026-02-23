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

package org.w3.qt3tests.assertions

import io.github.pdvrieze.formats.xpath.XPathExpression
import kotlinx.serialization.Serializable
import nl.adaptivity.xmlutil.serialization.XmlSerialName
import nl.adaptivity.xmlutil.serialization.XmlValue
import org.w3.qt3tests.QT3TNS

/**
 * The assert element contains an XPath expression whose effective boolean value must be true;
 * usually the expression will use the variable `$result` which references the result of the
 * expression. 
 * 
 * For example, `&lt;assert&gt;matches(string($result), '[0-9]{3}')&lt;/assert&gt;` asserts that
 * the result of the test expression is a three-digit number.</p>
 */
@Serializable
@XmlSerialName("assert", QT3TNS)
class Qt3Assert(@XmlValue val assertion: XPathExpression): Qt3AbstractAssertion()

