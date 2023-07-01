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
import io.github.pdvrieze.formats.xmlschema.datatypes.primitiveInstances.VID
import io.github.pdvrieze.formats.xmlschema.datatypes.primitiveTypes.PrimitiveDatatype
import io.github.pdvrieze.formats.xmlschema.datatypes.serialization.*
import io.github.pdvrieze.formats.xmlschema.model.AnnotationModel
import io.github.pdvrieze.formats.xmlschema.model.SimpleTypeModel
import io.github.pdvrieze.formats.xmlschema.model.TypeModel
import io.github.pdvrieze.formats.xmlschema.types.FundamentalFacets
import io.github.pdvrieze.formats.xmlschema.types.T_FullDerivationSet
import io.github.pdvrieze.formats.xmlschema.types.T_SimpleType
import nl.adaptivity.xmlutil.QName
import nl.adaptivity.xmlutil.isEquivalent
import nl.adaptivity.xmlutil.localPart
import nl.adaptivity.xmlutil.qname

sealed interface ResolvedSimpleType : ResolvedType, T_SimpleType, SimpleTypeModel {
    override val simpleDerivation: Derivation

    val model: Model

    override val mdlAnnotations: AnnotationModel? get() = model.mdlAnnotations

    override val mdlBaseTypeDefinition: TypeModel get() = model.mdlBaseTypeDefinition

    override val mdlFacets: List<XSFacet> get() = model.mdlFacets

    override val mdlFundamentalFacets: FundamentalFacets get() = model.mdlFundamentalFacets

    override val mdlVariety: SimpleTypeModel.Variety get() = model.mdlVariety

    override val mdlPrimitiveTypeDefinition: PrimitiveDatatype? get() = model.mdlPrimitiveTypeDefinition

    override val mdlItemTypeDefinition: ResolvedSimpleType? get() = model.mdlItemTypeDefinition as ResolvedSimpleType?

    @Suppress("UNCHECKED_CAST")
    override val mdlMemberTypeDefinitions: List<ResolvedSimpleType>
        get() = model.mdlMemberTypeDefinitions as List<ResolvedSimpleType>

    override val mdlFinal: T_FullDerivationSet
        get() = model.mdlFinal

    override fun check(
        seenTypes: SingleLinkedList<QName>,
        inheritedTypes: SingleLinkedList<QName>
    ) { // TODO maybe move to toplevel

        when (val n = (this as? OptNamedPart)?.name) {
            null -> simpleDerivation.check(SingleLinkedList(), inheritedTypes)
            else -> {
                val qName = n.toQname(schema.targetNamespace)
                simpleDerivation.check(SingleLinkedList(qName), inheritedTypes + qName)
            }
        }
    }

    sealed class Derivation(final override val schema: ResolvedSchemaLike) : T_SimpleType.Derivation, ResolvedPart {
        final override val annotation: XSAnnotation? get() = rawPart.annotation
        final override val id: VID? get() = rawPart.id

        abstract override val rawPart: T_SimpleType.Derivation
        abstract val baseType: ResolvedSimpleType
        abstract fun check(seenTypes: SingleLinkedList<QName>, inheritedTypes: SingleLinkedList<QName>)
    }

    interface Model: SimpleTypeModel {
        override val mdlFinal: T_FullDerivationSet
    }

