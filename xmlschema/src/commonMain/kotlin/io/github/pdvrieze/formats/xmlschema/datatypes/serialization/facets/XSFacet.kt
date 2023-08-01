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

package io.github.pdvrieze.formats.xmlschema.datatypes.serialization.facets

import io.github.pdvrieze.formats.xmlschema.datatypes.primitiveInstances.VID
import io.github.pdvrieze.formats.xmlschema.datatypes.serialization.XSAnnotation
import io.github.pdvrieze.formats.xmlschema.types.XSI_Annotated
import kotlinx.serialization.Serializable
import nl.adaptivity.xmlutil.SerializableQName
import nl.adaptivity.xmlutil.serialization.XmlId
import nl.adaptivity.xmlutil.serialization.XmlOtherAttributes

@Serializable
sealed class XSFacet : XSI_Annotated {

    abstract val value: Any
    abstract val fixed: Boolean?

    @XmlId
    final override val id: VID?
    final override val annotation: XSAnnotation?
    @XmlOtherAttributes
    final override val otherAttrs: Map<SerializableQName, String>

    constructor(id: VID?, annotation: XSAnnotation?, otherAttrs: Map<SerializableQName, String>) {
        this.id = id
        this.annotation = annotation
        this.otherAttrs = otherAttrs
    }

    @Serializable
    sealed class NotFixed : XSFacet {
        constructor(id: VID?, annotation: XSAnnotation?, otherAttrs: Map<SerializableQName, String>) : super(id, annotation, otherAttrs)

        final override val fixed: Nothing? get() = null
    }

    @Serializable
    sealed class Fixed : XSFacet {
        final override val fixed: Boolean?

        constructor(fixed: Boolean?, id: VID?, annotation: XSAnnotation?, otherAttrs: Map<SerializableQName, String>) : super(id, annotation, otherAttrs) {
            this.fixed = fixed
        }
    }

    @Serializable
    sealed class Numeric : Fixed {
        final override val value: ULong

        constructor(
            value: ULong,
            fixed: Boolean?,
            id: VID?,
            annotation: XSAnnotation?,
            otherAttrs: Map<SerializableQName, String>
        ) : super(fixed, id, annotation, otherAttrs) {
            this.value = value
        }
    }
}

