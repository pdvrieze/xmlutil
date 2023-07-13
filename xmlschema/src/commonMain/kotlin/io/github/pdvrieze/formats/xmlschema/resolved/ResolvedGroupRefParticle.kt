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

import io.github.pdvrieze.formats.xmlschema.datatypes.primitiveInstances.VNonNegativeInteger
import io.github.pdvrieze.formats.xmlschema.datatypes.serialization.XSAnnotation
import io.github.pdvrieze.formats.xmlschema.datatypes.serialization.XSGroupRefParticle
import io.github.pdvrieze.formats.xmlschema.model.AnnotationModel
import io.github.pdvrieze.formats.xmlschema.resolved.particles.ResolvedParticle
import io.github.pdvrieze.formats.xmlschema.types.T_AllNNI
import io.github.pdvrieze.formats.xmlschema.types.T_GroupRef
import nl.adaptivity.xmlutil.QName

/**
 * Type representing a particle group reference
 */
class ResolvedGroupRefParticle(
    override val rawPart: XSGroupRefParticle,
    override val schema: ResolvedSchemaLike,
) : ResolvedGroupBase,
    ResolvedParticle<ResolvedGlobalGroup>,
    T_GroupRef {

    override val ref: QName get() = rawPart.ref

    override val annotation: XSAnnotation?
        get() = mdlTerm.annotation

    override val minOccurs: VNonNegativeInteger?
        get() = rawPart.minOccurs

    override val mdlMinOccurs: VNonNegativeInteger
        get() = rawPart.minOccurs ?: VNonNegativeInteger.ONE

    override val mdlMaxOccurs: T_AllNNI
        get() = rawPart.maxOccurs ?: T_AllNNI.ONE

    override val maxOccurs: T_AllNNI?
        get() = rawPart.maxOccurs

    override val mdlAnnotations: AnnotationModel? get() = rawPart.annotation.models()

    override val mdlTerm: ResolvedGlobalGroup by lazy { schema.modelGroup(rawPart.ref) }

    override fun check() {
        mdlTerm.check()
    }

    fun collectConstraints(collector: MutableList<ResolvedIdentityConstraint>) {
        mdlTerm.collectConstraints(collector)
    }
}
