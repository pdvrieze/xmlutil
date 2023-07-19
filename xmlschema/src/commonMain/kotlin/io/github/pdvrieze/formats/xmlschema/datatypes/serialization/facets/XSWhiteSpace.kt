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

import io.github.pdvrieze.formats.xmlschema.XmlSchemaConstants
import io.github.pdvrieze.formats.xmlschema.datatypes.primitiveInstances.VID
import io.github.pdvrieze.formats.xmlschema.datatypes.primitiveInstances.VString
import io.github.pdvrieze.formats.xmlschema.datatypes.serialization.XSAnnotation
import io.github.pdvrieze.formats.xmlschema.types.T_Facet
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import nl.adaptivity.xmlutil.QName
import nl.adaptivity.xmlutil.QNameSerializer
import nl.adaptivity.xmlutil.serialization.XmlElement
import nl.adaptivity.xmlutil.serialization.XmlOtherAttributes
import nl.adaptivity.xmlutil.serialization.XmlSerialName
import nl.adaptivity.xmlutil.xmlCollapseWhitespace

@Serializable
@XmlSerialName("whiteSpace", XmlSchemaConstants.XS_NAMESPACE, XmlSchemaConstants.XS_PREFIX)
class XSWhiteSpace(
    @XmlElement(false)
    override val value: Values,
    override val fixed: Boolean? = null,
    override val id: VID? = null,
    override val annotation: XSAnnotation? = null,

    @XmlOtherAttributes
    override val otherAttrs: Map<@Serializable(QNameSerializer::class) QName, String> = emptyMap()
) : XSFacet(), T_Facet {


    @Serializable
    enum class Values {
        @SerialName("preserve")
        PRESERVE {
            override fun normalize(representation: VString): VString = representation
        },

        @SerialName("replace")
        REPLACE {
            override fun normalize(representation: VString): VString = buildVString(representation.length) {
                for(c in representation) {
                    when (c) {
                        '\t', '\n', '\r' -> append(' ')
                        else -> append(c)
                    }
                }
            }
        },

        @SerialName("collapse")
        COLLAPSE {
            override fun normalize(representation: VString): VString =
                VString(xmlCollapseWhitespace(representation.xmlString))
        };

        abstract fun normalize(representation: VString): VString
    }

}

internal inline fun buildVString(builderAction: StringBuilder.() -> Unit): VString {
    return VString(buildString(builderAction))
}

internal inline fun buildVString(capacity: Int, builderAction: StringBuilder.() -> Unit): VString {
    return VString(buildString(capacity, builderAction))
}
