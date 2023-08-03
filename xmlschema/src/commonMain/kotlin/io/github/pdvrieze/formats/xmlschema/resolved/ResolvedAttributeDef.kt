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

import io.github.pdvrieze.formats.xmlschema.datatypes.AnySimpleType
import io.github.pdvrieze.formats.xmlschema.datatypes.primitiveInstances.VNCName
import io.github.pdvrieze.formats.xmlschema.datatypes.primitiveInstances.VString
import io.github.pdvrieze.formats.xmlschema.datatypes.serialization.XSAttrUse
import io.github.pdvrieze.formats.xmlschema.datatypes.serialization.XSAttribute
import nl.adaptivity.xmlutil.QName

abstract class ResolvedAttributeDef(rawPart: XSAttribute, schema: ResolvedSchemaLike) : ResolvedAttribute(schema),
    NamedPart {
    final override val name: VNCName get() = mdlName

    final override val default: VString? get() = rawPart.default

    final override val fixed: VString? get() = rawPart.fixed

    final override val type: QName? get() = rawPart.type

    final override val mdlName: VNCName

    final override val mdlTypeDefinition: ResolvedSimpleType get() = model.mdlTypeDefinition



    private val model: Model by lazy { Model(this) }

    private class Model(base: ResolvedAttributeDef) {
        val mdlTypeDefinition: ResolvedSimpleType =
            base.rawPart.simpleType?.let { ResolvedLocalSimpleType(it, base.schema, base) }
                ?: base.rawPart.type?.let { base.schema.simpleType(it) }
                ?: AnySimpleType
    }

    init {
        mdlName = requireNotNull(rawPart.name) { "Attribute definitions require names" }

        require(mdlName.xmlString != "xmlns") { "Declaring xmlns attributes is forbidden" }
    }
}
