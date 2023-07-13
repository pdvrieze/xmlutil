/*
 * Copyright (c) 2021.
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
import io.github.pdvrieze.formats.xmlschema.datatypes.primitiveInstances.VID
import io.github.pdvrieze.formats.xmlschema.datatypes.primitiveInstances.VNCName
import io.github.pdvrieze.formats.xmlschema.datatypes.primitiveInstances.VString
import io.github.pdvrieze.formats.xmlschema.datatypes.primitiveTypes.IDType
import io.github.pdvrieze.formats.xmlschema.datatypes.serialization.XSAnnotation
import io.github.pdvrieze.formats.xmlschema.datatypes.serialization.XSIElement
import io.github.pdvrieze.formats.xmlschema.model.*
import io.github.pdvrieze.formats.xmlschema.types.*
import nl.adaptivity.xmlutil.QName

sealed class ResolvedElement(final override val schema: ResolvedSchemaLike) : OptNamedPart, T_Element, ElementModel,
    SimpleTypeContext, ResolvedTypeContext, ResolvedTerm {

    abstract override val rawPart: XSIElement
    abstract val scope: T_Scope

    override val type: QName?
        get() = rawPart.type
    override val nillable: Boolean get() = rawPart.nillable ?: false

    override val default: VString? get() = rawPart.default
    override val fixed: VString? get() = rawPart.fixed

    val valueConstraint: ValueConstraint? by lazy {
        val rawDefault = rawPart.default
        val rawFixed = rawPart.fixed
        when {
            rawDefault != null && rawFixed != null ->
                throw IllegalArgumentException("An element ${rawPart.name} cannot have default and fixed attributes both")

            rawDefault != null -> ValueConstraint.Default(rawDefault)
            rawFixed != null -> ValueConstraint.Fixed(rawFixed)
            else -> null
        }
    }
    override val id: VID? get() = rawPart.id

    override val localType: T_Type?
        get() = rawPart.localType

    override val name: VNCName? get() = rawPart.name

    override val annotation: XSAnnotation? get() = rawPart.annotation

    override val alternatives: List<T_AltType> get() = rawPart.alternatives

    abstract override val uniques: List<ResolvedUnique>

    abstract override val keys: List<ResolvedKey>

    abstract override val keyrefs: List<ResolvedKeyRef>

    abstract val model: Model

    override val mdlTypeDefinition: ResolvedType get() = model.mdlTypeDefinition
    override val mdlTypeTable: ElementModel.TypeTable? get() = model.mdlTypeTable
    override val mdlNillable: Boolean get() = model.mdlNillable
    override val mdlValueConstraint: ValueConstraintModel? get() = model.mdlValueConstraint
    override val mdlIdentityConstraints: Set<ResolvedIdentityConstraint> get() = model.mdlIdentityConstraints
    override val mdlSubstitutionGroupAffiliations: List<ElementModel.Use> get() = model.mdlSubstitutionGroupAffiliations
    override val mdlDisallowedSubstitutions: T_BlockSet get() = model.mdlDisallowedSubstitutions
    override val mdlSubstitutionGroupExclusions: Set<T_BlockSetValues> get() = model.mdlSubstitutionGroupExclusions
    override val mdlAbstract: Boolean get() = model.mdlAbstract
    override val mdlAnnotations: AnnotationModel? get() = model.mdlAnnotations
    override val mdlName: VNCName? get() = model.mdlName

    /**
     * disallowed substitutions
     */
    override val block: Set<T_BlockSetValues> get() = rawPart.block ?: schema.blockDefault

    override val otherAttrs: Map<QName, String>
        get() = rawPart.otherAttrs

    protected fun checkSingleType() {
        require(rawPart.type == null || rawPart.localType == null) {
            "Types can only be specified in one way"
        }
    }

    override fun check(checkedTypes: MutableSet<QName>) {
        super<OptNamedPart>.check(checkedTypes)
        for (keyref in keyrefs) {
            keyref.check(checkedTypes)
            checkNotNull(keyref.mdlReferencedKey)
        }
        default?.let { d ->
            check(fixed == null) { "fixed and default can not both be present on element: ${name ?: (this as Ref).mdlTerm.mdlName}" }
            check((mdlTypeDefinition as? ResolvedSimpleType)?.mdlPrimitiveTypeDefinition != IDType) {
                "ID types can not have fixed values"
            }
            mdlTypeDefinition.validate(d)
        }
        fixed?.let { f ->
            check((mdlTypeDefinition as? ResolvedSimpleType)?.mdlPrimitiveTypeDefinition != IDType) {
                "ID types can not have fixed values"
            }
            mdlTypeDefinition.validate(f)
        }

    }

    override fun collectConstraints(collector: MutableList<ResolvedIdentityConstraint>) {
        collector.addAll(mdlIdentityConstraints)
        (mdlTypeDefinition as? ResolvedLocalComplexType)?.collectConstraints(collector)
    }

    interface Use : ElementModel.Use
    interface Ref : ElementModel.Ref {
        override val mdlTerm: ResolvedGlobalElement
    }

    interface Model : ElementModel {
        override val mdlIdentityConstraints: Set<ResolvedIdentityConstraint>
        override val mdlTypeDefinition: ResolvedType
        override val mdlSubstitutionGroupAffiliations: List<ElementModel.Use>
    }

    protected abstract class ModelImpl(rawPart: XSIElement, schema: ResolvedSchemaLike, context: ResolvedElement) :
        Model {
        final override val mdlNillable: Boolean = rawPart.nillable ?: false

        abstract override val mdlSubstitutionGroupAffiliations: List<ElementModel.Use>

        final override val mdlDisallowedSubstitutions: T_BlockSet =
            (rawPart.block ?: schema.blockDefault)


        final override val mdlSubstitutionGroupExclusions: Set<T_BlockSetValues> =
            (rawPart.final ?: schema.finalDefault).filterIsInstanceTo(HashSet())

        final override val mdlAbstract: Boolean = rawPart.abstract ?: false

        final override val mdlAnnotations: AnnotationModel? = rawPart.annotation.models()

        final override val mdlIdentityConstraints: Set<ResolvedIdentityConstraint> =
            mutableSetOf<ResolvedIdentityConstraint>().also { set ->
                rawPart.keys.mapTo(set) { ResolvedKey(it, schema, context) }
                rawPart.uniques.mapTo(set) { ResolvedUnique(it, schema, context) }
                rawPart.keyrefs.mapTo(set) { ResolvedKeyRef(it, schema, context) }
            }

        final override val mdlTypeDefinition: ResolvedType =
            rawPart.localType?.let { ResolvedLocalType(it, schema, context) }
                ?: rawPart.type?.let { schema.type(it) }
                ?: rawPart.substitutionGroup?.firstOrNull()?.let { schema.element(it).mdlTypeDefinition }
                ?: AnyType

    }

}

