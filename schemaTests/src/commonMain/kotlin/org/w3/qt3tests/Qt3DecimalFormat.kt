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

import io.github.pdvrieze.xml.schematypes.values.UnicodeChar
import io.github.pdvrieze.xml.schematypes.values.formatters.DecimalFormat
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import nl.adaptivity.xmlutil.QName
import nl.adaptivity.xmlutil.SerializableQName
import nl.adaptivity.xmlutil.serialization.XmlSerialName

@Serializable
@XmlSerialName("decimal-format", QT3TNS)
class Qt3DecimalFormat(
    val name: SerializableQName? = null,
    @SerialName("decimal-separator")
    val decimalSeparator: UnicodeChar? = null,
    @SerialName("grouping-separator")
    val groupingSeparator: UnicodeChar? = null,
    @SerialName("zero-digit")
    val zeroDigit: UnicodeChar? = null,
    val digit: UnicodeChar? = null,
    @SerialName("minus-sign")
    val minusSign: UnicodeChar? = null,
    val percent: UnicodeChar? = null,
    @SerialName("per-mille")
    val perMille: UnicodeChar? = null,
    @SerialName("pattern-separator")
    val patternSeparator: UnicodeChar? = null,
    @SerialName("exponent-separator")
    val exponentSeparator: UnicodeChar? = null,
    val infinity: String? = null,
    val NaN: String? = null,
) : Qt3Environment.Element {

    private fun setValues(b: DecimalFormat.Builder) {
        name?.let { b.name = it }
        decimalSeparator?.let { b.decimalSeparator = it }
        exponentSeparator?.let { b.exponentSeparator = it }
        groupingSeparator?.let { b.groupingSeparator = it }
        percent?.let { b.percent = it }
        perMille?.let { b.perMille = it }
        zeroDigit?.let { b.zeroDigit = it }
        digit?.let { b.digit = it }
        minusSign?.let { b.minusSign = it }
        patternSeparator?.let { b.patternSeparator = it }
        infinity?.let { b.infinity = it }
        NaN?.let { b.NaN = it }

    }

    fun toDecimalFormat(overriddenName: QName): DecimalFormat.Named {
        return DecimalFormat.Builder().also { b ->
            setValues(b)
            b.name = overriddenName
        }.build() as DecimalFormat.Named
    }

    fun toDecimalFormat(): DecimalFormat {
        return DecimalFormat.Builder().also { b ->
            setValues(b)
        }.build()
    }
}
