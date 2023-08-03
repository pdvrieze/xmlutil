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

import io.github.pdvrieze.formats.xmlschema.datatypes.AnyType
import io.github.pdvrieze.formats.xmlschema.datatypes.primitiveInstances.VAnyURI
import io.github.pdvrieze.formats.xmlschema.datatypes.serialization.XSAttrUse
import io.github.pdvrieze.formats.xmlschema.datatypes.serialization.XSLocalAttribute
import io.github.pdvrieze.formats.xmlschema.types.VFormChoice
import nl.adaptivity.xmlutil.QName

class ResolvedLocalAttribute private constructor(
    parent: VAttributeScope.Member,
    override val rawPart: XSLocalAttribute,
    schema: ResolvedSchemaLike,
    dummy: Boolean
) : ResolvedAttributeDef(rawPart, schema), IResolvedAttributeUse {

    init {
        require(rawPart.ref == null)
        require(rawPart.use != XSAttrUse.PROHIBITED) { "Prohibited attributes are not attributes proper" }
        require(rawPart.targetNamespace == null || rawPart.form == null) { "When an attribute has a target namespace it may not have a form" }

        if (rawPart.targetNamespace != null && schema.targetNamespace != rawPart.targetNamespace) {
            error("XXX. Canary. Remove once verified")
            check(parent is ResolvedComplexType) { "3.2.3 - 6.3.1: Attribute with non-matchin namespace must have complex type ancestor"}
            val content = parent.content
            check(content is ResolvedComplexContent)
            val derivation = content.derivation
            check(derivation is ResolvedComplexRestriction)
            check(derivation.base != AnyType.mdlQName) { "3.2.3 - 6.3.2 restriction isn't anytype"}
        }
    }

    override val targetNamespace: VAnyURI?
        get() = rawPart.targetNamespace ?: schema.targetNamespace

    val use: XSAttrUse
        get() = rawPart.use ?: XSAttrUse.OPTIONAL

    val inheritable: Boolean
        get() = rawPart.inheritable ?: false

    override val mdlTargetNamespace: VAnyURI? by lazy {
        targetNamespace ?: when {
            (rawPart.form ?: schema.attributeFormDefault) == VFormChoice.QUALIFIED ->
                schema.targetNamespace

            else -> null
        }
    }

    override val mdlScope: VAttributeScope.Local = VAttributeScope.Local(parent)

    val parent: VAttributeScope.Member get() = mdlScope.parent

    override val mdlRequired: Boolean
        get() = rawPart.use == XSAttrUse.REQUIRED

    override val mdlAttributeDeclaration: ResolvedLocalAttribute get() = this

    companion object {
        operator fun invoke(
            parent: VAttributeScope.Member,
            rawPart: XSLocalAttribute,
            schema: ResolvedSchemaLike
        ): IResolvedAttributeUse {
            return when (rawPart.use) {
                XSAttrUse.PROHIBITED -> ResolvedProhibitedAttribute(parent, rawPart, schema)
                else -> when (rawPart.ref) {
                    null -> ResolvedLocalAttribute(parent, rawPart, schema, false)
                    else -> ResolvedAttributeRef(parent, rawPart, schema)
                }
            }
        }
    }

}

