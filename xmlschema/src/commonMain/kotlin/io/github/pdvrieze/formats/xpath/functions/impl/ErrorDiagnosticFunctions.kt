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

package io.github.pdvrieze.formats.xpath.functions.impl

import io.github.pdvrieze.formats.xpath.eval.ErrorCodes
import io.github.pdvrieze.formats.xpath.eval.UserEvaluationException
import io.github.pdvrieze.formats.xpath.eval.data.XdmString
import io.github.pdvrieze.formats.xpath.eval.typeTest.XdmSequenceTypeTest
import io.github.pdvrieze.formats.xpath.functions.BuiltinFunctionImpl
import io.github.pdvrieze.formats.xpath.functions.atomicArgN
import io.github.pdvrieze.formats.xpath.functions.atomicArgOrEmpty
import io.github.pdvrieze.formats.xpath.impl.ExprEvalContext
import io.github.pdvrieze.formats.xpath.impl.XPathInternal
import io.github.pdvrieze.xml.schematypes.values.XsdQName
import io.github.pdvrieze.xml.schematypes.values.XsdString

@XPathInternal
object ErrorDiagnosticFunctions : AbstractFunctionObject() {

    val fnError: BuiltinFunctionImpl<XdmString> = BuiltinFunctionImpl("error", listOf(
        functionType(XdmSequenceTypeTest.NONE),
        functionType(XdmSequenceTypeTest.NONE, QNAME.opt),
        functionType(XdmSequenceTypeTest.NONE, QNAME.opt, STRING),
        functionType(XdmSequenceTypeTest.NONE, QNAME.opt, STRING, ITEM.any),
    )) { args ->
        val declaredCode = if (args.size > 0) args.atomicArgOrEmpty<XsdQName>(0) else null

        val errorCode: ErrorCodes?
        val code = when (declaredCode) {
            null -> {
                errorCode = ErrorCodes.FOER0000
                errorCode.qName
            }
            else -> {
                errorCode = ErrorCodes.lookup(declaredCode)
                declaredCode
            }
        }

        val description = when {
            args.size > 1 -> args.atomicArgN<XsdString>(1).xmlString
            else -> errorCode?.message ?: "Unknown error"
        }
        val context = args.getOrNull(2)
        val expr = contextOf<ExprEvalContext>().expr
        when (errorCode) {
            null -> throw UserEvaluationException(code, expr, description, context)
            else -> throw UserEvaluationException(errorCode, expr, description, context)
        }
    }

    val fnTrace = BuiltinFunctionImpl("trace", listOf(
        functionType(ITEM.any, ITEM.any),
        functionType(ITEM.any, ITEM.any, STRING),
    )) { args ->
        val value = args[0]
        val label = if (args.size==2) args.atomicArgN<XsdString>(1).xmlString else null

        val text = Accessors.fnString(value).value.xmlString

        contextOf<ExprEvalContext>().trace(label, text)

        value
    }

}
