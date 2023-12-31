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
import io.github.pdvrieze.formats.xmlschema.resolved.checking.CheckHelper
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

    override val mdlQName: QName

    init {
        mdlQName = invariantNotNull(elemPart.elem.name).toQname(
            elemPart.elem.targetNamespace?.also { require(elemPart.elem.form==null) { "3.3.3(4.2) - If targetNamespace is present form must not be" } }
                ?: when (elemPart.elem.form ?: elemPart.elementFormDefault) {
                    VFormChoice.QUALIFIED -> schema.targetNamespace
                    else -> null
                }
        )

    }

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

    override fun isSiblingName(name: QName): Boolean {
        return super<ResolvedElement>.isSiblingName(name)
    }

    override fun flatten(checkHelper: CheckHelper): FlattenedParticle {
        return super<ResolvedElement>.flatten(checkHelper)
    }

    override fun toString(): String {
        return buildString {
            append("localElement(")
            append(mdlQName)
            if (mdlMinOccurs != VNonNegativeInteger.ONE || mdlMaxOccurs != VAllNNI.ONE) append(range)
            append(", type=${this@ResolvedLocalElement.model.mdlTypeDefinition.getOrDefault("<missing type>")}")
            append(")")
        }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || this::class != other::class) return false
        if (!super.equals(other)) return false

        other as ResolvedLocalElement

        if (mdlMinOccurs != other.mdlMinOccurs) return false
        if (mdlMaxOccurs != other.mdlMaxOccurs) return false
        if (mdlQName != other.mdlQName) return false
        if (mdlSubstitutionGroupExclusions != other.mdlSubstitutionGroupExclusions) return false
        if (mdlScope != other.mdlScope) return false

        return true
    }

    override fun hashCode(): Int {
        var result = super.hashCode()
        result = 31 * result + mdlMinOccurs.hashCode()
        result = 31 * result + mdlMaxOccurs.hashCode()
        result = 31 * result + mdlQName.hashCode()
        result = 31 * result + mdlSubstitutionGroupExclusions.hashCode()
        result = 31 * result + mdlScope.hashCode()
        return result
    }

    class Model internal constructor(
        elemPart: SchemaElement<XSLocalElement>,
        schema: ResolvedSchemaLike,
        context: ResolvedLocalElement
    ) : ResolvedElement.Model(elemPart.elem, schema, context) {

        val mdlTerm: ResolvedLocalElement = context

        override val mdlTypeTable: ITypeTable?
            get() = null

        override val mdlTypeDefinition: Result<ResolvedType>

        init {
            val localType = elemPart.wrap { localType }
            mdlTypeDefinition = when {
                localType.elem != null -> Result.success(ResolvedLocalType(localType.cast<XSLocalType>(), schema, context))
                elemPart.elem.type != null -> runCatching { schema.type(elemPart.elem.type) }
                else -> Result.success(AnyType)
            }
        }

        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other == null || this::class != other::class) return false
            if (!super.equals(other)) return false

            other as Model

            if (mdlTerm != other.mdlTerm) return false
            if (mdlTypeDefinition != other.mdlTypeDefinition) return false

            return true
        }

        override fun hashCode(): Int {
            var result = super.hashCode()
            result = 31 * result + mdlTerm.hashCode()
            result = 31 * result + mdlTypeDefinition.hashCode()
            return result
        }


    }

    interface Parent

}
