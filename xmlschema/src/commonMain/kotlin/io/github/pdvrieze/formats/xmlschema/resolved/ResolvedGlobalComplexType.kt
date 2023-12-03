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

import io.github.pdvrieze.formats.xmlschema.datatypes.serialization.*
import io.github.pdvrieze.formats.xmlschema.types.VDerivationControl
import nl.adaptivity.xmlutil.QName

open class ResolvedGlobalComplexType(
    override val mdlQName: QName,
    schema: ResolvedSchemaLike,
    modelFactory: ResolvedGlobalComplexType.() -> Model,
    val mdlAbstract: Boolean = false,
    val location: String = ""
) : ResolvedComplexType(schema), ResolvedGlobalType {

    internal constructor(elemPart: SchemaElement<XSGlobalComplexType>, schema: ResolvedSchemaLike, dummy: String = "") : this(
        elemPart.elem.name.toQname(schema.targetNamespace),
        schema,
        {
            when (elemPart.elem) {
                is XSGlobalComplexTypeComplex -> ComplexModel(this, elemPart.cast(), schema)
                is XSGlobalComplexTypeShorthand -> ShorthandModel(this, elemPart.cast(), schema)
                is XSGlobalComplexTypeSimple -> SimpleModel(this, elemPart.cast(), schema)
            }
        },
        elemPart.elem.abstract ?: false,
        elemPart.schemaLocation
    )

    override val mdlScope: VComplexTypeScope.Global get() = VComplexTypeScope.Global

    override val model: Model by lazy { modelFactory() }

    override fun toString(): String {
        return "ComplexType{ name=$mdlQName, base=$mdlBaseTypeDefinition }"
    }

    interface Model : ResolvedComplexType.Model {

        override fun calculateProhibitedSubstitutions(
            rawPart: XSIComplexType,
            schema: ResolvedSchemaLike
        ): Set<VDerivationControl.Complex> {
            return (rawPart as XSGlobalComplexType).block ?: schema.blockDefault.filterIsInstanceTo(HashSet())
        }
    }

    private class SimpleModel(
        parent: ResolvedComplexType,
        elemPart: SchemaElement<XSGlobalComplexTypeSimple>,
        schema: ResolvedSchemaLike,
    ) : SimpleModelBase<XSGlobalComplexTypeSimple>(parent, elemPart, schema), Model {
        override val mdlContext: VComplexTypeScope.Member = parent
        override val mdlAbstract: Boolean = elemPart.elem.abstract ?: false
        override val mdlProhibitedSubstitutions: Set<VDerivationControl.Complex> =
            calcProhibitedSubstitutions(elemPart.elem, schema)
        override val mdlFinal: Set<VDerivationControl.Complex> =
            calcFinalSubstitutions(elemPart.elem, schema)

        init {
            require(schema !is RedefineSchema) { "Redefines must extend their base type, thus a complex type in a redefine can not have simple content: ${elemPart.elem.name}" }
        }
    }

    private class ShorthandModel(
        parent: ResolvedComplexType,
        elemPart: SchemaElement<XSGlobalComplexTypeShorthand>,
        schema: ResolvedSchemaLike,
    ) : ComplexModelBase<XSGlobalComplexTypeShorthand>(parent, elemPart, schema),
        Model {
        override val mdlAbstract: Boolean = elemPart.elem.abstract ?: false
        override val mdlProhibitedSubstitutions: Set<VDerivationControl.Complex> =
            calcProhibitedSubstitutions(elemPart.elem, schema)
        override val mdlFinal: Set<VDerivationControl.Complex> =
            calcFinalSubstitutions(elemPart.elem, schema)
    }

    private class ComplexModel(
        parent: ResolvedComplexType,
        elemPart: SchemaElement<XSGlobalComplexTypeComplex>,
        schema: ResolvedSchemaLike,
    ) : ComplexModelBase<XSGlobalComplexTypeComplex>(parent, elemPart, schema),
        Model {
        override val mdlAbstract: Boolean = elemPart.elem.abstract ?: false
        override val mdlProhibitedSubstitutions: Set<VDerivationControl.Complex> =
            calcProhibitedSubstitutions(elemPart.elem, schema)
        override val mdlFinal: Set<VDerivationControl.Complex> =
            calcFinalSubstitutions(elemPart.elem, schema)
    }

}
