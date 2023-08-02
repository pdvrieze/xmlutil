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

import io.github.pdvrieze.formats.xmlschema.datatypes.impl.SingleLinkedList
import io.github.pdvrieze.formats.xmlschema.datatypes.primitiveInstances.VAnyURI
import io.github.pdvrieze.formats.xmlschema.datatypes.primitiveInstances.VID
import io.github.pdvrieze.formats.xmlschema.datatypes.primitiveInstances.VNCName
import io.github.pdvrieze.formats.xmlschema.datatypes.serialization.*
import io.github.pdvrieze.formats.xmlschema.model.SimpleTypeModel
import io.github.pdvrieze.formats.xmlschema.model.TypeModel
import io.github.pdvrieze.formats.xmlschema.types.VDerivationControl
import nl.adaptivity.xmlutil.QName

class ResolvedGlobalSimpleTypeImpl internal constructor(
    override val rawPart: XSGlobalSimpleType,
    override val schema: ResolvedSchemaLike,
    val location: String = "",
) : ResolvedGlobalSimpleType {

    internal constructor(rawPart: SchemaAssociatedElement<XSGlobalSimpleType>, schema: ResolvedSchemaLike) :
            this(rawPart.element, schema, rawPart.schemaLocation)

    override val annotation: XSAnnotation?
        get() = rawPart.annotation

    override val id: VID?
        get() = rawPart.id

    override val otherAttrs: Map<QName, String>
        get() = rawPart.otherAttrs

    override val name: VNCName
        get() = rawPart.name

    override val targetNamespace: VAnyURI?
        get() = schema.targetNamespace

    override val mdlTargetNamespace: VAnyURI? get() = model.mdlTargetNamespace

    override val simpleDerivation: ResolvedSimpleType.Derivation
        get() = when (val raw = rawPart.simpleDerivation) {
            is XSSimpleUnion -> ResolvedUnionDerivation(raw, schema, this)
            is XSSimpleList -> ResolvedListDerivation(raw, schema, this)
            is XSSimpleRestriction -> ResolvedSimpleRestriction(raw, schema, this)
            else -> error("unsupported derivation")
        }

    val final: Set<VDerivationControl.Type>
        get() = rawPart.final

    override fun check(checkedTypes: MutableSet<QName>, inheritedTypes: SingleLinkedList<QName>) {
        require(name.isNotEmpty())
        if (checkedTypes.add(qName)) {
            super.check(checkedTypes, inheritedTypes)
            requireNotNull(model)
        }
    }

    override val model: Model by lazy { ModelImpl(rawPart, schema) }

    override val mdlAnnotations: ResolvedAnnotation? get() = model.mdlAnnotations

    interface Model : ResolvedSimpleType.Model, SimpleTypeModel.Global

    private inner class ModelImpl(rawPart: XSGlobalSimpleType, schema: ResolvedSchemaLike) :
        ResolvedSimpleType.ModelBase(rawPart, schema, this@ResolvedGlobalSimpleTypeImpl),
        Model {

        override val mdlName: VNCName = rawPart.name
        override val mdlTargetNamespace: VAnyURI? get() = schema.targetNamespace

        override val mdlFinal: Set<VDerivationControl.Type> =
            rawPart.final

    }
}
