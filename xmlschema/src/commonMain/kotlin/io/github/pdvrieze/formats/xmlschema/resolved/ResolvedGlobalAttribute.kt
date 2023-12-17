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

import io.github.pdvrieze.formats.xmlschema.datatypes.serialization.XSGlobalAttribute
import nl.adaptivity.xmlutil.QName
import nl.adaptivity.xmlutil.XMLConstants
import nl.adaptivity.xmlutil.namespaceURI

class ResolvedGlobalAttribute internal constructor(
    rawPart: XSGlobalAttribute,
    schema: ResolvedSchemaLike,
    val location: String,
    val builtin: Boolean,
) : ResolvedAttributeDef(rawPart), IScope.Global, NamedPart {

    constructor(
        rawPart: XSGlobalAttribute,
        schema: ResolvedSchemaLike,
        location: String
    ) : this(rawPart, schema, location, false)

    internal constructor(elem: SchemaElement<XSGlobalAttribute>, schema: ResolvedSchemaLike) :
            this(elem.elem, elem.effectiveSchema(schema), elem.schemaLocation, elem.builtin)

    override val model: Model by lazy { Model(rawPart, schema, this) }

    override val mdlScope: VAttributeScope.Global get() = VAttributeScope.Global

    override val mdlQName: QName = rawPart.name.toQname(schema.targetNamespace)

    init {
        require(schema.targetNamespace?.value == XMLConstants.XSI_NS_URI ||
                mdlQName.namespaceURI != XMLConstants.XSI_NS_URI) {
            "Attributes can not be declared into the XSI namespace"
        }
    }

    class Model : ResolvedAttributeDef.Model {

        constructor(
            rawPart: XSGlobalAttribute,
            schema: ResolvedSchemaLike,
            typeContext: VSimpleTypeScope.Member
        ) : super(rawPart, schema, typeContext)

    }
}

