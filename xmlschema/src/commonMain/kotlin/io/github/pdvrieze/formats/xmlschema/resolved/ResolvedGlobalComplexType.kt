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
import io.github.pdvrieze.formats.xmlschema.datatypes.primitiveInstances.VNCName
import io.github.pdvrieze.formats.xmlschema.datatypes.serialization.*
import io.github.pdvrieze.formats.xmlschema.types.*
import nl.adaptivity.xmlutil.QName

class ResolvedGlobalComplexType(
    override val rawPart: XSGlobalComplexType,
    schema: ResolvedSchemaLike,
    val location: String
) : ResolvedGlobalType, ResolvedComplexType(schema) {

    internal constructor(rawPart: SchemaAssociatedElement<XSGlobalComplexType>, schema: ResolvedSchemaLike) :
            this(rawPart.element, schema, rawPart.schemaLocation)

    override val mdlQName: QName = rawPart.name.toQname(schema.targetNamespace)

    override val mdlScope: VComplexTypeScope.Global
        get() = VComplexTypeScope.Global

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

    override val model: Model by lazy(LazyThreadSafetyMode.SYNCHRONIZED) {
        when (val r = rawPart) {
            is XSGlobalComplexTypeComplex -> ComplexModelImpl(this, r, schema)
            is XSGlobalComplexTypeShorthand -> ShorthandModelImpl(this, r, schema)
            is XSGlobalComplexTypeSimple -> SimpleModelImpl(this, r, schema)
        }

    }

    override fun check(checkedTypes: MutableSet<QName>, inheritedTypes: SingleLinkedList<QName>) {
        if (checkedTypes.add(mdlQName)) {
            super<ResolvedComplexType>.check(checkedTypes, inheritedTypes)
            mdlContentType.check()
            content.check(checkedTypes, inheritedTypes + mdlQName)
        }
    }

    override fun toString(): String {
        return "ComplexType{ name=$mdlQName, base=$mdlBaseTypeDefinition }"
    }

    interface Model : ResolvedComplexType.Model {
        val mdlTargetNamespace: VAnyURI?
        val mdlName: VNCName
    }

    interface SimpleModel : Model, ResolvedSimpleContentType {
        override val mdlContentType: ResolvedSimpleContentType
    }

    interface ComplexBase: Model

    interface ComplexModel : ComplexBase

    interface ImplicitModel : ComplexBase

    private class SimpleModelImpl(
        parent: ResolvedComplexType,
        rawPart: XSGlobalComplexTypeSimple,
        schema: ResolvedSchemaLike,
    ) : SimpleModelBase(parent, rawPart, schema), SimpleModel {
        override val mdlName: VNCName = rawPart.name
        override val mdlContext: VComplexTypeScope.Member
            get() = TODO("not implemented")
        override val mdlAbstract: Boolean = rawPart.abstract ?: false
        override val mdlTargetNamespace: VAnyURI? = schema.targetNamespace
        override val mdlProhibitedSubstitutions: Set<VDerivationControl.Complex> =
            calcProhibitedSubstitutions(rawPart, schema)
        override val mdlFinal: Set<VDerivationControl.Complex> =
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
        override val mdlProhibitedSubstitutions: Set<VDerivationControl.Complex> =
            calcProhibitedSubstitutions(rawPart, schema)
        override val mdlFinal: Set<VDerivationControl.Complex> =
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
        override val mdlProhibitedSubstitutions: Set<VDerivationControl.Complex> =
            calcProhibitedSubstitutions(rawPart, schema)
        override val mdlFinal: Set<VDerivationControl.Complex> =
            calcFinalSubstitutions(rawPart, schema)
    }

}
