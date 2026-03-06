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

// Needed for serializer plugin (despite alias)
@file:UseSerializers(QNameSerializer::class)

package io.github.pdvrieze.formats.xmlschema.datatypes.serialization

import io.github.pdvrieze.formats.xmlschema.types.VAllNNI
import io.github.pdvrieze.formats.xmlschema.types.rangeTo
import io.github.pdvrieze.xml.schematypes.values.XsdID
import io.github.pdvrieze.xml.schematypes.values.XsdNonNegativeInteger
import kotlinx.serialization.Serializable
import kotlinx.serialization.UseSerializers
import nl.adaptivity.xmlutil.QNameSerializer
import nl.adaptivity.xmlutil.SerializableQName
import nl.adaptivity.xmlutil.XMLConstants.XSD_NS_URI
import nl.adaptivity.xmlutil.XMLConstants.XSD_PREFIX
import nl.adaptivity.xmlutil.serialization.XmlSerialName

@Serializable
@XmlSerialName("all", XSD_NS_URI, XSD_PREFIX)
class XSAll : XSExplicitGroup {
    override val maxOccurs: VAllNNI.Value?
    override val particles: List<XSI_AllParticle>

    constructor(
        particles: List<XSI_AllParticle>,
        minOccurs: XsdNonNegativeInteger? = null,
        maxOccurs: VAllNNI.Value? = null,
        id: XsdID? = null,
        annotation: XSAnnotation? = null,
        otherAttrs: Map<SerializableQName, String> = emptyMap()
    ) : super(minOccurs, id, annotation, otherAttrs) {
        this.maxOccurs = maxOccurs
        this.particles = particles
    }

    override fun toString(): String = buildString {
        append("all(")
        if (minOccurs!=null || maxOccurs!=null) {
            append((minOccurs ?: VAllNNI.ONE)..(maxOccurs ?: VAllNNI.ONE))
        }
        particles.joinTo(this, separator = " & ", postfix = ")")
    }

}

