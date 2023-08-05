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

import io.github.pdvrieze.formats.xmlschema.datatypes.AnyType
import io.github.pdvrieze.formats.xmlschema.datatypes.primitiveInstances.VAnyURI
import io.github.pdvrieze.formats.xmlschema.datatypes.primitiveInstances.VNonNegativeInteger
import io.github.pdvrieze.formats.xmlschema.datatypes.serialization.XSLocalElement
import io.github.pdvrieze.formats.xmlschema.impl.invariant
import io.github.pdvrieze.formats.xmlschema.impl.invariantNotNull
import io.github.pdvrieze.formats.xmlschema.resolved.checking.CheckHelper
import io.github.pdvrieze.formats.xmlschema.types.T_BlockSetValues
import io.github.pdvrieze.formats.xmlschema.types.VAllNNI
import io.github.pdvrieze.formats.xmlschema.types.VDerivationControl
import io.github.pdvrieze.formats.xmlschema.types.VFormChoice
import nl.adaptivity.xmlutil.QName

class ResolvedLocalElement private constructor(
    parent: VElementScope.Member,
    override val rawPart: XSLocalElement,
    schema: ResolvedSchemaLike,
    override val mdlMinOccurs: VNonNegativeInteger,
    override val mdlMaxOccurs: VAllNNI,
) : ResolvedElement(rawPart, schema),
    IResolvedElementUse {

    init {
        invariant(rawPart.ref == null)
        requireNotNull(rawPart.name) { "3.3.3(2.1) - A local element declaration must have exactly one of name or ref specified"}

        require(mdlMinOccurs <= mdlMaxOccurs) { "XXX minOccurs must be smaller or equal to maxOccurs" }

        if (rawPart.targetNamespace != null && schema.targetNamespace != rawPart.targetNamespace) {
            error("XXX. Canary. Remove once verified")
            check(parent is ResolvedComplexType) { "3.3.3(4.3.1) - Attribute with non-matchin namespace must have complex type ancestor"}
            check(parent.mdlContentType is ResolvedComplexType.ElementContentType)
            check(parent.mdlDerivationMethod == VDerivationControl.RESTRICTION)
            check(parent.mdlBaseTypeDefinition != AnyType) { "3.3.3(4.3.2) - Restriction isn't anytype" }
        }

    }

    override val model: Model by lazy { Model(rawPart, schema, this) }

    override val mdlQName: QName = invariantNotNull(rawPart.name).toQname(
        rawPart.targetNamespace?.also { require(rawPart.form==null) { "3.3.3(4.2) - If targetNamespace is present form must not be" } }
            ?: when (rawPart.form ?: schema.elementFormDefault) {
                VFormChoice.QUALIFIED -> schema.targetNamespace
                else -> null
            }
    )

    override val mdlSubstitutionGroupExclusions: Set<T_BlockSetValues> =
        schema.finalDefault.filterIsInstanceTo(HashSet())

    override val mdlScope: VElementScope.Local = VElementScope.Local(parent)

    override val mdlTerm: ResolvedLocalElement get() = this

    override val mdlAbstract: Boolean get() = false

    constructor(
        parent: VElementScope.Member,
        rawPart: XSLocalElement,
        schema: ResolvedSchemaLike,
    ) : this(
        parent,
        rawPart,
        schema,
        rawPart.minOccurs ?: VNonNegativeInteger.ONE,
        rawPart.maxOccurs ?: VAllNNI.ONE,
    )

    override fun normalizeTerm(
        minMultiplier: VNonNegativeInteger,
        maxMultiplier: VAllNNI
    ): ResolvedLocalElement = when {
        minMultiplier != VNonNegativeInteger.ONE || maxMultiplier != VAllNNI.ONE -> {
            ResolvedLocalElement(
                mdlScope.parent,
                rawPart,
                schema,
                mdlMinOccurs.times(minMultiplier),
                mdlMaxOccurs.times(maxMultiplier),
            )
        }

        else -> this
    }

    override fun toString(): String {
        return buildString {
            append("ResolvedLocalElement(")
            append("mdlQName=$mdlQName, ")
            if (mdlMinOccurs != VNonNegativeInteger.ONE) append("minOccurs=$mdlMinOccurs, ")
            if (mdlMaxOccurs != VAllNNI.ONE) append("maxOccurs=$mdlMaxOccurs, ")
            append("type=${this@ResolvedLocalElement.mdlTypeDefinition}")
            append(")")
        }
    }

    protected inner class Model(
        rawPart: XSLocalElement,
        schema: ResolvedSchemaLike,
        context: ResolvedLocalElement
    ) : ResolvedElement.Model(rawPart, schema, context) {

        val mdlTargetNamespace: VAnyURI? get() = rawPart.targetNamespace ?: schema.targetNamespace

        val mdlTerm: ResolvedLocalElement get() = this@ResolvedLocalElement


        val mdlMinOccurs: VNonNegativeInteger = rawPart.minOccurs ?: VNonNegativeInteger.ONE

        val mdlMaxOccurs: VAllNNI = rawPart.maxOccurs ?: VAllNNI.ONE

        override val mdlTypeTable: ITypeTable
            get() = TODO("not implemented")

        override val mdlTypeDefinition: ResolvedType =
            rawPart.localType?.let { ResolvedLocalType(it, schema, context) }
                ?: rawPart.type?.let { schema.type(it) }
                ?: AnyType
    }

    interface Parent

}
