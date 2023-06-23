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
import io.github.pdvrieze.formats.xmlschema.datatypes.AnyType
import io.github.pdvrieze.formats.xmlschema.datatypes.impl.SingleLinkedList
import io.github.pdvrieze.formats.xmlschema.datatypes.primitiveInstances.VAnyURI
import io.github.pdvrieze.formats.xmlschema.datatypes.primitiveInstances.VID
import io.github.pdvrieze.formats.xmlschema.datatypes.primitiveInstances.VNCName
import io.github.pdvrieze.formats.xmlschema.datatypes.primitiveTypes.PrimitiveDatatype
import io.github.pdvrieze.formats.xmlschema.datatypes.serialization.*
import io.github.pdvrieze.formats.xmlschema.model.*
import io.github.pdvrieze.formats.xmlschema.types.*
import nl.adaptivity.xmlutil.QName

class ResolvedGlobalComplexType(
    override val rawPart: XSGlobalComplexType,
    schema: ResolvedSchemaLike
) : ResolvedGlobalType, ResolvedComplexType(schema), T_GlobalComplexType_Base, ComplexTypeModel.Global {
    override val name: VNCName
        get() = rawPart.name

    override val annotation: XSAnnotation?
        get() = rawPart.annotation

    override val id: VID?
        get() = rawPart.id

    override val otherAttrs: Map<QName, String>
        get() = rawPart.otherAttrs

    override val targetNamespace: VAnyURI?
        get() = schema.targetNamespace

    override val mixed: Boolean?
        get() = rawPart.mixed

    override val defaultAttributesApply: Boolean?
        get() = rawPart.defaultAttributesApply

    override val content: ResolvedComplexTypeContent
        by lazy {
            when (val c = rawPart.content) {
                is XSComplexContent -> ResolvedComplexContent(this, c, schema)
                is IXSComplexTypeShorthand -> ResolvedComplexShorthandContent(this, c, schema)
                is XSSimpleContent -> ResolvedSimpleContent(this, c, schema)
                else -> error("unsupported content")
            }
        }

    override val abstract: Boolean get() = model.mdlAbstract

    override val final: T_DerivationSet get() = model.mdlFinal

    override val block: T_DerivationSet get() = model.mdlProhibitedSubstitutions

    override fun check(seenTypes: SingleLinkedList<QName>, inheritedTypes: SingleLinkedList<QName>) {
        content.check(seenTypes + qName, inheritedTypes + qName)
    }

    override val model: Model by lazy {
        when (val r = rawPart) {
            is XSGlobalComplexTypeComplex -> ComplexModelImpl(r, schema)
            is XSGlobalComplexTypeShorthand -> ShorthandModelImpl(r, schema)
            is XSGlobalComplexTypeSimple -> SimpleModelImpl(r, schema, this)
        }

    }

    override val mdlName: VNCName get() = model.mdlName

    override val mdlTargetNamespace: VAnyURI? get() = model.mdlTargetNamespace

    interface Model: ResolvedComplexType.Model, ComplexTypeModel.Global

    private abstract class ModelBase(rawPart: XSGlobalComplexType, schema: ResolvedSchemaLike) :
        ResolvedComplexType.ModelImpl(rawPart, schema), Model {
        final override val mdlName: VNCName = rawPart.name
        final override val mdlTargetNamespace: VAnyURI? = rawPart.targetNamespace?:schema.targetNamespace

        final override val mdlAbstract: Boolean = rawPart.abstract ?: false

        final override val mdlProhibitedSubstitutions: T_DerivationSet = rawPart.block ?: schema.blockDefault.toDerivationSet()

        final override val mdlFinal: T_DerivationSet = rawPart.final ?: schema.finalDefault.toDerivationSet()

        final override val mdlAttributeUses: Set<AttributeModel.Use> get() = TODO()

        final override val mdlAttributeWildcard: WildcardModel
            get() = TODO("not implemented")

        init {
            val pseudoAttributesGroup = (schema as? ResolvedSchema)?.defaultAttributes?.takeIf { rawPart.defaultAttributesApply!=false }?.let { defName ->
                ResolvedAttributeGroupRef(XSAttributeGroupRef(ref=defName), schema)
            }


        }
    }

    private abstract class ComplexModelbase(rawPart: XSGlobalComplexType, schema: ResolvedSchemaLike) : ModelBase(rawPart, schema) {
        override val mdlContentType: ComplexTypeModel.ContentType
            get() = TODO("not implemented")
    }

    private class SimpleModelImpl(rawPart: XSGlobalComplexTypeSimple, schema: ResolvedSchemaLike, context: ResolvedComplexType) :
        ModelBase(rawPart, schema), ComplexTypeModel.ContentType.Simple {

        override val mdlBaseTypeDefinition: ResolvedType =
            rawPart.content.derivation.base?.let { schema.type(it) } ?: AnyType

        override val mdlContentType: ComplexTypeModel.ContentType get() = this

        override val mdlSimpleTypeDefinition: SimpleTypeModel = run {
            val baseType = mdlBaseTypeDefinition
            val baseTypeComplexBase = (baseType as? ResolvedComplexType)?.mdlBaseTypeDefinition
            val complexBaseType = (mdlBaseTypeDefinition as? ComplexTypeModel)?.mdlContentType
            val r = when {
/*
                derivation is XSSimpleContentRestriction -> {
                    check (complexBaseType is ComplexTypeModel.ContentType.Simple)
                    val b = derivation.simpleType?.let { ResolvedLocalSimpleType(it, schema) } ?: complexBaseType.mdlSimpleTypeDefinition


                    object : SimpleTypeModel {
                        override val mdlTargetNamespace: VAnyURI? = schema.targetNamespace
                        override val mdlFinal: Set<Nothing> get() = emptySet()
                        override val mdlContext: ResolvedComplexType = context
                        override val mdlBaseTypeDefinition: TypeModel = b
                        override val mdlFacets: List<T_Facet> get() = TODO("restriction children facets")
                        override val mdlFundamentalFacects: List<T_Facet> get() = TODO("unclear what this is")
                        override val mdlVariety: SimpleTypeModel.Variety get() = TODO("b.{variety}")
                        override val mdlPrimitiveTypeDefinition: PrimitiveDatatype get() = TODO("b.{primitive type definition}")
                        override val mdlItemTypeDefinition: SimpleTypeModel get() = TODO("b.{item type definition}")
                        override val mdlMemberTypeDefinitions: List<SimpleTypeModel> get() = TODO("b.{memberTypeDefinitions}")
                        override val mdlAnnotations: List<AnnotationModel> get() = emptyList()
                    }
                }
                complexBaseType?.mdlVariety == ComplexTypeModel.Variety.MIXED -> TODO()
*/

                else -> AnySimpleType
            }
            TODO()
        }

        override val mdlDerivationMethod: ComplexTypeModel.DerivationMethod = rawPart.content.derivation.derivationMethod
    }

    private class ShorthandModelImpl(rawPart: XSGlobalComplexTypeShorthand, schema: ResolvedSchemaLike) : ComplexModelbase(rawPart, schema) {
        override val mdlDerivationMethod: ComplexTypeModel.DerivationMethod get() = ComplexTypeModel.DerivationMethod.RESTRICION
        override val mdlBaseTypeDefinition: ResolvedType get() = AnyType
    }

    private class ComplexModelImpl(rawPart: XSGlobalComplexTypeComplex, schema: ResolvedSchemaLike) : ComplexModelbase(rawPart, schema) {
        override val mdlDerivationMethod: ComplexTypeModel.DerivationMethod =
            rawPart.content.derivation.derivationMethod

        override val mdlBaseTypeDefinition: ResolvedType = schema.type(requireNotNull(rawPart.content.derivation.base))
    }

}
