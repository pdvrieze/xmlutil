/*
 * Copyright (c) 2021.
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

import io.github.pdvrieze.formats.xmlschema.XmlSchemaConstants
import io.github.pdvrieze.formats.xmlschema.datatypes.primitiveInstances.VID
import io.github.pdvrieze.formats.xmlschema.types.T_ContentMode
import io.github.pdvrieze.formats.xmlschema.types.XSI_Annotated
import kotlinx.serialization.Serializable
import nl.adaptivity.xmlutil.SerializableQName
import nl.adaptivity.xmlutil.serialization.XmlBefore
import nl.adaptivity.xmlutil.serialization.XmlElement
import nl.adaptivity.xmlutil.serialization.XmlId
import nl.adaptivity.xmlutil.serialization.XmlSerialName

interface XSI_OpenContent: XSI_Annotated {
    val mode: T_ContentMode
    val any: XSAny?
}

@Serializable
@XmlSerialName("openContent", XmlSchemaConstants.XS_NAMESPACE, XmlSchemaConstants.XS_PREFIX)
class XSOpenContent(
    @XmlId
    override val id: VID? = null,
    @XmlElement(false)
    override val mode: T_ContentMode = T_ContentMode.INTERLEAVE,
    override val otherAttrs: Map<SerializableQName, String> = emptyMap(),
    @XmlBefore("*")
    override val annotation: XSAnnotation? = null,
    override val any: XSAny? = null
): XSI_OpenContent
