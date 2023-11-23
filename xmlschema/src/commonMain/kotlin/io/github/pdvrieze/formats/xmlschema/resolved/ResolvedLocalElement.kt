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
import io.github.pdvrieze.formats.xmlschema.datatypes.primitiveInstances.VNonNegativeInteger
import io.github.pdvrieze.formats.xmlschema.datatypes.serialization.XSLocalElement
import io.github.pdvrieze.formats.xmlschema.datatypes.serialization.XSLocalType
import io.github.pdvrieze.formats.xmlschema.impl.invariant
import io.github.pdvrieze.formats.xmlschema.impl.invariantNotNull
import io.github.pdvrieze.formats.xmlschema.types.VAllNNI
import io.github.pdvrieze.formats.xmlschema.types.VDerivationControl
import io.github.pdvrieze.formats.xmlschema.types.VFormChoice
import nl.adaptivity.xmlutil.QName

class ResolvedLocalElement private constructor(
    parent: VElementScope.Member,
    elemPart: SchemaElement<XSLocalElement>,
    schema: ResolvedSchemaLike,
    override val mdlMinOccurs: VNonNegativeInteger,
    override val mdlMaxOccurs: VAllNNI,
) : ResolvedElement(elemPart.elem, schema),
    IResolvedElementUse {

    init {
        val rawPart = elemPart.elem
        invariant(rawPart.ref == null)
        requireNotNull(rawPart.name) { "3.3.3(2.1) - A local element declaration must have exactly one of name or ref specified"}

        require(mdlMinOccurs<=mdlMaxOccurs) { "Invalid bounds: ! (${mdlMinOccurs}<=$mdlMaxOccurs)" }

        if (rawPart.targetNamespace != null && schema.targetNamespace != rawPart.targetNamespace) {
            error("XXX. Canary. Remove once verified")
            check(parent is ResolvedComplexType) { "3.3.3(4.3.1) - Attribute with non-matchin namespace must have complex type ancestor"}
            check(parent.mdlContentType is ResolvedComplexType.ElementContentType)
            check(parent.mdlDerivationMethod == VDerivationControl.RESTRICTION)
            check(parent.mdlBaseTypeDefinition != AnyType) { "3.3.3(4.3.2) - Restriction isn't anytype" }
        }

    }

    override val model: Model by lazy { Model(elemPart, schema, this) }

    override val mdlQName: QName = invariantNotNull(elemPart.elem.name).toQname(
        elemPart.elem.targetNamespace?.also { require(elemPart.elem.form==null) { "3.3.3(4.2) - If targetNamespace is present form must not be" } }
            ?: when (elemPart.elem.form ?: schema.elementFormDefault) {
                VFormChoice.QUALIFIED -> schema.targetNamespace
                else -> null
            }
    )

    override val mdlSubstitutionGroupExclusions: Set<VDerivationControl.T_BlockSetValues> =
        schema.finalDefault.filterIsInstanceTo(HashSet())

    override val mdlScope: VElementScope.Local = VElementScope.Local(parent)

    override val mdlTerm: ResolvedLocalElement get() = this

    override val mdlAbstract: Boolean get() = false

    internal constructor(
        parent: VElementScope.Member,
        elemPart: SchemaElement<XSLocalElement>,
        schema: ResolvedSchemaLike,
    ) : this(
        parent,
        elemPart,
        schema,
        elemPart.elem.minOccurs ?: VNonNegativeInteger.ONE,
        elemPart.elem.maxOccurs ?: VAllNNI.ONE,
    )

    override fun toString(): String {
        return buildString {
            append("ResolvedLocalElement(")
            append("name=$mdlQName, ")
            if (mdlMinOccurs != VNonNegativeInteger.ONE) append("minOccurs=$mdlMinOccurs, ")
            if (mdlMaxOccurs != VAllNNI.ONE) append("maxOccurs=$mdlMaxOccurs, ")
            append("type=${this@ResolvedLocalElement.mdlTypeDefinition}")
            append(")")
        }
    }

    class Model internal constructor(
        elemPart: SchemaElement<XSLocalElement>,
        schema: ResolvedSchemaLike,
        context: ResolvedLocalElement
    ) : ResolvedElement.Model(elemPart.elem, schema, context) {

        val mdlTerm: ResolvedLocalElement = context

        override val mdlTypeTable: ITypeTable?
            get() = null

        override val mdlTypeDefinition: ResolvedType =
            elemPart.wrap { localType }
                .let { if (it.elem != null) ResolvedLocalType(it.cast<XSLocalType>(), schema, context) else null }
                ?: elemPart.elem.type?.let { schema.type(it) }
                ?: AnyType
    }

    interface Parent

}
