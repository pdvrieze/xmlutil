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

import io.github.pdvrieze.formats.xmlschema.datatypes.serialization.XSAttrUse
import io.github.pdvrieze.formats.xmlschema.datatypes.serialization.XSLocalAttribute
import io.github.pdvrieze.formats.xmlschema.impl.invariant
import io.github.pdvrieze.formats.xmlschema.types.VFormChoice
import nl.adaptivity.xmlutil.QName

class ResolvedAttributeRef(
    parent: VAttributeScope.Member,
    override val rawPart: XSLocalAttribute,
    schema: ResolvedSchemaLike
) : ResolvedAttribute(rawPart, schema), IResolvedAttributeUse {

    init {
        invariant(rawPart.ref!=null) { "Attribute references must have a value" }
        invariant(rawPart.use != XSAttrUse.PROHIBITED) { "Attribute references cannot have prohibited use" }

        require(rawPart.targetNamespace == null) { "3.2.3(6.1) - Attribute references have no target namespace" }
        require(rawPart.simpleType == null) { "Attribute references cannot provide direct types" }
        require(rawPart.type == null) { "3.2.3(3.2) - Attribute references cannot provide type references" }
        require(rawPart.form == null) { "3.2.3(3.2) - Attribute references cannot specify form" }
    }

    override val mdlScope: VAttributeScope.Local = VAttributeScope.Local(parent)

    override val mdlRequired: Boolean get() = rawPart.use == XSAttrUse.REQUIRED

    override val mdlAttributeDeclaration: ResolvedAttributeDef
        get() = model.mdlAttributeDeclaration

    override val model: Model by lazy { Model(rawPart, schema, this) }

    override fun check(checkedTypes: MutableSet<QName>) {
        super.check(checkedTypes)

        val r = mdlAttributeDeclaration
        val vc = r.mdlValueConstraint
        if (rawPart.fixed != null && vc is ValueConstraint.Fixed) {
            require(rawPart.fixed == vc.value) { "If an attribute reference has a fixed value it must be the same as the original" }
        }
        r.check(checkedTypes)

    }

    protected class Model(rawPart: XSLocalAttribute, schema: ResolvedSchemaLike, context: ResolvedAttributeRef) :
        ResolvedAttribute.Model(context, schema) {

        override val mdlQName: QName = rawPart.ref?.let { schema.attribute(it).mdlQName } ?: run {
            val ns = rawPart.targetNamespace ?: when {
                (rawPart.form ?: schema.attributeFormDefault) == VFormChoice.QUALIFIED ->
                    schema.targetNamespace

                else -> null
            }

            requireNotNull(rawPart.name).toQname(ns)
        }

        val mdlAttributeDeclaration: ResolvedAttributeDef = schema.attribute(
            requireNotNull(rawPart.ref) { "Missing ref for attributeRef" }
        )

        override val mdlTypeDefinition: ResolvedSimpleType get() = mdlAttributeDeclaration.mdlTypeDefinition
    }

}
