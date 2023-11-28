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

import io.github.pdvrieze.formats.xmlschema.datatypes.serialization.XSGroup
import io.github.pdvrieze.formats.xmlschema.datatypes.serialization.XSLocalElement
import io.github.pdvrieze.formats.xmlschema.resolved.checking.CheckHelper
import nl.adaptivity.xmlutil.QName

class ResolvedGlobalGroup internal constructor(
    elemPart: SchemaElement<XSGroup>,
    schema: ResolvedSchemaLike,
    val location: String,
) : ResolvedGroupBase, ResolvedAnnotated, VElementScope.Member, NamedPart {
    override val model: ResolvedAnnotated.Model by lazy { ResolvedAnnotated.Model(elemPart.elem) }

    override val otherAttrs: Map<QName, String> = elemPart.elem.resolvedOtherAttrs()

    internal constructor(element: SchemaElement<XSGroup>, schema: ResolvedSchemaLike) :
            this(element, element.effectiveSchema(schema), element.schemaLocation)

    override val mdlQName: QName = elemPart.elem.name.toQname(schema.targetNamespace)

    val mdlModelGroup: ResolvedModelGroup = run {
        val content = elemPart.wrap { content }
        when (val c = content.elem) {
            is XSGroup.All -> AllImpl(this, content.cast(), schema)
            is XSGroup.Choice -> ChoiceImpl(this, content.cast(), schema)
            is XSGroup.Sequence -> SequenceImpl(this, content.cast(), schema)
        }
    }


    override fun hasLocalNsInContext(): Boolean {
        return mdlModelGroup.hasLocalNsInContext()
    }

    fun checkGroup(checkHelper: CheckHelper) {
        checkRecursion(mutableSetOf())

        mdlModelGroup.checkTerm(checkHelper)
    }

    internal fun checkRecursion(seen: MutableSet<ResolvedGlobalGroup>) {
        check (seen.add(this)) { "Circular group ref to $mdlQName" }
        val toCheck= ArrayList(mdlModelGroup.mdlParticles)

        while (toCheck.isNotEmpty()) {
            val p = toCheck.removeLast()
            when (p) {
                is ResolvedGroupRef -> p.model.referenced.checkRecursion(seen)
                is ResolvedModelGroup -> toCheck.addAll(p.mdlParticles)
            }
        }
    }

    fun collectConstraints(collector: MutableCollection<ResolvedIdentityConstraint>) {
        for(p in mdlModelGroup.mdlParticles) {
            if (p is ResolvedTerm) p.collectConstraints(collector)
        }
    }

    class Model internal constructor(
        parent: ResolvedGlobalGroup,
        elemPart: SchemaElement<XSGroup.XSGroupElement>,
        schema: ResolvedSchemaLike
    ) : ResolvedAnnotated.Model(elemPart.elem) {

        // TODO replace with operator invoke
        internal constructor(parent: ResolvedGlobalGroup, elemPart: SchemaElement<XSGroup>, schema: ResolvedSchemaLike,
                             dummy: Boolean = false) :
                this(parent, elemPart.wrap { content }, schema)

        val modelGroup: ResolvedModelGroup = when (val c = elemPart.elem) {
            is XSGroup.All -> AllImpl(parent, elemPart.cast(), schema)
            is XSGroup.Choice -> ChoiceImpl(parent, elemPart.cast(), schema)
            is XSGroup.Sequence -> SequenceImpl(parent, elemPart.cast(), schema)
        }
    }

    private sealed class ModelGroupBase(
        parent: ResolvedGlobalGroup,
        elemPart: SchemaElement<XSGroup.XSGroupElement>,
        schema: ResolvedSchemaLike
    ): ResolvedTerm {
        override val model: Model by lazy { Model(parent, elemPart, schema) }
        val mdlParticles: List<ResolvedParticle<ResolvedTerm>> get() = model.particles

        abstract override fun checkTerm(checkHelper: CheckHelper)
//        val mdlAnnotations: ResolvedAnnotation? get() = rawPart.annotation.models()
//        abstract val mdlParticles: List<ResolvedParticle<ResolvedTerm>>

        class Model(parent: ResolvedGlobalGroup, elemPart: SchemaElement<XSGroup.XSGroupElement>, schema: ResolvedSchemaLike): ResolvedAnnotated.Model(elemPart.elem) {
            val localInContext = elemPart.elem.particles.any { it.hasLocalNsInContext(schema) }

            val particles = elemPart.wrapEach { particles }.map { ResolvedParticle(parent, it, schema, localInContext) }
        }
    }

    private class AllImpl(parent: ResolvedGlobalGroup, elemPart: SchemaElement<XSGroup.All>, schema: ResolvedSchemaLike) :
        ModelGroupBase(parent, elemPart, schema), IResolvedAll {

        init {
            if (schema.version== SchemaVersion.V1_0) {
                require(elemPart.elem.particles.all { it is XSLocalElement }) {
                    "Schema 1.0 only allows element members for all model group schema components"
                }
            }
        }

        override fun checkTerm(checkHelper: CheckHelper) {
            super.checkTerm(checkHelper)
        }

    }

    private class ChoiceImpl(parent: ResolvedGlobalGroup, elemPart: SchemaElement<XSGroup.Choice>, schema: ResolvedSchemaLike) :
        ModelGroupBase(parent, elemPart, schema),
        IResolvedChoice {

        override fun checkTerm(checkHelper: CheckHelper) {
            super.checkTerm(checkHelper)
        }

    }

    private class SequenceImpl(
        parent: ResolvedGlobalGroup,
        elemPart: SchemaElement<XSGroup.Sequence>, schema: ResolvedSchemaLike
    ) : ModelGroupBase(parent, elemPart, schema), IResolvedSequence {

        override fun checkTerm(checkHelper: CheckHelper) {
            super.checkTerm(checkHelper)
        }

    }
}

