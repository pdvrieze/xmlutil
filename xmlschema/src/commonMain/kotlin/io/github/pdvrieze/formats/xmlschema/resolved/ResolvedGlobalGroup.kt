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
import io.github.pdvrieze.formats.xmlschema.datatypes.primitiveInstances.VNonNegativeInteger
import io.github.pdvrieze.formats.xmlschema.datatypes.serialization.XSGroup
import io.github.pdvrieze.formats.xmlschema.types.VAllNNI
import nl.adaptivity.xmlutil.QName

class ResolvedGlobalGroup(
    override val rawPart: XSGroup,
    override val schema: ResolvedSchemaLike,
    val location: String,
) : ResolvedGroupBase, VElementScope.Member, NamedPart {

    final override val otherAttrs: Map<QName, String> = rawPart.resolvedOtherAttrs()

    internal constructor(rawPart: SchemaAssociatedElement<XSGroup>, schema: ResolvedSchemaLike) :
            this(rawPart.element, schema, rawPart.schemaLocation)

    val mdlName: VNCName
        get() = rawPart.name

    override val mdlQName: QName = rawPart.name.toQname(schema.targetNamespace)

    val mdlTargetNamespace: VAnyURI?
        get() = schema.targetNamespace

    val mdlModelGroup: ResolvedModelGroup by lazy {
        when (val c = rawPart.content) {
            is XSGroup.All -> AllImpl(this, c, schema)
            is XSGroup.Choice -> ChoiceImpl(this, c, schema)
            is XSGroup.Sequence -> SequenceImpl(this, c, schema)
        }
    }

    override val mdlAnnotations: ResolvedAnnotation?
        get() = rawPart.annotation.models()

    override fun check(checkedTypes: MutableSet<QName>) {
        super.check(checkedTypes)
        mdlModelGroup.check()
    }

    fun collectConstraints(collector: MutableList<ResolvedIdentityConstraint>) {
        for(p in mdlModelGroup.mdlParticles) {
            if (p is ResolvedTerm) p.collectConstraints(collector)
        }
    }

    private sealed class ModelGroupBase(rawPart: XSGroup.XSGroupElement, override val schema: ResolvedSchemaLike): ResolvedPart {

        final override val otherAttrs: Map<QName, String> = rawPart.resolvedOtherAttrs()

        abstract override val rawPart: XSGroup.XSGroupElement
//        val mdlAnnotations: ResolvedAnnotation? get() = rawPart.annotation.models()
//        abstract val mdlParticles: List<ResolvedParticle<ResolvedTerm>>
    }

    private class AllImpl(parent: ResolvedGlobalGroup, override val rawPart: XSGroup.All, schema: ResolvedSchemaLike) : ModelGroupBase(
        rawPart,
        schema
    ), IResolvedAll {


        override val mdlParticles: List<ResolvedParticle<ResolvedTerm>> = rawPart.particles.map {
            ResolvedParticle.allMember(parent, it, schema)
        }

        override fun collectConstraints(collector: MutableList<ResolvedIdentityConstraint>) {
            for (p in mdlParticles) {
                if (p is ResolvedTerm) {
                    p.collectConstraints(collector)
                }
            }
        }

        override fun normalize(
            minMultiplier: VNonNegativeInteger,
            maxMultiplier: VAllNNI
        ): SyntheticAll {
            // there are no minOccurs/maxOccurs
            val newParticles = mutableListOf<ResolvedParticle<ResolvedTerm>>()
            for (particle in this.mdlParticles) {
                val normalized = particle.normalizeTerm(VNonNegativeInteger.ONE, VAllNNI.ONE)
                when (normalized) {
                    is IResolvedAll -> newParticles.addAll(normalized.mdlParticles)
                    else -> newParticles.add(normalized)
                }
            }
            return SyntheticAll(minMultiplier, maxMultiplier, newParticles, schema)
        }

        override fun check(checkedTypes: MutableSet<QName>) {
            super<IResolvedAll>.check(checkedTypes)
        }

        override fun check() {
            check(mutableSetOf())
        }
    }

    private class ChoiceImpl(parent: ResolvedGlobalGroup, override val rawPart: XSGroup.Choice, schema: ResolvedSchemaLike) :
        ModelGroupBase(rawPart, schema),
        IResolvedChoice {

        override val mdlParticles: List<ResolvedParticle<ResolvedTerm>> = rawPart.particles.map {
            ResolvedParticle.choiceSeqMember(parent, it, schema)
        }

        override fun collectConstraints(collector: MutableList<ResolvedIdentityConstraint>) {
            for (p in mdlParticles) {
                if (p is ResolvedTerm) {
                    p.collectConstraints(collector)
                }
            }
        }

        override fun normalize(
            minMultiplier: VNonNegativeInteger,
            maxMultiplier: VAllNNI
        ): SyntheticChoice {
            // there are no minOccurs/maxOccurs
            val newParticles = mutableListOf<ResolvedParticle<ResolvedTerm>>()
            for (particle in this.mdlParticles) {
                val normalized = particle.normalizeTerm(VNonNegativeInteger.ONE, VAllNNI.ONE) as ResolvedParticle<ResolvedTerm>
                when (normalized) {
                    is IResolvedChoice -> newParticles.addAll(normalized.mdlParticles)
                    else -> newParticles.add(normalized)
                }
            }
            return SyntheticChoice(minMultiplier, maxMultiplier, newParticles, schema)
        }

        override fun check(checkedTypes: MutableSet<QName>) {
            super<IResolvedChoice>.check(checkedTypes)
        }

        override fun check() {
            check(mutableSetOf())
        }
    }

    private class SequenceImpl(parent: ResolvedGlobalGroup, override val rawPart: XSGroup.Sequence, schema: ResolvedSchemaLike) :
        ModelGroupBase(rawPart, schema),
        IResolvedSequence {
        override val mdlParticles: List<ResolvedParticle<ResolvedTerm>> = rawPart.particles.map {
            ResolvedParticle.choiceSeqMember(parent, it, schema)
        }

        override fun collectConstraints(collector: MutableList<ResolvedIdentityConstraint>) {
            for (p in mdlParticles) {
                if (p is ResolvedTerm) {
                    p.collectConstraints(collector)
                }
            }
        }

        override fun check(checkedTypes: MutableSet<QName>) {
            super<IResolvedSequence>.check(checkedTypes)
        }

        override fun check() {
            check(mutableSetOf())
        }
    }
}

