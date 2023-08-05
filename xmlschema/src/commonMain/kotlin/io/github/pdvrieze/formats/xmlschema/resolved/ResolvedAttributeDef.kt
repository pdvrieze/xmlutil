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

import io.github.pdvrieze.formats.xmlschema.XmlSchemaConstants
import io.github.pdvrieze.formats.xmlschema.datatypes.AnySimpleType
import io.github.pdvrieze.formats.xmlschema.datatypes.serialization.XSAttribute
import nl.adaptivity.xmlutil.QName

abstract class ResolvedAttributeDef(rawPart: XSAttribute, schema: ResolvedSchemaLike) :
    ResolvedAttribute(rawPart, schema) {

    abstract override val model: Model

    init {
        val name = requireNotNull(rawPart.name) { "3.2.3(3.1) - Attribute definitions require names" }

        require(name.xmlString != "xmlns") { "3.2.6.3 - Declaring xmlns attributes is forbidden" }
    }

    override fun check(checkedTypes: MutableSet<QName>) {
        super.check(checkedTypes)
        require(mdlQName.getNamespaceURI()!= XmlSchemaConstants.XSI_NAMESPACE) {
            "3.2.6.4 - Attributes may not have the XSI namespace as their target namespace"
        }
    }

    protected abstract class Model(base: ResolvedAttributeDef, schema: ResolvedSchemaLike):
        ResolvedAttribute.Model(base, schema) {

        override val mdlTypeDefinition: ResolvedSimpleType =
            base.rawPart.simpleType?.let {
                require(base.rawPart.type == null) { "3.2.3(4) both simpletype and type attribute present" }
                ResolvedLocalSimpleType(it, base.schema, base)
            } ?: base.rawPart.type?.let { base.schema.simpleType(it) }
            ?: AnySimpleType

    }
}
