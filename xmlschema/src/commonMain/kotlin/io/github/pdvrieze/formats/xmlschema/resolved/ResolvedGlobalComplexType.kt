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
import io.github.pdvrieze.formats.xmlschema.model.ComplexTypeModel
import io.github.pdvrieze.formats.xmlschema.types.*
import nl.adaptivity.xmlutil.QName

class ResolvedGlobalComplexType(
    override val rawPart: XSGlobalComplexType,
    schema: ResolvedSchemaLike,
    val location: String
) : ResolvedGlobalType, ResolvedComplexType(schema), ComplexTypeModel.Global {

    internal constructor(rawPart: SchemaAssociatedElement<XSGlobalComplexType>, schema: ResolvedSchemaLike) :
            this(rawPart.element, schema, rawPart.schemaLocation)

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

    val mixed: Boolean?
        get() = rawPart.mixed

    val defaultAttributesApply: Boolean?
        get() = rawPart.defaultAttributesApply

    override val content: ResolvedComplexTypeContent
            by lazy {
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

    val abstract: Boolean get() = model.mdlAbstract
    val final: Set<ComplexTypeModel.Derivation> get() = model.mdlFinal
    val block: Set<ComplexTypeModel.Derivation> get() = model.mdlProhibitedSubstitutions

    override val model: Model by lazy(LazyThreadSafetyMode.SYNCHRONIZED) {
        when (val r = rawPart) {
            is XSGlobalComplexTypeComplex -> ComplexModelImpl(this, r, schema)
            is XSGlobalComplexTypeShorthand -> ShorthandModelImpl(this, r, schema)
            is XSGlobalComplexTypeSimple -> SimpleModelImpl(this, r, schema)
        }

    }

    override val mdlName: VNCName get() = model.mdlName

    override val mdlTargetNamespace: VAnyURI? get() = model.mdlTargetNamespace

    override fun check(checkedTypes: MutableSet<QName>, inheritedTypes: SingleLinkedList<QName>) {
        if (checkedTypes.add(qName)) {
            super<ResolvedComplexType>.check(checkedTypes, inheritedTypes)
            mdlContentType.check()
            content.check(checkedTypes, inheritedTypes + qName)
        }
    }

    override fun toString(): String {
        return "ComplexType{ name=${name}, base=${mdlBaseTypeDefinition} }"
    }

    interface Model : ResolvedComplexType.Model, ComplexTypeModel.Global {
    }

    interface SimpleModel : Model, ComplexTypeModel.GlobalSimpleContent, ResolvedSimpleContentType {
        override val mdlContentType: ResolvedSimpleContentType
    }

    interface ComplexModel : Model, ComplexTypeModel.GlobalComplexContent {

    }

    interface ImplicitModel : Model, ComplexTypeModel.GlobalImplicitContent {

    }


    private class SimpleModelImpl(
        parent: ResolvedComplexType,
        rawPart: XSGlobalComplexTypeSimple,
        schema: ResolvedSchemaLike,
    ) : SimpleModelBase(parent, rawPart, schema), SimpleModel {
        override val mdlName: VNCName = rawPart.name
        override val mdlAbstract: Boolean = rawPart.abstract ?: false
        override val mdlTargetNamespace: VAnyURI? = schema.targetNamespace
        override val mdlProhibitedSubstitutions: Set<ComplexTypeModel.Derivation> =
            calcProhibitedSubstitutions(rawPart, schema)
        override val mdlFinal: Set<T_DerivationControl.ComplexBase> =
            calcFinalSubstitutions(rawPart, schema)

    }

    private class ShorthandModelImpl(
        parent: ResolvedComplexType,
        rawPart: XSGlobalComplexTypeShorthand,
        schema: ResolvedSchemaLike,
    ) : ComplexModelBase(parent, rawPart, schema), ImplicitModel {
        override val mdlName: VNCName = rawPart.name
        override val mdlAbstract: Boolean = rawPart.abstract ?: false
        override val mdlTargetNamespace: VAnyURI? = schema.targetNamespace
        override val mdlProhibitedSubstitutions: Set<ComplexTypeModel.Derivation> =
            calcProhibitedSubstitutions(rawPart, schema)
        override val mdlFinal: Set<T_DerivationControl.ComplexBase> =
            calcFinalSubstitutions(rawPart, schema)

    }

    private class ComplexModelImpl(
        parent: ResolvedComplexType,
        rawPart: XSGlobalComplexTypeComplex,
        schema: ResolvedSchemaLike
    ) : ComplexModelBase(parent, rawPart, schema), ComplexModel {
        override val mdlName: VNCName = rawPart.name
        override val mdlAbstract: Boolean = rawPart.abstract ?: false
        override val mdlTargetNamespace: VAnyURI? = schema.targetNamespace
        override val mdlProhibitedSubstitutions: Set<ComplexTypeModel.Derivation> =
            calcProhibitedSubstitutions(rawPart, schema)
        override val mdlFinal: Set<T_DerivationControl.ComplexBase> =
            calcFinalSubstitutions(rawPart, schema)

    }

}
