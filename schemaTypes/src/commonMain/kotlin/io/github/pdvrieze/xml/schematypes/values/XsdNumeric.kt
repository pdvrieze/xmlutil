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

package io.github.pdvrieze.xml.schematypes.values

import io.github.pdvrieze.xml.schematypes.Collation
import io.github.pdvrieze.xml.schematypes.types.PrimitiveType
import nl.adaptivity.xmlutil.ExperimentalXmlUtilApi

@ExperimentalXmlUtilApi
sealed interface XsdNumeric<out T : XsdNumeric<T>> : XsdPrimitive {
    override val schemaType: PrimitiveType<XsdPrimitive>

    fun toDouble(): Double

    fun abs(): T

    operator fun unaryMinus(): T

    fun ceiling(): T
    fun floor(): T
    fun round(): T
    fun round(precision: XsdInteger): T = round(precision.toInt())
    fun round(precision: Int): T
    fun roundToHalfEven(): T
    fun roundToHalfEven(precision: XsdInteger): T = roundToHalfEven(precision.toInt())
    fun roundToHalfEven(precision: Int): T

    operator fun compareTo(other: XsdNumeric<*>): Int

    override fun compareTo(other: XsdPrimitive, collation: Collation): Int {
        return when (other) {
            is XsdNumeric<*> -> compareTo(other)
            else -> throw IllegalArgumentException("Cannot compare decimal to ${other.schemaType}")
        }
    }

}
