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

import io.github.pdvrieze.formats.xmlschema.datatypes.primitiveInstances.VAnyURI
import io.github.pdvrieze.formats.xmlschema.datatypes.primitiveInstances.VNCName
import io.github.pdvrieze.formats.xmlschema.datatypes.serialization.XSGroup
import io.github.pdvrieze.formats.xmlschema.model.AnnotationModel
import io.github.pdvrieze.formats.xmlschema.model.GroupDefModel
import io.github.pdvrieze.formats.xmlschema.resolved.particles.ResolvedParticle
import io.github.pdvrieze.formats.xmlschema.types.T_NamedGroup
import nl.adaptivity.xmlutil.QName

class ResolvedGlobalGroup(
    override val rawPart: XSGroup,
    override val schema: ResolvedSchemaLike,
    val location: String,
) : ResolvedGroupBase, NamedPart, T_NamedGroup, GroupDefModel, ResolvedGroupLikeTerm, ResolvedAllMember,
    ResolvedLocalElement.Parent, ResolvedParticleParent {

    internal constructor(rawPart: SchemaAssociatedElement<XSGroup>, schema: ResolvedSchemaLike) :
            this(rawPart.element, schema, rawPart.schemaLocation)

    override val mdlName: VNCName
        get() = rawPart.name

    override val mdlTargetNamespace: VAnyURI?
        get() = schema.targetNamespace

    override val mdlModelGroup: ResolvedModelGroup by lazy {
        val r: ResolvedModelGroup = when (val c = rawPart.content) {
            is XSGroup.All -> AllImpl(this, c, schema)
            is XSGroup.Choice -> ChoiceImpl(this, c, schema)
            is XSGroup.Sequence -> SequenceImpl(this, c, schema)
        }
        r
    }

    override val mdlAnnotations: AnnotationModel?
        get() = rawPart.annotation.models()

    override fun check(checkedTypes: MutableSet<QName>) {
        rawPart.content
//        TODO("not implemented")
    }

    @Deprecated("incorrect")
    override val particle: T_NamedGroup.Particle
        get() = TODO()

    override val name: VNCName
        get() = rawPart.name

    override val targetNamespace: VAnyURI?
        get() = schema.targetNamespace

    override fun collectConstraints(collector: MutableList<ResolvedIdentityConstraint>) {
        for(p in mdlModelGroup.mdlParticles) {
            if (p is ResolvedTerm) p.collectConstraints(collector)
        }
    }

    override val mdlParticles: List<ResolvedParticle<ResolvedTerm>>
        get() = mdlModelGroup.mdlParticles

    private sealed class ModelGroupBase(val schema: ResolvedSchemaLike) : ResolvedModelGroup {
        abstract val rawPart: XSGroup.XSGroupElement
        override val mdlAnnotations: AnnotationModel? get() = rawPart.annotation.models()
    }

    private class AllImpl(parent: ResolvedGlobalGroup, override val rawPart: XSGroup.All, schema: ResolvedSchemaLike) : ModelGroupBase(schema),
        IResolvedAll {
        override val mdlParticles: List<ResolvedParticle<ResolvedAllMember>> = rawPart.particles.map {
            ResolvedParticle.allMember(parent, it, schema)
        }

        override fun collectConstraints(collector: MutableList<ResolvedIdentityConstraint>) {
            for (p in mdlParticles) {
                if (p is ResolvedTerm) {
                    p.collectConstraints(collector)
                }
            }
        }
    }

    private class ChoiceImpl(parent: ResolvedGlobalGroup, override val rawPart: XSGroup.Choice, schema: ResolvedSchemaLike) :
        ModelGroupBase(schema),
        IResolvedChoice {

        override val mdlParticles: List<ResolvedParticle<ResolvedChoiceSeqMember>> = rawPart.particles.map {
            ResolvedParticle.choiceSeqMember(parent, it, schema)
        }

        override fun collectConstraints(collector: MutableList<ResolvedIdentityConstraint>) {
            for (p in mdlParticles) {
                if (p is ResolvedTerm) {
                    p.collectConstraints(collector)
                }
            }
        }
    }

    private class SequenceImpl(parent: ResolvedGlobalGroup, override val rawPart: XSGroup.Sequence, schema: ResolvedSchemaLike) :
        ModelGroupBase(schema),
        IResolvedSequence {
        override val mdlParticles: List<ResolvedParticle<ResolvedChoiceSeqMember>> = rawPart.particles.map {
            ResolvedParticle.choiceSeqMember(parent, it, schema)
        }

        override fun collectConstraints(collector: MutableList<ResolvedIdentityConstraint>) {
            for (p in mdlParticles) {
                if (p is ResolvedTerm) {
                    p.collectConstraints(collector)
                }
            }
        }
    }
}
