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
import io.github.pdvrieze.formats.xmlschema.datatypes.serialization.XSI_Annotated
import io.github.pdvrieze.formats.xmlschema.resolved.facets.FacetList
import nl.adaptivity.xmlutil.QName

interface ResolvedAnnotated {
    @Deprecated("Not needed")
    val rawPart: XSI_Annotated

    @Deprecated("Not needed")
    val schema: ResolvedSchemaLike

    val model: IModel

    val mdlAnnotations: List<ResolvedAnnotation> get() = model.annotations
    val id: VID? get() = model.id
    val otherAttrs: Map<QName, String> get() = model.otherAttrs

    interface IModel {
        val annotations: List<ResolvedAnnotation>
        val id: VID?
        val otherAttrs: Map<QName, String>
    }

    object Empty: EmptyModel()

    open class EmptyModel : IModel {
        override val annotations: List<Nothing> get() = emptyList()
        override val id: Nothing? get() = null
        override val otherAttrs: Map<QName, Nothing> get() = emptyMap()
    }

    open class Model(
        final override val annotations: List<ResolvedAnnotation> = emptyList(),
        final override val id: VID? = null,
        final override val otherAttrs: Map<QName, String> = emptyMap()
    ) : IModel {
        constructor(
            rawPart: XSI_Annotated?,
            annotations: List<ResolvedAnnotation> = listOfNotNull(rawPart?.annotation.models())
        ) : this(
            annotations,
            rawPart?.id,
            rawPart?.resolvedOtherAttrs() ?: emptyMap()
        )
    }
}
