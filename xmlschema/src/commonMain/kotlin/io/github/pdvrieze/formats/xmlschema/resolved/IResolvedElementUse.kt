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
import io.github.pdvrieze.formats.xmlschema.datatypes.serialization.XSI_Annotated
import io.github.pdvrieze.formats.xmlschema.datatypes.serialization.XSLocalElement
import nl.adaptivity.xmlutil.QName

sealed interface IResolvedElementUse : ResolvedAnnotated, ResolvedParticle<ResolvedElement> {

    /** Name of the used element */
    val mdlQName: QName

    companion object {
        operator fun invoke(
            parent: VElementScope.Member,
            rawPart: XSLocalElement,
            schema: ResolvedSchemaLike,
        ): IResolvedElementUse = when {
            rawPart.minOccurs == VNonNegativeInteger.ZERO &&
                    rawPart.maxOccurs == VNonNegativeInteger.ZERO ->
                ResolvedProhibitedElement(rawPart, schema)

            rawPart.ref == null -> ResolvedLocalElement(parent, rawPart, schema)
            else -> ResolvedElementRef(rawPart, schema)
        }
    }
}

