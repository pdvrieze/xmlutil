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
import io.github.pdvrieze.formats.xmlschema.datatypes.primitiveInstances.VAnyURI
import io.github.pdvrieze.formats.xmlschema.datatypes.primitiveInstances.VNCName
import io.github.pdvrieze.formats.xmlschema.datatypes.primitiveInstances.VString
import io.github.pdvrieze.formats.xmlschema.datatypes.serialization.XSGlobalAttribute
import io.github.pdvrieze.formats.xmlschema.datatypes.serialization.XSLocalSimpleType
import io.github.pdvrieze.formats.xmlschema.types.VFormChoice
import nl.adaptivity.xmlutil.QName
import nl.adaptivity.xmlutil.qname

class ResolvedGlobalAttribute(
    override val rawPart: XSGlobalAttribute,
    schema: ResolvedSchemaLike,
    val location: String,
) : ResolvedAttributeDef(rawPart, schema), IScope.Global, NamedPart {

    internal constructor(rawPart: SchemaAssociatedElement<XSGlobalAttribute>, schema: ResolvedSchemaLike) :
            this(rawPart.element, schema, rawPart.schemaLocation)

    override val mdlTargetNamespace: VAnyURI? by lazy {
        rawPart.targetNamespace ?: when(schema.attributeFormDefault) {
            VFormChoice.QUALIFIED -> schema.targetNamespace

            else -> null
        }
    }

    override val targetNamespace: VAnyURI?
        get() = schema.targetNamespace

    override val otherAttrs: Map<QName, String>
        get() = rawPart.otherAttrs

    override val mdlValueConstraint: Nothing? get() = null

    override val mdlScope: VAttributeScope.Global get() = VAttributeScope.Global

    override fun check(checkedTypes: MutableSet<QName>) {
        super<ResolvedAttributeDef>.check(checkedTypes)
    }
}

