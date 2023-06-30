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
import io.github.pdvrieze.formats.xmlschema.datatypes.serialization.XSGlobalAttribute
import io.github.pdvrieze.formats.xmlschema.datatypes.serialization.XSLocalSimpleType
import io.github.pdvrieze.formats.xmlschema.model.ValueConstraintModel
import io.github.pdvrieze.formats.xmlschema.types.T_GlobalAttribute
import nl.adaptivity.xmlutil.QName

class ResolvedGlobalAttribute(
    override val rawPart: XSGlobalAttribute,
    schema: ResolvedSchemaLike
) : ResolvedAttribute(schema),
    T_GlobalAttribute, NamedPart, ResolvedAttributeGlobal {

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

    override val targetNamespace: VAnyURI?
        get() = schema.targetNamespace

    override val otherAttrs: Map<QName, String>
        get() = rawPart.otherAttrs

    override val mdlName: VNCName get() = name

    override val mdlTypeDefinition: ResolvedSimpleType =
        rawPart.simpleType?.let { ResolvedLocalSimpleType(it, schema, this) } ?: schema.simpleType(requireNotNull(rawPart.type) { "missing type for attribute $mdlQName" } )

    override val mdlValueConstraint: Nothing? get() = null

    override val mdlScope: ResolvedScope get() = this

    override fun check() {
        super<ResolvedAttribute>.check()
    }
}
