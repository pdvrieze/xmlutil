/*
 * Copyright (c) 2023-2026.
 *
 * This file is part of xmlutil.
 *
 * This file is licenced to you under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance
 * with the License.  You should have  received a copy of the license
 * with the source distribution. Alternatively, you may obtain a copy
 * of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or
 * implied.  See the License for the specific language governing
 * permissions and limitations under the License.
 */

package io.github.pdvrieze.formats.xmlschema.resolved

import io.github.pdvrieze.formats.xmlschema.datatypes.serialization.XSLocalElement
import io.github.pdvrieze.xml.schematypes.values.XsdNonNegativeInteger
import io.github.pdvrieze.xml.schematypes.values.XsdQName

sealed interface IResolvedElementUse : ResolvedAnnotated, ResolvedParticle<ResolvedElement> {

    /** Name of the used element */
    val mdlQName: XsdQName

    companion object {
        internal operator fun invoke(
            parent: VElementScope.Member,
            elemPart: SchemaElement<XSLocalElement>,
            schema: ResolvedSchemaLike,
        ): IResolvedElementUse = when {
            elemPart.elem.minOccurs == XsdNonNegativeInteger.ZERO &&
                    elemPart.elem.maxOccurs == XsdNonNegativeInteger.ZERO ->
                ResolvedProhibitedElement(elemPart.elem, schema)

            elemPart.elem.ref == null -> ResolvedLocalElement(parent, elemPart, schema)
            else -> ResolvedElementRef(elemPart.elem, schema)
        }
    }
}

