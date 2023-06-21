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

import io.github.pdvrieze.formats.xmlschema.datatypes.primitiveInstances.VAnyURI
import io.github.pdvrieze.formats.xmlschema.datatypes.primitiveInstances.VID
import io.github.pdvrieze.formats.xmlschema.datatypes.primitiveInstances.VNCName
import io.github.pdvrieze.formats.xmlschema.datatypes.serialization.XSAnnotation
import io.github.pdvrieze.formats.xmlschema.datatypes.serialization.XSAttrUse
import io.github.pdvrieze.formats.xmlschema.datatypes.serialization.XSLocalAttribute
import io.github.pdvrieze.formats.xmlschema.datatypes.serialization.XSLocalSimpleType
import io.github.pdvrieze.formats.xmlschema.datatypes.serialization.types.T_FormChoice
import io.github.pdvrieze.formats.xmlschema.datatypes.serialization.types.T_LocalAttribute
import nl.adaptivity.xmlutil.QName

class ResolvedLocalAttribute(
    val parent: Parent,
    override val rawPart: XSLocalAttribute,
    schema: ResolvedSchemaLike
) : ResolvedAttribute(schema), ResolvedAttribute.Use, T_LocalAttribute {
    private val referenced: ResolvedAttribute? by lazy {
        rawPart.ref?.let {
            schema.attribute(
                it
            )
        }
    }

    override val annotation: XSAnnotation?
        get() = rawPart.annotation

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

    override val use: XSAttrUse
        get() = rawPart.use ?: referenced?.use ?: XSAttrUse.OPTIONAL

    override val inheritable: Boolean
        get() = rawPart.inheritable ?: referenced?.inheritable ?: false

    override val simpleType: XSLocalSimpleType?
        get() = rawPart.simpleType ?: referenced?.simpleType

    override fun check() {
        super<ResolvedAttribute>.check()
//        if (rawPart.use!=XSAttrUse.PROHIBITED) {
//            check(type!=null) { "Attributes must have a type if their use is not prohibited" }
//        }
        if (rawPart.ref != null) {
            require(referenced != null) { "If an attribute has a ref, it must also be resolvable" }
        } else if (rawPart.name == null) error("Attributes must either have a reference or a name")
    }

    interface Parent
}
