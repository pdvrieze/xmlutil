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
import io.github.pdvrieze.formats.xmlschema.datatypes.serialization.XSLocalSimpleType
import io.github.pdvrieze.formats.xmlschema.datatypes.serialization.XSSimpleRestriction
import io.github.pdvrieze.formats.xmlschema.datatypes.serialization.XSSimpleUnion
import io.github.pdvrieze.formats.xmlschema.datatypes.serialization.facets.XSEnumeration
import io.github.pdvrieze.formats.xmlschema.datatypes.serialization.facets.XSPattern
import io.github.pdvrieze.formats.xmlschema.resolved.checking.CheckHelper
import io.github.pdvrieze.formats.xmlschema.resolved.facets.FacetList
import io.github.pdvrieze.formats.xmlschema.types.VDerivationControl
import nl.adaptivity.xmlutil.QName

class ResolvedUnionDerivation(
    rawPart: XSSimpleUnion,
    schema: ResolvedSchemaLike,
    context: ResolvedSimpleType,
) : ResolvedSimpleType.Derivation() {

    private val _model: Model by lazy {
        Model(rawPart, schema, context)
    }
    override val model: ResolvedAnnotated.IModel get() = _model

    override val baseType: ResolvedSimpleType get() = AnySimpleType

    val memberTypes: List<ResolvedSimpleType> get() = _model.memberTypes.map { (it as? UnionMemberWrapper)?.base ?: it }

    override fun checkDerivation(checkHelper: CheckHelper) {
        require(_model.memberTypes.isNotEmpty()) { "Union without elements" }
        for (m in _model.memberTypes) {
            // TODO must ignore member facets that are not "pattern" or "enumeration"
            checkHelper.checkType(m)

            check(VDerivationControl.UNION !in m.mdlFinal) {
                "$m is final for union, and can not be put in a union"
            }
        }
    }

    private class Model(
        rawPart: XSSimpleUnion,
        schema: ResolvedSchemaLike,
        context: ResolvedSimpleType
    ) : ResolvedAnnotated.Model(rawPart) {
        val memberTypes: List<ResolvedSimpleType>

        init {
            val simpleTypes = rawPart.simpleTypes.map { ResolvedLocalSimpleType(it.filterUnionFacets(), schema, context) }

            val mt: List<ResolvedGlobalSimpleType>? = rawPart.memberTypes?.map {
                schema.simpleType(it).unionMemberWrapper()
            }

            memberTypes = when {
                mt.isNullOrEmpty() -> simpleTypes
                rawPart.simpleTypes.isEmpty() -> mt
                else -> mt + simpleTypes
            }

            // Check 3.16.6.2
            for (mt in memberTypes) {
                require(mt!is ResolvedBuiltinType || !mt.isSpecial) { "3.16.6.2(3.1) - Unions cannot derive from special types" }
                require(VDerivationControl.UNION !in mt.mdlFinal) { "3.16.6.2(3.2.1.1) - Member type is final for union"}
            }
        }
    }
}

private fun ResolvedGlobalSimpleType.unionMemberWrapper(): ResolvedGlobalSimpleType {
    val f = mdlFacets
    if (f.assertions.isEmpty() && f.minConstraint == null && f.maxConstraint == null &&
        f.explicitTimezone == null && f.fractionDigits == null && f.minLength == null &&
        f.maxLength == null && f.totalDigits == null && f.whiteSpace == null &&
        f.otherFacets.isEmpty()) return this

    return UnionMemberWrapper(this)
}

private class UnionMemberWrapper(val base: ResolvedGlobalSimpleType) : ResolvedGlobalSimpleType {
    override val mdlFacets: FacetList = FacetList(enumeration = base.mdlFacets.enumeration, patterns = base.mdlFacets.patterns)

    override val mdlQName: QName get() = base.mdlQName

    override val model: ResolvedSimpleType.Model get() = base.model
    override val simpleDerivation: ResolvedSimpleType.Derivation get() = base.simpleDerivation
    override val mdlFinal: Set<VDerivationControl.Type> get() = base.mdlFinal
}

/**
 * Unions ignore facets other than pattern/enumeration
 */
private fun XSLocalSimpleType.filterUnionFacets(): XSLocalSimpleType {
    val newDerivation = when (simpleDerivation) {
        is XSSimpleRestriction -> with(simpleDerivation) {
            val filteredFacets = facets.filter { it is XSPattern || it is XSEnumeration }
            XSSimpleRestriction(
                base, simpleType, filteredFacets, otherContents, id, annotation, otherAttrs
            )
        }
        else -> return this
    }
    return XSLocalSimpleType(newDerivation, id, annotation, otherAttrs)
}
