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

import io.github.pdvrieze.formats.xmlschema.datatypes.primitiveInstances.VID
import io.github.pdvrieze.formats.xmlschema.datatypes.primitiveInstances.VNonNegativeInteger
import io.github.pdvrieze.formats.xmlschema.datatypes.serialization.*
import io.github.pdvrieze.formats.xmlschema.resolved.checking.CheckHelper
import io.github.pdvrieze.formats.xmlschema.types.VAllNNI
import nl.adaptivity.xmlutil.QName

sealed class ResolvedGroupParticleTermBase<T : ResolvedModelGroup>(
    parent: VElementScope.Member,
    elemPart: SchemaElement<XSI_Grouplike>,
    schema: ResolvedSchemaLike,
    final override val mdlMinOccurs: VNonNegativeInteger = elemPart.elem.minOccurs ?: VNonNegativeInteger.ONE,
    final override val mdlMaxOccurs: VAllNNI = elemPart.elem.maxOccurs ?: VAllNNI.ONE,
) : ResolvedGroupParticle<T>, ResolvedTerm {

    init {
        require(mdlMinOccurs<=mdlMaxOccurs) { "Invalid bounds: ! (${mdlMinOccurs}<=$mdlMaxOccurs)" }
    }

    final override val model: Model by lazy {
        Model(parent, elemPart, schema)
    }

    val mdlParticles: List<ResolvedParticle<ResolvedTerm>> get() = model.particles

    override fun flatten(checkHelper: CheckHelper): FlattenedParticle {
        return super<ResolvedGroupParticle>.flatten(checkHelper)
    }

    override fun isSiblingName(name: QName): Boolean {
        return super<ResolvedGroupParticle>.isSiblingName(name)
    }

    internal operator fun invoke(
        parent: ResolvedComplexType,
        elemPart: SchemaElement<XSExplicitGroup>,
        schema: ResolvedSchemaLike
    ): ResolvedGroupParticle<ResolvedModelGroup> = when (elemPart.elem) {
        is XSAll -> ResolvedAll(parent, elemPart.cast(), schema)
        is XSChoice -> ResolvedChoice(parent, elemPart.cast(), schema)
        is XSSequence -> ResolvedSequence(parent, elemPart.cast(), schema)
        else -> error("Found unsupported group: ${elemPart.elem}")
    }

    class Model : ResolvedAnnotated.Model {
        val particles: List<ResolvedParticle<ResolvedTerm>>

        constructor(
            particles: List<ResolvedParticle<ResolvedTerm>>,
            annotations: List<ResolvedAnnotation> = emptyList(),
            id: VID? = null,
            otherAttrs: Map<QName, String> = emptyMap(),
        ) : super(annotations, id, otherAttrs) {
            this.particles = particles
        }

        internal constructor(
            parent: VElementScope.Member,
            elemPart: SchemaElement<XSI_Grouplike>,
            schema: ResolvedSchemaLike,
        ) : super(elemPart.elem) {
            particles = elemPart.wrapEach { particles }.map { ResolvedParticle(parent, it, schema, elemPart.elem.hasLocalNsInContext(schema)) }
        }
    }
}
