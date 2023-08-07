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
import io.github.pdvrieze.formats.xmlschema.resolved.checking.CheckHelper
import nl.adaptivity.xmlutil.QName

class ResolvedGlobalGroup(
    rawPart: XSGroup,
    schema: ResolvedSchemaLike,
    val location: String,
) : ResolvedGroupBase, ResolvedAnnotated, VElementScope.Member, NamedPart {
    override val model: ResolvedAnnotated.Model by lazy { ResolvedAnnotated.Model(rawPart) }

    override val otherAttrs: Map<QName, String> = rawPart.resolvedOtherAttrs()

    internal constructor(rawPart: SchemaAssociatedElement<XSGroup>, schema: ResolvedSchemaLike) :
            this(rawPart.element, schema, rawPart.schemaLocation)

    override val mdlQName: QName = rawPart.name.toQname(schema.targetNamespace)

    val mdlModelGroup: ResolvedModelGroup = when (val c = rawPart.content) {
        is XSGroup.All -> AllImpl(this, c, schema)
        is XSGroup.Choice -> ChoiceImpl(this, c, schema)
        is XSGroup.Sequence -> SequenceImpl(this, c, schema)
    }

    fun checkGroup(checkHelper: CheckHelper) {
        mdlModelGroup.checkTerm(checkHelper)
    }

    fun collectConstraints(collector: MutableList<ResolvedIdentityConstraint>) {
        for(p in mdlModelGroup.mdlParticles) {
            if (p is ResolvedTerm) p.collectConstraints(collector)
        }
    }

    class Model(
        parent: ResolvedGlobalGroup,
        rawPart: XSGroup.XSGroupElement,
        schema: ResolvedSchemaLike
    ) : ResolvedAnnotated.Model(rawPart) {

        constructor(parent: ResolvedGlobalGroup, rawPart: XSGroup, schema: ResolvedSchemaLike) :
                this(parent, rawPart.content, schema)

        val modelGroup: ResolvedModelGroup = when (val c = rawPart) {
            is XSGroup.All -> AllImpl(parent, c, schema)
            is XSGroup.Choice -> ChoiceImpl(parent, c, schema)
            is XSGroup.Sequence -> SequenceImpl(parent, c, schema)
        }
    }

    private sealed class ModelGroupBase(
        parent: ResolvedGlobalGroup,
        rawPart: XSGroup.XSGroupElement,
        schema: ResolvedSchemaLike
    ): ResolvedTerm {
        override val model: Model by lazy { Model(parent, rawPart, schema) }
        val mdlParticles: List<ResolvedParticle<ResolvedTerm>> get() = model.particles

        abstract override fun checkTerm(checkHelper: CheckHelper)
//        val mdlAnnotations: ResolvedAnnotation? get() = rawPart.annotation.models()
//        abstract val mdlParticles: List<ResolvedParticle<ResolvedTerm>>

        class Model(parent: ResolvedGlobalGroup, rawPart: XSGroup.XSGroupElement, schema: ResolvedSchemaLike): ResolvedAnnotated.Model(rawPart) {
            val particles = rawPart.particles.map { ResolvedParticle(parent, it, schema) }
        }
    }

    private class AllImpl(parent: ResolvedGlobalGroup, rawPart: XSGroup.All, schema: ResolvedSchemaLike) :
        ModelGroupBase(parent, rawPart, schema), IResolvedAll {

        override fun collectConstraints(collector: MutableCollection<ResolvedIdentityConstraint>) {
            for (p in mdlParticles) {
                if (p is ResolvedTerm) {
                    p.collectConstraints(collector)
                }
            }
        }

        override fun checkTerm(checkHelper: CheckHelper) {
            super.checkTerm(checkHelper)
        }

    }

    private class ChoiceImpl(parent: ResolvedGlobalGroup, rawPart: XSGroup.Choice, schema: ResolvedSchemaLike) :
        ModelGroupBase(parent, rawPart, schema),
        IResolvedChoice {

        override fun collectConstraints(collector: MutableCollection<ResolvedIdentityConstraint>) {
            for (p in mdlParticles) {
                if (p is ResolvedTerm) {
                    p.collectConstraints(collector)
                }
            }
        }

        override fun checkTerm(checkHelper: CheckHelper) {
            super.checkTerm(checkHelper)
        }

    }

    private class SequenceImpl(
        parent: ResolvedGlobalGroup,
        rawPart: XSGroup.Sequence, schema: ResolvedSchemaLike
    ) : ModelGroupBase(parent, rawPart, schema), IResolvedSequence {

        override fun collectConstraints(collector: MutableCollection<ResolvedIdentityConstraint>) {
            for (p in mdlParticles) {
                if (p is ResolvedTerm) {
                    p.collectConstraints(collector)
                }
            }
        }

        override fun checkTerm(checkHelper: CheckHelper) {
            super.checkTerm(checkHelper)
        }

    }
}

