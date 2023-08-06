/*
 * Copyright (c) 2023.
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

import io.github.pdvrieze.formats.xmlschema.datatypes.AnySimpleType
import io.github.pdvrieze.formats.xmlschema.datatypes.impl.SingleLinkedList
import io.github.pdvrieze.formats.xmlschema.datatypes.serialization.XSSimpleUnion
import io.github.pdvrieze.formats.xmlschema.resolved.checking.CheckHelper
import io.github.pdvrieze.formats.xmlschema.types.VDerivationControl

class ResolvedUnionDerivation(
    override val rawPart: XSSimpleUnion,
    schema: ResolvedSchemaLike,
    context: ResolvedSimpleType,
    inheritedTypes: SingleLinkedList<ResolvedType>,
) : ResolvedSimpleType.Derivation(rawPart, schema, inheritedTypes) {

    private val _model: Model by lazy {
        Model(rawPart, schema, context, inheritedTypes)
    }
    override val model: ResolvedAnnotated.IModel get() = _model

    override val baseType: ResolvedSimpleType get() = AnySimpleType

    val memberTypes: List<ResolvedSimpleType> get() = _model.memberTypes

    override fun checkDerivation(checkHelper: CheckHelper, inheritedTypes: SingleLinkedList<ResolvedType>) {
        require(memberTypes.isNotEmpty()) { "Union without elements" }
        for (m in memberTypes) {
            checkHelper.checkType(m, inheritedTypes)

            check(VDerivationControl.UNION !in m.mdlFinal) {
                "$m is final for union, and can not be put in a union"
            }
        }


    }

    fun transitiveMembership(collector: MutableSet<ResolvedSimpleType> = mutableSetOf()): Set<ResolvedSimpleType> {
        for (m in memberTypes) {
            val d = m.simpleDerivation
            if (d is ResolvedUnionDerivation) {
                d.transitiveMembership(collector)
            } else {
                collector.add(m)
            }
        }
        return collector
    }

    private class Model(
        rawPart: XSSimpleUnion,
        schema: ResolvedSchemaLike,
        context: ResolvedSimpleType,
        inheritedTypes: SingleLinkedList<ResolvedType>
    ) : ResolvedAnnotated.Model(rawPart) {
        val memberTypes: List<ResolvedSimpleType>

        init {
            val simpleTypes = rawPart.simpleTypes.map { ResolvedLocalSimpleType(it, schema, context, inheritedTypes) }

            val mt = rawPart.memberTypes?.map {
                schema.simpleType(it, inheritedTypes)
            }

            memberTypes = when {
                mt.isNullOrEmpty() -> simpleTypes
                rawPart.simpleTypes.isEmpty() -> mt
                else -> mt + simpleTypes
            }
        }
    }
}
