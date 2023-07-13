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
import io.github.pdvrieze.formats.xmlschema.datatypes.primitiveInstances.VID
import io.github.pdvrieze.formats.xmlschema.datatypes.serialization.*
import io.github.pdvrieze.formats.xmlschema.model.ComplexTypeModel
import io.github.pdvrieze.formats.xmlschema.types.T_DerivationControl
import io.github.pdvrieze.formats.xmlschema.types.T_LocalComplexType_Base
import io.github.pdvrieze.formats.xmlschema.types.toDerivationSet
import nl.adaptivity.xmlutil.QName

class ResolvedLocalComplexType(
    override val rawPart: XSLocalComplexType,
    schema: ResolvedSchemaLike,
    override val mdlContext: ResolvedComplexTypeContext,
) : ResolvedComplexType(schema), ResolvedLocalType, T_LocalComplexType_Base, ComplexTypeModel.Local {
    override val mixed: Boolean? get() = rawPart.mixed
    override val defaultAttributesApply: Boolean? get() = rawPart.defaultAttributesApply
    override val annotation: XSAnnotation? get() = rawPart.annotation
    override val id: VID? get() = rawPart.id
    override val otherAttrs: Map<QName, String> get() = rawPart.otherAttrs

    override val content: ResolvedComplexTypeContent by lazy {
        when (val c = rawPart.content) {
            is XSComplexContent -> ResolvedComplexContent(this, c, schema)
            is XSComplexType.Shorthand -> ResolvedComplexShorthandContent(
                this,
                c,
                schema
            )
            is XSSimpleContent -> ResolvedSimpleContent(this, c, schema)
            else -> error("unsupported content")
        }
    }

    override val model: Model by lazy {
        when (val raw = rawPart) {
            is XSLocalComplexTypeComplex -> ComplexModelImpl(raw, schema, this, mdlContext)
            is XSLocalComplexTypeShorthand -> ShorthandModelImpl(raw, schema, this, mdlContext)
            is XSLocalComplexTypeSimple -> SimpleModelImpl(raw, schema, this, mdlContext)
            else -> error("XSLocalComplexType should be sealed")
        }
    }

    override fun check(checkedTypes: MutableSet<QName>, inheritedTypes: SingleLinkedList<QName>) {
        super<ResolvedComplexType>.check(checkedTypes, inheritedTypes)
        content.check(checkedTypes, inheritedTypes) // there is no name here
    }

    interface Model : ResolvedComplexType.Model, ComplexTypeModel.Local

    interface SimpleModel : Model, ComplexTypeModel.LocalSimpleContent, ResolvedSimpleContentType {
        override val mdlContentType: ResolvedSimpleContentType
    }

    interface ComplexModel : Model, ComplexTypeModel.LocalComplexContent

    interface ImplicitModel : Model, ComplexTypeModel.LocalImplicitContent

    private class SimpleModelImpl(
        rawPart: XSLocalComplexTypeSimple,
        schema: ResolvedSchemaLike,
        parent: ResolvedComplexType,
        override val mdlContext: ResolvedComplexTypeContext
    ) : SimpleModelBase(parent, rawPart, schema), SimpleModel {
        override val mdlAbstract: Boolean get() = false
        override val mdlProhibitedSubstitutions: Set<out ComplexTypeModel.Derivation> =
            schema.blockDefault.toDerivationSet()
        override val mdlFinal: Set<T_DerivationControl.ComplexBase> =
            schema.finalDefault.toDerivationSet()
    }

    private class ShorthandModelImpl(
        rawPart: XSLocalComplexTypeShorthand,
        schema: ResolvedSchemaLike,
        parent: ResolvedComplexType,
        override val mdlContext: ResolvedComplexTypeContext
    ) : ComplexModelBase(parent, rawPart, schema), ImplicitModel {
        override val mdlAbstract: Boolean get() = false
        override val mdlProhibitedSubstitutions: Set<out ComplexTypeModel.Derivation> =
            schema.blockDefault.toDerivationSet()
        override val mdlFinal: Set<T_DerivationControl.ComplexBase> =
            schema.finalDefault.toDerivationSet()
    }

    private class ComplexModelImpl(
        rawPart: XSLocalComplexTypeComplex,
        schema: ResolvedSchemaLike,
        parent: ResolvedComplexType,
        override val mdlContext: ResolvedComplexTypeContext
    ) : ComplexModelBase(parent, rawPart, schema), ComplexModel {
        override val mdlAbstract: Boolean get() = false
        override val mdlProhibitedSubstitutions: Set<out ComplexTypeModel.Derivation> =
            schema.blockDefault.toDerivationSet()
        override val mdlFinal: Set<T_DerivationControl.ComplexBase> =
            schema.finalDefault.toDerivationSet()
    }
}

