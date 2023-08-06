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
import io.github.pdvrieze.formats.xmlschema.resolved.checking.CheckHelper

abstract class ResolvedAttributeDef(rawPart: XSAttribute, schema: ResolvedSchemaLike) :
    ResolvedAttribute(rawPart, schema) {

    abstract override val model: Model

    init {
        val name = requireNotNull(rawPart.name) { "3.2.3(3.1) - Attribute definitions require names" }

        require(name.xmlString != "xmlns") { "3.2.6.3 - Declaring xmlns attributes is forbidden" }
    }

    open fun checkAttribute(checkHelper: CheckHelper) {
        checkHelper.checkType(mdlTypeDefinition)
        mdlValueConstraint?.let { mdlTypeDefinition.validate(it.value) }
        require(mdlQName.getNamespaceURI()!= XmlSchemaConstants.XSI_NAMESPACE) {
            "3.2.6.4 - Attributes may not have the XSI namespace as their target namespace"
        }
    }

    abstract class Model : ResolvedAttribute.Model {

        final override val mdlTypeDefinition: ResolvedSimpleType

        constructor(base: ResolvedAttributeDef) : super(base) {
            this.mdlTypeDefinition = base.mdlTypeDefinition
        }

        constructor(rawPart: XSAttribute, schema: ResolvedSchemaLike, typeContext: VSimpleTypeScope.Member) : super(rawPart) {
            this.mdlTypeDefinition = rawPart.simpleType?.let {
                require(rawPart.type == null) { "3.2.3(4) both simpletype and type attribute present" }
                ResolvedLocalSimpleType(it, schema, typeContext)
            } ?: rawPart.type?.let { schema.simpleType(it) }
                    ?: AnySimpleType
        }

    }
}
