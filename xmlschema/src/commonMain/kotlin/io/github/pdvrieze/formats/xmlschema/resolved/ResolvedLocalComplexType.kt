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
import io.github.pdvrieze.formats.xmlschema.types.toDerivationSet

class ResolvedLocalComplexType(
    override val rawPart: XSLocalComplexType,
    schema: ResolvedSchemaLike,
    context: VComplexTypeScope.Member,
) : ResolvedComplexType(schema), ResolvedLocalType,
    VSimpleTypeScope.Member {

    override val mdlScope: VComplexTypeScope.Local = VComplexTypeScope.Local(context)
    override val mdlContext: VComplexTypeScope.Member get() = mdlScope.parent

    override val model: Model<*> by lazy {
        when (val raw = rawPart) {
            is XSLocalComplexTypeComplex -> ComplexModel(raw, schema, this, mdlContext)
            is XSLocalComplexTypeShorthand -> ShorthandModel(raw, schema, this, mdlContext)
            is XSLocalComplexTypeSimple -> SimpleModel(raw, schema, this, mdlContext)
            else -> error("XSLocalComplexType should be sealed")
        }
    }

    // TODO don't inherit simpleTypeContext
    interface Model<R : XSLocalComplexType> : ResolvedComplexType.Model<R> {
        val mdlContext: VComplexTypeScope.Member

        override fun calculateProhibitedSubstitutions(
            rawPart: R,
            schema: ResolvedSchemaLike
        ): Set<VDerivationControl.Complex> {
            return schema.blockDefault.filterIsInstanceTo(HashSet())
        }

    }

    private class SimpleModel(
        rawPart: XSLocalComplexTypeSimple,
        schema: ResolvedSchemaLike,
        parent: ResolvedComplexType,
        override val mdlContext: VComplexTypeScope.Member,
    ) : SimpleModelBase<XSLocalComplexTypeSimple>(parent, rawPart, schema),
        Model<XSLocalComplexTypeSimple> {
        override val mdlAbstract: Boolean get() = false
        override val mdlProhibitedSubstitutions: Set<VDerivationControl.Complex> =
            schema.blockDefault.toDerivationSet()
        override val mdlFinal: Set<VDerivationControl.Complex> =
            schema.finalDefault.toDerivationSet()
    }

    private class ShorthandModel(
        rawPart: XSLocalComplexTypeShorthand,
        schema: ResolvedSchemaLike,
        parent: ResolvedComplexType,
        override val mdlContext: VComplexTypeScope.Member
    ) : ComplexModelBase<XSLocalComplexTypeShorthand>(parent, rawPart, schema),
        Model<XSLocalComplexTypeShorthand> {
        override val mdlAbstract: Boolean get() = false
        override val mdlProhibitedSubstitutions: Set<VDerivationControl.Complex> =
            schema.blockDefault.toDerivationSet()
        override val mdlFinal: Set<VDerivationControl.Complex> =
            schema.finalDefault.toDerivationSet()
    }

    private class ComplexModel(
        rawPart: XSLocalComplexTypeComplex,
        schema: ResolvedSchemaLike,
        parent: ResolvedComplexType,
        override val mdlContext: VComplexTypeScope.Member
    ) : ComplexModelBase<XSLocalComplexTypeComplex>(parent, rawPart, schema),
        Model<XSLocalComplexTypeComplex> {
        override val mdlAbstract: Boolean get() = false
        override val mdlProhibitedSubstitutions: Set<VDerivationControl.Complex> =
            schema.blockDefault.toDerivationSet()
        override val mdlFinal: Set<VDerivationControl.Complex> =
            schema.finalDefault.toDerivationSet()
    }
}

