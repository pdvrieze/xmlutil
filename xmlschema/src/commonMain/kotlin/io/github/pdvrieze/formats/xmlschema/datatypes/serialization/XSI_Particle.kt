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

package io.github.pdvrieze.formats.xmlschema.datatypes.serialization

import io.github.pdvrieze.formats.xmlschema.datatypes.primitiveInstances.VNonNegativeInteger
import io.github.pdvrieze.formats.xmlschema.types.VAllNNI
import kotlinx.serialization.Serializable
import nl.adaptivity.xmlutil.QName

@Serializable
sealed interface XSI_Particle : XSI_Annotated {
    /** Optional, default 1 */
    val minOccurs: VNonNegativeInteger?

    /** Optional, default 1 */
    val maxOccurs: VAllNNI?

    object DUMMY : XSI_Particle {
        override val minOccurs: Nothing? get() = null
        override val maxOccurs: Nothing? get() = null
        override val annotation: Nothing? get() = null
        override val id: Nothing? get() = null
        override val otherAttrs: Map<QName, String> get() = emptyMap()
    }
}

/**
 * Base interface for all terms that can contain particles
 */
@Serializable
sealed interface XSI_Grouplike : XSI_Particle {
    val particles: List<XSI_Particle>

    fun hasChildren(): Boolean =
        particles.isNotEmpty() // TODO filter out maxCount==0
}

/*
 * Base interface for particle that is not a group reference.
 */
@Serializable
sealed interface XSI_NestedParticle : XSI_Particle
