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
import io.github.pdvrieze.formats.xmlschema.datatypes.primitiveInstances.toAnyUri
import io.github.pdvrieze.formats.xmlschema.datatypes.serialization.XSAttrUse
import io.github.pdvrieze.formats.xmlschema.datatypes.serialization.XSLocalAttribute
import io.github.pdvrieze.formats.xmlschema.impl.invariant
import io.github.pdvrieze.formats.xmlschema.impl.invariantNotNull
import io.github.pdvrieze.formats.xmlschema.resolved.checking.CheckHelper
import io.github.pdvrieze.formats.xmlschema.types.VDerivationControl
import io.github.pdvrieze.formats.xmlschema.types.VFormChoice
import nl.adaptivity.xmlutil.QName

class ResolvedLocalAttribute private constructor(
    parent: VAttributeScope.Member,
    elem: SchemaElement<XSLocalAttribute>,
    localAttributeFormDefault: VFormChoice,
    unresolvedSchema: ResolvedSchemaLike
) : ResolvedAttributeDef(elem, elem.effectiveSchema(unresolvedSchema)), IResolvedAttributeUse {

    override val mdlQName: QName

    init {
        val rawPart = elem.elem
        val schema = elem.effectiveSchema(unresolvedSchema)
        invariant(rawPart.ref == null)
        invariant(rawPart.use != XSAttrUse.PROHIBITED) { "Prohibited attributes are not attributes proper" }

        require(rawPart.targetNamespace == null || rawPart.form == null) { "3.2.3(6.2) - When an attribute has a target namespace it may not have a form" }

        if (rawPart.targetNamespace != null && schema.targetNamespace != rawPart.targetNamespace) {
//            error("XXX. Canary. Remove once verified")
            check(parent is ResolvedComplexType) { "3.2.3(6.3.1) - Attribute with non-matching namespace must have complex type ancestor"}
            check(parent.mdlDerivationMethod == VDerivationControl.RESTRICTION)
            val contentType = parent.mdlContentType
            check(contentType is ResolvedComplexType.ElementContentType) { "content type not elementContent, but ${contentType::class}" }
            check(parent.mdlBaseTypeDefinition != AnyType) { "3.2.3(6.3.2) - Restriction isn't anytype" }
        }

        mdlQName = invariantNotNull(rawPart.name).toQname(
            rawPart.targetNamespace ?: when {
                (rawPart.form ?: schema.attributeFormDefault) == VFormChoice.QUALIFIED ->
                    elem.targetNamespace.toAnyUri()

                else -> null
            }
        )
    }


    override val model: Model by lazy { Model(elem.elem, elem.effectiveSchema(unresolvedSchema), this) }

    override val mdlScope: VAttributeScope.Local = VAttributeScope.Local(parent)

    override val mdlRequired: Boolean = elem.elem.use == XSAttrUse.REQUIRED

    override val mdlAttributeDeclaration: ResolvedLocalAttribute get() = this

    override fun checkUse(checkHelper: CheckHelper) {
        // inline, doesn't need checkHelper
        checkAttribute(checkHelper)
    }

    override fun toString(): String {
        return buildString {
            append("ResolvedLocalAttribute(name = ")
            append(mdlQName)
            append(", type =")
            when (val t = model.mdlTypeDefinition) {
                is ResolvedGlobalSimpleType -> append(t.mdlQName)
                else -> append(t)
            }
            model.mdlValueConstraint?.let {
                append(", valueConstraint = $it")
            }
            append(")")
        }
    }

    class Model(rawPart: XSLocalAttribute, schema: ResolvedSchemaLike, typeContext: VSimpleTypeScope.Member) :
        ResolvedAttributeDef.Model(rawPart, schema, typeContext) {



    }

    companion object {
        internal operator fun invoke(
            parent: VAttributeScope.Member,
            elem: SchemaElement<XSLocalAttribute>,
            schema: ResolvedSchemaLike,
            localAttributeFormDefault: VFormChoice
        ): IResolvedAttributeUse {
            val rawPart = elem.elem
            return when (rawPart.use) {
                XSAttrUse.PROHIBITED -> ResolvedProhibitedAttribute(rawPart, schema)
                else -> when (rawPart.ref) {
                    null -> ResolvedLocalAttribute(parent, elem, localAttributeFormDefault, schema)
                    else -> ResolvedAttributeRef(parent, rawPart, schema)
                }
            }
        }
    }

}

