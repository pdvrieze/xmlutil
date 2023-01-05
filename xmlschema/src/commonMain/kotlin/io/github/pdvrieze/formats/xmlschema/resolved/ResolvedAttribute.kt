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
import io.github.pdvrieze.formats.xmlschema.datatypes.serialization.*
import io.github.pdvrieze.formats.xmlschema.datatypes.serialization.types.T_AttributeBase
import io.github.pdvrieze.formats.xmlschema.datatypes.serialization.types.T_FormChoice
import io.github.pdvrieze.formats.xmlschema.datatypes.serialization.types.T_LocalAttribute
import io.github.pdvrieze.formats.xmlschema.datatypes.serialization.types.T_TopLevelAttribute
import nl.adaptivity.xmlutil.QName

sealed interface ResolvedAttribute: ResolvedPart, T_AttributeBase {
    override val rawPart: XSAttributeBase
}

sealed class ResolvedAttributeBase(
    override val schema: ResolvedSchemaLike
): ResolvedAttribute, T_AttributeBase {

    val resolvedType: ResolvedSimpleType by lazy {
        this.type?.let { schema.simpleType(it) }
            ?: this.simpleType?.let { ResolvedLocalSimpleType(it, schema) }
            ?: error("Missing type")
    }

    open fun check() {
        resolvedType.check()
    }
}


class ResolvedLocalAttribute(
    override val rawPart: XSLocalAttribute,
    schema: ResolvedSchemaLike
) : ResolvedAttributeBase(schema), T_LocalAttribute {
    private val referenced: ResolvedAttribute? by lazy { rawPart.ref?.let { schema.attribute(it) } }

    override val annotations: List<XSAnnotation>
        get() = rawPart.annotations

    override val id: VID?
        get() = rawPart.id

    override val default: String?
        get() = rawPart.default ?: referenced?.default

    override val fixed: String?
        get() = rawPart.fixed ?: referenced?.fixed

    override val form: T_FormChoice?
        get() = rawPart.form ?: referenced?.form

    override val name: VNCName
        get() = rawPart.name ?: referenced?.name ?: error("An attribute requires a name, either direct or referenced")

    override val ref: QName?
        get() = rawPart.ref

    override val targetNamespace: VAnyURI?
        get() = rawPart.targetNamespace ?: schema.targetNamespace

    override val type: QName?
        get() = rawPart.type ?: referenced?.type

    override val use: XSAttrUse?
        get() = rawPart.use ?: referenced?.use

    override val inheritable: Boolean?
        get() = rawPart.inheritable ?: referenced?.inheritable

    override val simpleType: XSLocalSimpleType?
        get() = rawPart.simpleType ?: referenced?.simpleType

    override val otherAttrs: Map<QName, String>
        get() = rawPart.otherAttrs

    override fun check() {
        super.check()
        if (rawPart.ref == null && rawPart.name == null) error("Attributes must either have a reference or a name")
    }
}

class ResolvedToplevelAttribute(
    override val rawPart: XSAttribute,
    schema: ResolvedSchemaLike
) : ResolvedAttributeBase(schema), ResolvedAttribute, T_TopLevelAttribute, NamedPart {

    override val annotations: List<XSAnnotation>
        get() = rawPart.annotations

    override val id: VID?
        get() = rawPart.id

    override val default: String?
        get() = rawPart.default

    override val fixed: String?
        get() = rawPart.fixed

    override val name: VNCName
        get() = rawPart.name

    override val type: QName?
        get() = rawPart.type

    override val inheritable: Boolean?
        get() = rawPart.inheritable

    override val simpleType: XSLocalSimpleType?
        get() = rawPart.simpleType

    override val targetNamespace: Nothing?
        get() = null

    override val otherAttrs: Map<QName, String>
        get() = rawPart.otherAttrs
}
