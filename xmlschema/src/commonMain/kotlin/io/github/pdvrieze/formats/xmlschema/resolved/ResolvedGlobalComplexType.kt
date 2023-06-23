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
import io.github.pdvrieze.formats.xmlschema.model.AttributeModel
import io.github.pdvrieze.formats.xmlschema.model.ComplexTypeModel
import io.github.pdvrieze.formats.xmlschema.model.TypeModel
import io.github.pdvrieze.formats.xmlschema.model.WildcardModel
import io.github.pdvrieze.formats.xmlschema.types.*
import nl.adaptivity.xmlutil.QName

class ResolvedGlobalComplexType(
    override val rawPart: XSGlobalComplexType,
    schema: ResolvedSchemaLike
) : ResolvedGlobalType, ResolvedComplexType(schema), T_GlobalComplexType_Base, ComplexTypeModel.Global {
    override val name: VNCName
        get() = rawPart.name

    override val annotation: XSAnnotation?
        get() = rawPart.annotation

    override val id: VID?
        get() = rawPart.id

    override val otherAttrs: Map<QName, String>
        get() = rawPart.otherAttrs

    override val targetNamespace: VAnyURI?
        get() = schema.targetNamespace

    override val mixed: Boolean?
        get() = rawPart.mixed

    override val defaultAttributesApply: Boolean?
        get() = rawPart.defaultAttributesApply

    override val content: ResolvedComplexTypeContent
        by lazy {
            when (val c = rawPart.content) {
                is XSComplexContent -> ResolvedComplexContent(this, c, schema)
                is IXSComplexTypeShorthand -> ResolvedComplexShorthandContent(this, c, schema)
                is XSSimpleContent -> ResolvedSimpleContent(this, c, schema)
                else -> error("unsupported content")
            }
        }

    override val abstract: Boolean get() = model.mdlAbstract

    override val final: T_DerivationSet get() = model.mdlFinal

    override val block: T_DerivationSet get() = model.mdlProhibitedSubstitutions

    override fun check(seenTypes: SingleLinkedList<QName>, inheritedTypes: SingleLinkedList<QName>) {
        content.check(seenTypes + qName, inheritedTypes + qName)
    }

    override val model: ComplexTypeModel.Global by lazy { ModelImpl(rawPart, schema) }

    override val mdlName: VNCName get() = model.mdlName

    override val mdlTargetNamespace: VAnyURI? get() = model.mdlTargetNamespace

    protected class ModelImpl(rawPart: XSGlobalComplexType, schema: ResolvedSchemaLike) :
        ResolvedComplexType.ModelImpl(rawPart, schema), ComplexTypeModel.Global {
        override val mdlName: VNCName = rawPart.name
        override val mdlTargetNamespace: VAnyURI? = rawPart.targetNamespace?:schema.targetNamespace

        override val mdlAbstract: Boolean = rawPart.abstract ?: false

        override val mdlProhibitedSubstitutions: T_DerivationSet = rawPart.block ?: schema.blockDefault.toDerivationSet()

        override val mdlFinal: T_DerivationSet = rawPart.final ?: schema.finalDefault.toDerivationSet()

        override val mdlContentType: ComplexTypeModel.ContentType
            get() = TODO("not implemented")
        override val mdlAttributeUses: Set<AttributeModel.Use>
            get() = TODO("not implemented")
        override val mdlAttributeWildcard: WildcardModel
            get() = TODO("not implemented")
        override val mdlBaseTypeDefinition: TypeModel
            get() = TODO("not implemented")
        override val mdlDerivationMethod: ComplexTypeModel.DerivationMethod
            get() = TODO("not implemented")

    }
}
