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

@file:OptIn(XPathInternal::class)

package io.github.pdvrieze.formats.xpath.impl

import io.github.pdvrieze.formats.xpath.eval.ErrorCodes
import io.github.pdvrieze.formats.xpath.eval.EvaluationException
import io.github.pdvrieze.xml.schematypes.types.AnySimpleListType
import io.github.pdvrieze.xml.schematypes.types.AnySimpleType
import nl.adaptivity.xmlutil.QName


private fun deepItemType(listType: AnySimpleListType<*,*>): AnySimpleType<*> {
    when (val i = listType.itemType) {
        is AnySimpleListType<*,*> -> return deepItemType(i)
        else -> return i
    }
}

context(ctx: ExprEvalContext)
internal fun castItemType(typeName: QName) : Pair<Boolean, AnySimpleType<*>> {

    var isList = false

    val schemaType = when (val s = ctx.resolveTypeOrNull(typeName)) {
        null -> throw EvaluationException(ErrorCodes.XQST0052_INVALID_TYPE_IN_CAST, "The type $typeName is not known")
        is AnySimpleListType<*, *> -> {
            isList = true
            deepItemType(s)
        }
        !is AnySimpleType<*> -> throw EvaluationException(ErrorCodes.XPST0080_INVALID_TARGET_TYPE, "The type $typeName is not simple")
        else -> s
    }
    return isList to schemaType
}
