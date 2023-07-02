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

import io.github.pdvrieze.formats.xmlschema.datatypes.primitiveInstances.VID
import io.github.pdvrieze.formats.xmlschema.datatypes.primitiveInstances.VNonNegativeInteger
import io.github.pdvrieze.formats.xmlschema.types.*
import kotlinx.serialization.Serializable
import nl.adaptivity.xmlutil.QName

interface XSExplicitGroup : XSI_Particle, T_ExplicitGroupParticle {
    override val particles: List<XSI_NestedParticle>

    fun hasChildren(): Boolean =
        particles.isNotEmpty() // TODO filter out maxCount==0
}

@Serializable
sealed interface XSI_Particle : XSI_OpenAttrs, XSI_Annotated, T_Particle {
    object DUMMY: XSI_Particle {
        override val minOccurs: Nothing? get() = null
        override val maxOccurs: Nothing? get() = null
        override val annotation: Nothing? get() = null
        override val id: Nothing? get() = null
        override val otherAttrs: Map<QName, String> get() = emptyMap()
    }
}

@Serializable
sealed interface XSI_AllParticle : XSI_NestedParticle

/*
 * Base interface for particle that is not a group reference.
 */
@Serializable
sealed interface XSI_NestedParticle : XSI_Particle