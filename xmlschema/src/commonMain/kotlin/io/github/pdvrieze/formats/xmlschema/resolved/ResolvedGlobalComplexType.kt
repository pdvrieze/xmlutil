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
import io.github.pdvrieze.formats.xmlschema.types.*
import nl.adaptivity.xmlutil.QName

open class ResolvedGlobalComplexType(
    override val mdlQName: QName,
    schema: ResolvedSchemaLike,
    modelFactory: ResolvedGlobalComplexType.() -> Model,
    val mdlAbstract: Boolean = false,
    val location: String = ""
) : ResolvedComplexType(schema), ResolvedGlobalType {

    constructor(rawPart: XSGlobalComplexType, schema: ResolvedSchemaLike, location: String) : this(
        rawPart.name.toQname(schema.targetNamespace),
        schema,
        {
            when (val r = rawPart) {
                is XSGlobalComplexTypeComplex -> ComplexModel(this, r, schema)
                is XSGlobalComplexTypeShorthand -> ShorthandModel(this, r, schema)
                is XSGlobalComplexTypeSimple -> SimpleModel(this, r, schema)
            }
        },
        rawPart.abstract ?: false,
        location
    )

    internal constructor(
        rawPart: SchemaAssociatedElement<XSGlobalComplexType>,
        schema: ResolvedSchemaLike
    ) : this(rawPart.element, schema, rawPart.schemaLocation)

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
        rawPart: XSGlobalComplexTypeSimple,
        schema: ResolvedSchemaLike,
    ) : SimpleModelBase<XSGlobalComplexTypeSimple>(parent, rawPart, schema),
        Model {
        override val mdlContext: VComplexTypeScope.Member = parent
        override val mdlAbstract: Boolean = rawPart.abstract ?: false
        override val mdlProhibitedSubstitutions: Set<VDerivationControl.Complex> =
            calcProhibitedSubstitutions(rawPart, schema)
        override val mdlFinal: Set<VDerivationControl.Complex> =
            calcFinalSubstitutions(rawPart, schema)
    }

    private class ShorthandModel(
        parent: ResolvedComplexType,
        rawPart: XSGlobalComplexTypeShorthand,
        schema: ResolvedSchemaLike,
    ) : ComplexModelBase<XSGlobalComplexTypeShorthand>(parent, rawPart, schema),
        Model {
        override val mdlAbstract: Boolean = rawPart.abstract ?: false
        override val mdlProhibitedSubstitutions: Set<VDerivationControl.Complex> =
            calcProhibitedSubstitutions(rawPart, schema)
        override val mdlFinal: Set<VDerivationControl.Complex> =
            calcFinalSubstitutions(rawPart, schema)
    }

    private class ComplexModel(
        parent: ResolvedComplexType,
        rawPart: XSGlobalComplexTypeComplex,
        schema: ResolvedSchemaLike,
    ) : ComplexModelBase<XSGlobalComplexTypeComplex>(parent, rawPart, schema),
        Model {
        override val mdlAbstract: Boolean = rawPart.abstract ?: false
        override val mdlProhibitedSubstitutions: Set<VDerivationControl.Complex> =
            calcProhibitedSubstitutions(rawPart, schema)
        override val mdlFinal: Set<VDerivationControl.Complex> =
            calcFinalSubstitutions(rawPart, schema)
    }

}
