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
 * The `decimal-format` element allows a decimal format to be defined as part of the static context
 * for evaluating an XPath expression that calls the `format-number()` function.
 *     
 * When the `decimal-format` element is used in an environment, the test expression will always be
 * a simple XPath expression. If the test is to be run using an XQuery processor, the decimal
 * format can be added to the static context either by using the processor's API, or by
 * constructing a query prolog containing a `declare decimal format` declaration and prepending
 * this to the test expression.
 * 
 * The mechanism is used for testing the format-number() function. As such, the decimal format
 * being defined should always be valid. Tests for invalid decimal formats should be written
 * as XQuery tests with an explicit query prolog (or the equivalent in XSLT).
 *     
 * Test Catalog006 ensures that the decimal-format element is only used in tests that are pure
 * XPath expressions.
 */
@Serializable
@XmlSerialName("decimal-format", QT3TNS)
class Qt3DecimalFormat(
    val name: SerializableQName? = null,
    @SerialName("decimal-separator")
    val decimalSeparator: Char? = null,
    @SerialName("grouping-separator")
    val groupingSeparator: Char? = null,
    @SerialName("zero-digit")
    val zeroDigit: Char? = null,
    val digit: Char? = null,
    @SerialName("minus-sign")
    val minusSign: Char? = null,
    val percent: Char? = null,
    @SerialName("per-mille")
    val perMille: Char? = null,
    @SerialName("pattern-separator")
    val patternSeparator: Char? = null,
    @SerialName("exponent-separator")
    val exponentSeparator: Char? = null,
    val infinity: String? = null,
    val NaN: String? = null,
): Qt3Environment.Element
