/*
 * Copyright (c) 2024.
 *
 * This file is part of xmlutil.
 *
 * This file is licenced to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You should have received a copy of the license with the source distribution.
 * Alternatively, you may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package io.github.pdvrieze.formats.xmlschema.resolved

import io.github.pdvrieze.formats.xmlschema.datatypes.serialization.XSAlternative
import io.github.pdvrieze.formats.xmlschema.model.TypeAlternativeModel
import io.github.pdvrieze.formats.xmlschema.resolved.checking.CheckHelper
import io.github.pdvrieze.formats.xpath.XPathExpression

class ResolvedTypeTable(val alternatives: List<ResolvedAlternative>, val default: ResolvedType) {

    internal constructor(
        alternatives: List<SchemaElement<XSAlternative>>,
        schema: ResolvedSchemaLike,
        parentType: ResolvedType,
        scope: ResolvedElement
    ) : this(
        alternatives = skipDefault(alternatives).map { ResolvedAlternative(it, schema, scope) },
        default = (alternatives.lastOrNull()?.takeIf { it.elem.test == null }
            ?.let { ResolvedAlternative(it, schema, scope).type }) ?: parentType
    )

    fun isEquivalent(other: ResolvedTypeTable?): Boolean {
        if (other == null) return false

        if (alternatives.size != other.alternatives.size) return false

        return true
    }

    fun check(checkHelper: CheckHelper) {
        for(a in alternatives) {
            a.check(checkHelper)
        }
    }

    companion object {
        private fun skipDefault(alternatives: List<SchemaElement<XSAlternative>>): List<SchemaElement<XSAlternative>> {
            return when ((alternatives.lastOrNull() ?: return emptyList()).elem.test) {
                null -> alternatives.take(alternatives.size - 1)
                else -> alternatives
            }
        }
    }
}

class ResolvedAlternative internal constructor(alternative: SchemaElement<XSAlternative>, schema: ResolvedSchemaLike, scope: ResolvedElement): ResolvedAnnotated {
    override val model: Model by lazy { Model(alternative, schema, scope) }

    val test: XPathExpression?

    init {
        // TODO handle default xpath namespace attribute (here or on schema)
        test = alternative.elem.test
    }

    val type: ResolvedType get() = model.type

    fun check(checkHelper: CheckHelper) {
        if(test != null) {
             check(ResolvedIdentityConstraint.SELECTORPATTERN.matches(test.xmlString)) { "Invalid xpath expression for selectors: '${test.xmlString}'" }
        }
        if (type is ResolvedLocalType) {
            type.checkType(checkHelper)
        }
    }

    class Model internal constructor(rawPart: SchemaElement<XSAlternative>, schema: ResolvedSchemaLike, scope: ResolvedElement) : ResolvedAnnotated.Model(rawPart.elem) {
        val type: ResolvedType

        init {
            val t = rawPart.wrapNullable { type }
            val lt = rawPart.wrapNullable { localType }

            type = when {
                t != null -> schema.type(t.elem)
                lt != null -> ResolvedLocalType(lt, schema, scope)
                else -> throw IllegalArgumentException("Alternative must specify a type, either as attribute or as member")
            }
        }
    }
}


typealias ITypeTable = ResolvedTypeTable

fun ITypeTable?.isEquivalent(other: ITypeTable?): Boolean = when {
    this == null -> other == null
    other == null -> false
    else -> isEquivalent(other)
}
