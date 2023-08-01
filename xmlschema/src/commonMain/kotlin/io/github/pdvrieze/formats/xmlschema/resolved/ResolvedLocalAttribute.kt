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
import io.github.pdvrieze.formats.xmlschema.datatypes.primitiveInstances.VString
import io.github.pdvrieze.formats.xmlschema.datatypes.serialization.XSAttrUse
import io.github.pdvrieze.formats.xmlschema.datatypes.serialization.XSLocalAttribute
import io.github.pdvrieze.formats.xmlschema.model.AttributeModel
import io.github.pdvrieze.formats.xmlschema.model.ValueConstraintModel
import io.github.pdvrieze.formats.xmlschema.types.VFormChoice
import nl.adaptivity.xmlutil.QName

class ResolvedLocalAttribute(
    override val parent: Parent,
    override val rawPart: XSLocalAttribute,
    schema: ResolvedSchemaLike
) : ResolvedAttribute(schema), ResolvedAttributeLocal {
    private val referenced: ResolvedAttribute? by lazy {
        rawPart.ref?.let {
            schema.attribute(
                it
            )
        }
    }

    override val id: VID?
        get() = rawPart.id

    override val default: VString?
        get() = rawPart.default ?: referenced?.default

    override val fixed: VString?
        get() = rawPart.fixed ?: referenced?.fixed

    val form: VFormChoice?
        get() = rawPart.form

    override val name: VNCName
        get() = rawPart.name ?: referenced?.name ?: error("An attribute requires a name, either direct or referenced")

    val ref: QName?
        get() = rawPart.ref

    override val targetNamespace: VAnyURI?
        get() = rawPart.targetNamespace ?: schema.targetNamespace

    override val type: QName?
        get() = rawPart.type ?: referenced?.type

    val use: XSAttrUse
        get() = rawPart.use ?: XSAttrUse.OPTIONAL

    val inheritable: Boolean
        get() = rawPart.inheritable  ?: false
/*

    override val simpleType: XSLocalSimpleType?
        get() = rawPart.simpleType ?: referenced?.simpleType
*/

    override val mdlName: VNCName
        get() = name

    override val mdlTypeDefinition: ResolvedSimpleType by lazy {
        rawPart.simpleType?.let { ResolvedLocalSimpleType(it, schema, this) } ?:
        referenced?.mdlTypeDefinition ?:
        schema.simpleType(requireNotNull(ref) { "Missing simple type for attribute $name" } )
    }

    override val mdlScope: ResolvedScope get() = this

    override val mdlRequired: Boolean
        get() = rawPart.use == XSAttrUse.REQUIRED

    override val mdlAttributeDeclaration: ResolvedAttributeDecl
        get() = when (ref) {
            null -> this
            else -> requireNotNull(referenced)
        }

    override val mdlValueConstraint: ValueConstraintModel?
        get() = TODO("Implement local attribute value constraint")

    override fun check(checkedTypes: MutableSet<QName>) {
        super<ResolvedAttribute>.check(checkedTypes)
//        if (rawPart.use!=XSAttrUse.PROHIBITED) {
//            check(type!=null) { "Attributes must have a type if their use is not prohibited" }
//        }
        if (rawPart.ref != null) {
            val r = referenced
            require(r != null) { "If an attribute has a ref, it must also be resolvable" }
            if (rawPart.fixed!=null && r.fixed!=null) {
                require(rawPart.fixed == r.fixed) { "If an attribute reference has a fixed value it must be the same as the original" }
            }
        } else if (rawPart.name == null) error("Attributes must either have a reference or a name")
    }

    interface Parent : AttributeModel.AttributeParentModel
}
