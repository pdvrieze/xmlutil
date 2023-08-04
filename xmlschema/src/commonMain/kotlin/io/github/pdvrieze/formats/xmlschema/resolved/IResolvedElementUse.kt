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
import io.github.pdvrieze.formats.xmlschema.datatypes.serialization.XSLocalElement
import io.github.pdvrieze.formats.xmlschema.resolved.particles.ResolvedParticle
import io.github.pdvrieze.formats.xmlschema.types.VAllNNI

sealed interface IResolvedElementUse: ResolvedAnnotated, ResolvedParticle<ResolvedElement> {

    val mdlElementDeclaration: ResolvedElement

    companion object {
        operator fun invoke(
            parent: VElementScope.Member,
            rawPart: XSLocalElement,
            schema: ResolvedSchemaLike,
            minOccurs: VNonNegativeInteger? = rawPart.minOccurs,
            maxOccurs: VAllNNI? = rawPart.maxOccurs,
        ): IResolvedElementUse = when (rawPart.ref) {
            null -> ResolvedLocalElement(parent, rawPart, schema, minOccurs, maxOccurs)
            else -> ResolvedElementRef(parent, rawPart, schema, minOccurs, maxOccurs)
        }
    }
}

