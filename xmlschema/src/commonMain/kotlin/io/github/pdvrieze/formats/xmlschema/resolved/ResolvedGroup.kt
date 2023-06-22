/*
 * Copyright (c) 2022.
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
import io.github.pdvrieze.formats.xmlschema.datatypes.primitiveInstances.VID
import io.github.pdvrieze.formats.xmlschema.datatypes.primitiveInstances.VNCName
import io.github.pdvrieze.formats.xmlschema.datatypes.primitiveInstances.VNonNegativeInteger
import io.github.pdvrieze.formats.xmlschema.datatypes.serialization.XSAnnotation
import io.github.pdvrieze.formats.xmlschema.datatypes.serialization.XSGroup
import io.github.pdvrieze.formats.xmlschema.datatypes.serialization.XSGroupRef
import io.github.pdvrieze.formats.xmlschema.types.*
import nl.adaptivity.xmlutil.QName

sealed class ResolvedGroup(override val schema: ResolvedSchemaLike): T_Group, ResolvedPart {
    abstract override val rawPart: T_Group
    final override val id: VID?
        get() = rawPart.id
    final override val otherAttrs: Map<QName, String>
        get() = rawPart.otherAttrs
}



class ResolvedGroupRef(
    override val rawPart: XSGroupRef,
    schema: ResolvedSchemaLike
): ResolvedGroup(schema), ResolvedParticle, T_GroupRef {
    val referencedGroup: ResolvedToplevelGroup by lazy { schema.modelGroup(rawPart.ref) }

    override val ref: QName get() = rawPart.ref

    override val annotation: XSAnnotation?
        get() = referencedGroup.annotation

    override val particles: List<T_RealGroup.RG_Particle>
        get() = referencedGroup.particles

    override fun check() {
        referencedGroup.check()
    }
}

class ResolvedToplevelGroup(
    override val rawPart: XSGroup,
    schema: ResolvedSchemaLike
): ResolvedGroup(schema), NamedPart, T_NamedGroup {
    override fun check() {
//        TODO("not implemented")
    }

    override val annotation: XSAnnotation?
        get() = rawPart.annotation

    override val particle: T_NamedGroup.NG_Particle
        get() = rawPart.particle

    override val name: VNCName
        get() = rawPart.name

    override val targetNamespace: VAnyURI?
        get() = schema.targetNamespace
}
