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

package io.github.pdvrieze.formats.xpath.data

import io.github.pdvrieze.formats.xpath.impl.ExprEvalContext
import io.github.pdvrieze.formats.xpath.impl.XPathInternal
import io.github.pdvrieze.xml.schematypes.types.AnyType

@OptIn(XPathInternal::class)
sealed class XdmType {

    context(ctx: ExprEvalContext)
    abstract fun isSubtypeOf(other: XdmType): Boolean

    context(ctx: ExprEvalContext)
    fun isSubtypeOf(expectedType: AnyType): Boolean =
        isSubtypeOf(XdmSchemaType(expectedType))


    object EmptySequence : XdmType() {
        context(ctx: ExprEvalContext)
        override fun isSubtypeOf(other: XdmType): Boolean {
            when {
                other == EmptySequence -> return true
                other is XdmSequenceType -> return other.cardinality.allowsEmpty
                else -> return false
            }
        }

        override fun toString(): String = "EmptySequence()"


    }
}
