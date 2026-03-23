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

package io.github.pdvrieze.formats.xpath.impl

import io.github.pdvrieze.formats.xpath.data.XdmValue
import io.github.pdvrieze.xml.schematypes.types.AnyType
import io.github.pdvrieze.xml.schematypes.types.builtinType
import nl.adaptivity.xmlutil.QName
import nl.adaptivity.xmlutil.dom2.Document
import nl.adaptivity.xmlutil.localPart
import nl.adaptivity.xmlutil.namespaceURI
import nl.adaptivity.xmlutil.xmlStreaming
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract

@XPathInternal
open class EvalContext(val contextItem: XdmValue) {
    fun resolveType(name: QName): AnyType? {
        return builtinType(name.localPart, name.namespaceURI)
    }

    val outputDocument: Document by lazy {
        xmlStreaming.genericDomImplementation.createDocument(null, null, null)
    }

    @OptIn(ExperimentalContracts::class)
    inline fun <R> withExprContext(expr: Expr, block: context(ExprEvalContext) ()-> R): R {
        contract {
            callsInPlace(block, InvocationKind.EXACTLY_ONCE)
        }
        return block(ExprEvalContext(contextItem, expr))
    }
}

@XPathInternal
class ExprEvalContext(contextItem: XdmValue, val expr: Expr) : EvalContext(contextItem)