    @Suppress("LeakingThis")
    sealed class ModelBase(
        rawPart: XSISimpleType,
        protected val schema: ResolvedSchemaLike,
        context: ResolvedSimpleType
    ) : SimpleTypeModel, Model {

        final override val mdlAnnotations: AnnotationModel? = rawPart.annotation.models()
        final override val mdlBaseTypeDefinition: ResolvedSimpleType
        final override val mdlItemTypeDefinition: ResolvedSimpleType?
        final override val mdlMemberTypeDefinitions: List<SimpleTypeModel>
        final override val mdlVariety: SimpleTypeModel.Variety
        final override val mdlPrimitiveTypeDefinition: PrimitiveDatatype?

        init {
            val typeName = (rawPart as? XSGlobalSimpleType)?.let { qname(schema.targetNamespace?.value, it.name.xmlString) }
            val simpleDerivation = rawPart.simpleDerivation

            mdlBaseTypeDefinition = when {
                simpleDerivation !is XSSimpleRestriction -> AnySimpleType

                rawPart is XSGlobalSimpleType &&
                        typeName !=null && simpleDerivation.base!=null && typeName.isEquivalent(simpleDerivation.base) -> {
                    require(schema is ResolvedRedefine) { "Only redefines can have 'self-referencing types'" }
                    ResolvedGlobalSimpleType(schema.nestedSchema.simpleTypes.single { it.name.xmlString == typeName.localPart }, schema)
                }

                else -> simpleDerivation.base?.let {
                    require(typeName == null || !it.isEquivalent(typeName))
                    schema.simpleType (it)
                } ?: ResolvedLocalSimpleType(simpleDerivation.simpleType!!, schema, context)
            }


            mdlItemTypeDefinition = when (simpleDerivation) {
                is XSSimpleList -> when (mdlBaseTypeDefinition) {
                    AnySimpleType -> simpleDerivation.itemTypeName?.let { schema.simpleType(it) }
                        ?: ResolvedLocalSimpleType(simpleDerivation.simpleType!!, schema, context)

                    else -> recurseBaseType(typeName, rawPart.simpleDerivation, schema, context) { it.mdlItemTypeDefinition }
                }

                else -> null
            }

            mdlMemberTypeDefinitions = when {
                    simpleDerivation !is XSSimpleUnion -> emptyList()
                    mdlBaseTypeDefinition == AnySimpleType -> simpleDerivation.memberTypes?.map { schema.simpleType(it) }
                        ?: simpleDerivation.simpleTypes.map { ResolvedLocalSimpleType(it, schema, this@ModelBase) }

                    else -> recurseBaseType(typeName, rawPart.simpleDerivation, schema, context) { it.mdlMemberTypeDefinitions } ?: emptyList()
                }


            mdlVariety = when (simpleDerivation) {
                is XSSimpleList -> SimpleTypeModel.Variety.LIST
                is XSSimpleRestriction -> recurseBaseType(rawPart, schema, context) { it.mdlVariety }
                    ?: SimpleTypeModel.Variety.ATOMIC
                is XSSimpleUnion -> SimpleTypeModel.Variety.UNION
                else -> error("Unreachable/unsupported derivation")
            }

            mdlPrimitiveTypeDefinition = when (mdlVariety) {
                SimpleTypeModel.Variety.ATOMIC -> when (val b = mdlBaseTypeDefinition) {
                    is PrimitiveDatatype -> b
                    else -> recurseBaseType(rawPart, schema, context) { it.mdlPrimitiveTypeDefinition }
                        ?: run { null }
                }

                else -> null
            }



        }



        final override val mdlFundamentalFacets: FundamentalFacets
            get() = TODO("not implemented")

        final override val mdlFacets: List<XSFacet>
            get() = TODO("not implemented")

        protected companion object {
            fun <R> recurseBaseType(
                rawPart: XSISimpleType,
                schema: ResolvedSchemaLike,
                context: ResolvedSimpleType,
                seenTypes: SingleLinkedList<QName> = SingleLinkedList(),
                valueFun: (ResolvedSimpleType) -> R?
            ): R? = recurseBaseType(
                (rawPart as? XSGlobalSimpleType)?.let { qname(schema.targetNamespace?.value, it.name.xmlString) },
                rawPart.simpleDerivation,
                schema, context, seenTypes, valueFun
            )

            fun <R> recurseBaseType(
                thisName: QName?,
                d: XSSimpleDerivation?,
                schema: ResolvedSchemaLike,
                context: ResolvedSimpleType,
                seenTypes: SingleLinkedList<QName> = SingleLinkedList(),
                valueFun: (ResolvedSimpleType) -> R?
            ): R? {
//                val thisName = (rawPart as? XSGlobalSimpleType)?.let { qname(schema.targetNamespace?.value, it.name.xmlString) }

                val newSeen = when (thisName) {
                    null -> seenTypes
                    in seenTypes -> throw IllegalArgumentException("Recursion in seen types")
                    else -> seenTypes + thisName
                }

                return when (/*val d = rawPart.simpleDerivation*/d) {
                    !is XSSimpleRestriction -> valueFun(AnySimpleType)
                    else -> {
                        when (val base = d.base) {
                            null -> valueFun(ResolvedLocalSimpleType(d.simpleType!!, schema, context))
                            thisName -> null
                            in seenTypes -> throw IllegalArgumentException("Loop in base type definition")
                            else -> {
                                recurseBaseType(
                                    base,
                                    (schema.simpleType(base).rawPart as? XSSimpleDerivation),
                                    schema,
                                    context,
                                    newSeen,
                                    valueFun
                                )
                            }
                        }
                    }
                }
            }
        }
    }


}
