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
import io.github.pdvrieze.formats.xmlschema.datatypes.impl.SingleLinkedList
import io.github.pdvrieze.formats.xmlschema.datatypes.primitiveInstances.VID
import io.github.pdvrieze.formats.xmlschema.datatypes.serialization.*
import io.github.pdvrieze.formats.xmlschema.model.*
import io.github.pdvrieze.formats.xmlschema.types.*
import nl.adaptivity.xmlutil.QName

class ResolvedLocalComplexType(
    override val rawPart: XSLocalComplexType,
    schema: ResolvedSchemaLike,
    context: ResolvedElement,
) : ResolvedComplexType(schema), ResolvedLocalType, T_LocalComplexType_Base, ComplexTypeModel.Local {
    override val mixed: Boolean? get() = rawPart.mixed
    override val defaultAttributesApply: Boolean? get() = rawPart.defaultAttributesApply
    override val annotation: XSAnnotation? get() = rawPart.annotation
    override val id: VID? get() = rawPart.id
    override val otherAttrs: Map<QName, String> get() = rawPart.otherAttrs

    override val content: ResolvedComplexTypeContent by lazy {
        when (val c = rawPart.content) {
            is XSComplexContent -> ResolvedComplexContent(this, c, schema)
            is IXSComplexTypeShorthand -> ResolvedComplexShorthandContent(this, c, schema)
            is XSSimpleContent -> ResolvedSimpleContent(this, c, schema)
            else -> error("unsupported content")
        }
    }

    override val model: Model by lazy {
        when (rawPart) {
            is XSLocalComplexTypeComplex -> ComplexModelImpl(rawPart, schema, mdlContext)
            is XSLocalComplexTypeShorthand -> ShorthandModelImpl(rawPart, schema, mdlContext)
            is XSLocalComplexTypeSimple -> SimpleModelImpl(rawPart, schema, mdlContext)
        }
    }

    override val mdlContext: ComplexTypeContext get() = model.mdlContext

    override fun check(seenTypes: SingleLinkedList<QName>, inheritedTypes: SingleLinkedList<QName>) {
        content.check(seenTypes, inheritedTypes) // there is no name here
    }

    interface Model: ResolvedComplexType.Model, ComplexTypeModel.Local

    private abstract class ModelBase(
        rawPart: XSLocalComplexType, schema: ResolvedSchemaLike,
        override val mdlContext: ComplexTypeContext
    ) : ResolvedComplexType.ModelImpl(rawPart, schema), Model {
        override val mdlAbstract: Boolean get() = false
        override val mdlProhibitedSubstitutions: Set<Nothing> get() = emptySet()
        override val mdlFinal: Set<Nothing> get() = emptySet()
        override val mdlAttributeUses: Set<AttributeModel.Use>
            get() = TODO("not implemented")
        override val mdlAttributeWildcard: WildcardModel
            get() = TODO("not implemented")
        override val mdlContentType: ComplexTypeModel.ContentType
            get() = TODO("not implemented")
    }

    private abstract class ComplexModelBase(
        rawPart: XSLocalComplexType,
        schema: ResolvedSchemaLike,
        mdlContext: ComplexTypeContext
    ) : ModelBase(rawPart, schema, mdlContext)

    private class SimpleModelImpl(
        rawPart: XSLocalComplexTypeSimple,
        schema: ResolvedSchemaLike,
        mdlContext: ComplexTypeContext
    ) : ModelBase(rawPart, schema, mdlContext), ComplexTypeModel.LocalSimpleContent {
        override val mdlDerivationMethod: ComplexTypeModel.DerivationMethod
            get() = TODO("not implemented")
        override val mdlBaseTypeDefinition: ResolvedType
            get() = TODO("not implemented")
        override val mdlContentType: ComplexTypeModel.ContentType.Simple
            get() = TODO("not implemented")
    }

    private class ShorthandModelImpl(
        rawPart: XSLocalComplexTypeShorthand,
        schema: ResolvedSchemaLike,
        mdlContext: ComplexTypeContext
    ) : ComplexModelBase(rawPart, schema, mdlContext), ComplexTypeModel.LocalImplicitContent {
        override val mdlDerivationMethod: ComplexTypeModel.DerivationMethod
            get() = super<ComplexTypeModel.LocalImplicitContent>.mdlDerivationMethod
        override val mdlBaseTypeDefinition: AnyType
            get() = super<ComplexTypeModel.LocalImplicitContent>.mdlBaseTypeDefinition
    }

    private class ComplexModelImpl(
        rawPart: XSLocalComplexTypeComplex,
        schema: ResolvedSchemaLike,
        mdlContext: ComplexTypeContext
    ) : ComplexModelBase(rawPart, schema, mdlContext), ComplexTypeModel.LocalComplexContent {
        override val mdlDerivationMethod: ComplexTypeModel.DerivationMethod
            get() = TODO("not implemented")
        override val mdlBaseTypeDefinition: ResolvedType
            get() = TODO("not implemented")
    }
}

