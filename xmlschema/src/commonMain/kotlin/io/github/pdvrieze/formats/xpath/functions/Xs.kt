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

package io.github.pdvrieze.formats.xpath.functions

import io.github.pdvrieze.formats.xpath.data.*
import io.github.pdvrieze.formats.xpath.functions.impl.AbstractFunctionObject
import io.github.pdvrieze.formats.xpath.impl.ExprEvalContext
import io.github.pdvrieze.formats.xpath.impl.XPathInternal
import io.github.pdvrieze.xml.schematypes.types.AnyAtomicType
import io.github.pdvrieze.xml.schematypes.values.XsdString
import nl.adaptivity.xmlutil.QName

@XPathInternal
object Xs: AbstractFunctionObject() {

    context(ctx: ExprEvalContext)
    fun createFromSchemaType(name: QName): BuiltinFunction<*> {
        val type = ctx.resolveType(name) as AnyAtomicType<*>
        return BuiltinFunctionImpl(name, listOf(XdmFunctionType(XdmType.STRING, XdmSchemaType(type)))) { args ->
            val arg = toSingleAtomic<XsdString>(args) ?: throw EvaluationException(ErrorCodes.XPTY0004_TYPE_ERROR)
            XdmAtomic(type.fromString(arg.xmlString))
        }
    }

}
