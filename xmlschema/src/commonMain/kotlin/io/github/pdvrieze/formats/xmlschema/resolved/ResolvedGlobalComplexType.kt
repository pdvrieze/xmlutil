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
import io.github.pdvrieze.formats.xmlschema.datatypes.impl.SingleLinkedList
import io.github.pdvrieze.formats.xmlschema.datatypes.primitiveInstances.VAnyURI
import io.github.pdvrieze.formats.xmlschema.datatypes.primitiveInstances.VID
import io.github.pdvrieze.formats.xmlschema.datatypes.primitiveInstances.VNCName
import io.github.pdvrieze.formats.xmlschema.datatypes.primitiveInstances.VNonNegativeInteger
import io.github.pdvrieze.formats.xmlschema.datatypes.primitiveTypes.PrimitiveDatatype
import io.github.pdvrieze.formats.xmlschema.datatypes.serialization.*
import io.github.pdvrieze.formats.xmlschema.model.*
import io.github.pdvrieze.formats.xmlschema.types.*
import nl.adaptivity.xmlutil.QName
import nl.adaptivity.xmlutil.qname

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
            is XSGlobalComplexTypeComplex -> ComplexModelImpl(this, r, schema)
            is XSGlobalComplexTypeShorthand -> ShorthandModelImpl(this, r, schema)
            is XSGlobalComplexTypeSimple -> SimpleModelImpl(r, schema, this)
        }

    }

    override val mdlName: VNCName get() = model.mdlName

    override val mdlTargetNamespace: VAnyURI? get() = model.mdlTargetNamespace

    interface Model : ResolvedComplexType.Model, ComplexTypeModel.Global

    private abstract class ModelBase(rawPart: XSGlobalComplexType, schema: ResolvedSchemaLike) :
        ResolvedComplexType.ModelBase(rawPart, schema), Model {
        final override val mdlName: VNCName = rawPart.name
        final override val mdlTargetNamespace: VAnyURI? = rawPart.targetNamespace ?: schema.targetNamespace

        final override val mdlAbstract: Boolean = rawPart.abstract ?: false

        final override val mdlProhibitedSubstitutions: T_DerivationSet =
            rawPart.block ?: schema.blockDefault.toDerivationSet()

        final override val mdlFinal: T_DerivationSet = rawPart.final ?: schema.finalDefault.toDerivationSet()

        final override val mdlAttributeUses: Set<AttributeModel.Use> get() = TODO()

        final override val mdlAttributeWildcard: WildcardModel
            get() = TODO("not implemented")

        init {
            val pseudoAttributesGroup =
                (schema as? ResolvedSchema)?.defaultAttributes?.takeIf { rawPart.defaultAttributesApply != false }
                    ?.let { defName ->
                        ResolvedAttributeGroupRef(XSAttributeGroupRef(ref = defName), schema)
                    }


        }
    }

    private abstract class ComplexModelbase(
        type: ResolvedComplexType,
        rawPart: XSGlobalComplexType,
        schema: ResolvedSchemaLike,
    ) : ModelBase(rawPart, schema) {

        final override val mdlContentType: ResolvedContentType

        final override val mdlBaseTypeDefinition: ResolvedType

        final override val mdlDerivationMethod: ComplexTypeModel.DerivationMethod


        init {

            val content = rawPart.content as XSI_ComplexContent.Complex
            val derivation: XSI_ComplexDerivation

            when (content) {
                is XSComplexContent -> {
                    derivation = content.derivation
                    mdlBaseTypeDefinition =
                        schema.type(requireNotNull(derivation.base) { "Missing base attribute for complex type derivation" })
                }

                is IXSComplexTypeShorthand -> {
                    derivation = content
                    check(derivation.base == null) { " Shorthand has no base" }
                    mdlBaseTypeDefinition = AnyType
                }
            }


            mdlDerivationMethod = when (derivation) {
                is XSComplexContent.XSExtension -> ComplexTypeModel.DerivationMethod.EXTENSION
                else -> ComplexTypeModel.DerivationMethod.RESTRICION
            }


            val effectiveMixed = (content as? XSComplexContent)?.mixed ?: rawPart.mixed ?: false
            val term = derivation.term

            val explicitContent: ResolvedGroupParticle<*>? = when {
                (term == null) ||
                        (term.maxOccurs == T_AllNNI(0)) ||
                        (term is XSAll || term is XSSequence && !term.hasChildren()) ||
                        (term is XSChoice && term.minOccurs?.toUInt() == 0u && !term.hasChildren()) -> null


                else -> ResolvedGroupParticle(type, term, schema)
            }

            val effectiveContent: ResolvedParticle<*>? = explicitContent ?: when {
                !effectiveMixed -> null
                else -> ResolvedSequence(
                    type,
                    XSSequence(minOccurs = VNonNegativeInteger(1), maxOccurs = T_AllNNI(1)),
                    schema
                )
            }

            val explicitContentType: ComplexTypeModel.ContentType = when {
                derivation is XSComplexContent.XSRestriction ||
                        derivation is IXSComplexTypeShorthand ->
                    contentType(effectiveMixed, effectiveContent)


                mdlBaseTypeDefinition !is ResolvedComplexType -> // simple type
                    contentType(effectiveMixed, effectiveContent)

                mdlBaseTypeDefinition.mdlContentType.mdlVariety.let {
                    it == ComplexTypeModel.Variety.SIMPLE || it == ComplexTypeModel.Variety.EMPTY
                } -> contentType(effectiveMixed, effectiveContent)

                effectiveContent == null -> mdlBaseTypeDefinition.mdlContentType
                else -> {
                    val baseParticle = (mdlBaseTypeDefinition.mdlContentType as ResolvedElementBase).mdlParticle
                    val baseTerm: ResolvedTerm = baseParticle.mdlTerm
                    val part = when {
                        baseTerm is ResolvedAll && explicitContent == null -> baseParticle
                        (baseTerm is ResolvedAll && effectiveContent.mdlTerm is AllModel<*>) -> {
                            val p = baseTerm.mdlParticles
                            TODO()
/*
                            SyntheticAll(
                                minOccurs = effectiveContent.mdlMinOccurs,
                                maxOccurs = T_AllNNI(1),
                                particles = p,
                            )
*/
/*
                            XSAll(
                                minOccurs = effectiveContent.mdlMinOccurs,
                                maxOccurs = T_AllNNI(1),
                                particles = p,
                                elements = p.filterIsInstance<XSLocalElement>(),
                                groups = p.filterIsInstance<XSGroupRef>(),
                                anys = p.filterIsInstance<XSAny>(),
                            )
*/
                        }

                        else -> {
                            val p = listOf(baseParticle) + listOfNotNull(effectiveContent)
                            XSSequence(
                                minOccurs = VNonNegativeInteger(1),
                                maxOccurs = T_AllNNI(1),
                            )
                        }
                    }

                    TODO()
                }
            }

            mdlContentType = EmptyContentType
        }
    }

    interface ResolvedContentType : ComplexTypeModel.ContentType

    object EmptyContentType : ComplexTypeModel.ContentType.Empty, ResolvedContentType


    interface ResolvedElementBase : ResolvedContentType, ComplexTypeModel.ContentType.ElementBase {
        override val mdlParticle: ResolvedParticle<ResolvedTerm>
    }

    class MixedContentType(
        override val mdlParticle: ResolvedParticle<*>
    ) : ComplexTypeModel.ContentType.Mixed, ResolvedElementBase {
        override val openContent: OpenContentModel? get() = null
    }

    class ElementOnlyContentType(
        override val mdlParticle: ResolvedParticle<*>
    ) : ComplexTypeModel.ContentType.Mixed, ResolvedElementBase {
        override val openContent: OpenContentModel? get() = null
    }

    interface ResolvedSimpleContentType : ResolvedContentType, ComplexTypeModel.GlobalSimpleContent,
        ComplexTypeModel.ContentType.Simple

    private class SimpleModelImpl(
        rawPart: XSGlobalComplexTypeSimple,
        schema: ResolvedSchemaLike,
        context: ResolvedComplexType
    ) : ModelBase(rawPart, schema), ResolvedSimpleContentType {

        override val mdlBaseTypeDefinition: ResolvedType

        override val mdlContentType: ResolvedSimpleContentType get() = this

        override val mdlSimpleTypeDefinition: SimpleTypeModel

        init {
            val qname = qname(schema.targetNamespace?.value, rawPart.name.xmlString)
            val derivation = rawPart.content.derivation
            val baseType: ResolvedType = derivation.base?.let { schema.type(it) } ?: AnyType

            mdlBaseTypeDefinition = baseType


            val complexBaseDerivation = (baseType as? ResolvedComplexType)?.mdlDerivationMethod
            val baseTypeComplexBase = (baseType as? ResolvedComplexType)?.mdlBaseTypeDefinition
            val complexBaseContentType = (baseType as? ComplexTypeModel)?.mdlContentType

            when {
                baseType is ResolvedComplexType &&
                        complexBaseContentType is SimpleModelImpl &&
                        derivation is XSSimpleContentRestriction -> {
                    val b = derivation.simpleType?.let { ResolvedLocalSimpleType(it, schema, context) }
                        ?: complexBaseContentType.mdlSimpleTypeDefinition

                    mdlSimpleTypeDefinition = object : SimpleTypeModel {
                        //                            override val mdlTargetNamespace: VAnyURI? = schema.targetNamespace
                        override val mdlFinal: Set<Nothing> get() = emptySet()

                        //                            override val mdlContext: ResolvedComplexType = context
                        override val mdlBaseTypeDefinition: TypeModel = b
                        override val mdlFacets: List<T_Facet> get() = TODO("restriction children facets")
                        override val mdlFundamentalFacets: List<T_Facet> get() = TODO("unclear what this is")
                        override val mdlVariety: SimpleTypeModel.Variety get() = TODO("b.{variety}")
                        override val mdlPrimitiveTypeDefinition: PrimitiveDatatype get() = TODO("b.{primitive type definition}")
                        override val mdlItemTypeDefinition: SimpleTypeModel get() = TODO("b.{item type definition}")
                        override val mdlMemberTypeDefinitions: List<SimpleTypeModel> get() = TODO("b.{memberTypeDefinitions}")
                        override val mdlAnnotations: Nothing? get() = null

                    }
                }

                baseType is ResolvedComplexType &&
                        complexBaseContentType is ComplexTypeModel.ContentType.Mixed &&
                        complexBaseContentType.mdlParticle.mdlIsEmptiable() &&
                        derivation is XSSimpleContentRestriction -> {
                    val sb =
                        derivation.simpleType ?: error("Simple type definition constrained violated: 3.4.2.2 - step 2")

                    val st = ResolvedLocalSimpleType(
                        XSLocalSimpleType(
                            simpleDerivation = XSSimpleRestriction(
                                simpleType = sb,
                                facets = derivation.facets
                            )
                        ), schema, context
                    )

                    mdlSimpleTypeDefinition = st
                }

                else -> mdlSimpleTypeDefinition = baseType as ResolvedSimpleType
            }


        }

        override val mdlDerivationMethod: ComplexTypeModel.DerivationMethod =
            rawPart.content.derivation.derivationMethod
    }

    private class ShorthandModelImpl(
        parent: ResolvedComplexType,
        rawPart: XSGlobalComplexTypeShorthand,
        schema: ResolvedSchemaLike
    ) : ComplexModelbase(parent, rawPart, schema), ComplexTypeModel.GlobalImplicitContent {

    }

    private class ComplexModelImpl(
        parent: ResolvedComplexType,
        rawPart: XSGlobalComplexTypeComplex,
        schema: ResolvedSchemaLike
    ) : ComplexModelbase(parent, rawPart, schema), ComplexTypeModel.GlobalComplexContent {
    }

    companion object {
        internal fun contentType(effectiveMixed: Boolean, particle: ResolvedParticle<*>?): ResolvedContentType {
            return when {
                particle == null -> EmptyContentType
                effectiveMixed -> MixedContentType(particle)
                else -> ElementOnlyContentType(particle)
            }
        }

    }

}
