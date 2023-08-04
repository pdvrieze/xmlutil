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
import io.github.pdvrieze.formats.xmlschema.datatypes.serialization.*
import io.github.pdvrieze.formats.xmlschema.types.VDerivationControl
import io.github.pdvrieze.formats.xmlschema.types.toDerivationSet
import nl.adaptivity.xmlutil.QName

class ResolvedLocalComplexType(
    override val rawPart: XSLocalComplexType,
    schema: ResolvedSchemaLike,
    override val mdlContext: VComplexTypeScope.Member,
) : ResolvedComplexType(schema), ResolvedLocalType,
    VSimpleTypeScope.Member {

    override val mdlScope: VComplexTypeScope.Local
        get() = VComplexTypeScope.Local(mdlContext)

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

    interface Foo {}
//    interface Context {} /* : ComplexTypeContext*/

    // TODO don't inherit simpleTypeContext
    interface Model : ResolvedComplexType.Model {
        val mdlContext: VComplexTypeScope.Member
    }

    interface SimpleModel : Model, ResolvedSimpleContentType {
        override val mdlContentType: ResolvedSimpleContentType
    }

    private class SimpleModelImpl(
        rawPart: XSLocalComplexTypeSimple,
        schema: ResolvedSchemaLike,
        parent: ResolvedComplexType,
        override val mdlContext: VComplexTypeScope.Member
    ) : SimpleModelBase(parent, rawPart, schema), SimpleModel {
        override val mdlAbstract: Boolean get() = false
        override val mdlProhibitedSubstitutions: Set<VDerivationControl.Complex> =
            schema.blockDefault.toDerivationSet()
        override val mdlFinal: Set<VDerivationControl.Complex> =
            schema.finalDefault.toDerivationSet()
    }

    private class ShorthandModelImpl(
        rawPart: XSLocalComplexTypeShorthand,
        schema: ResolvedSchemaLike,
        parent: ResolvedComplexType,
        override val mdlContext: VComplexTypeScope.Member
    ) : ComplexModelBase(parent, rawPart, schema),
        Model {
        override val mdlAbstract: Boolean get() = false
        override val mdlProhibitedSubstitutions: Set<VDerivationControl.Complex> =
            schema.blockDefault.toDerivationSet()
        override val mdlFinal: Set<VDerivationControl.Complex> =
            schema.finalDefault.toDerivationSet()
    }

    private class ComplexModelImpl(
        rawPart: XSLocalComplexTypeComplex,
        schema: ResolvedSchemaLike,
        parent: ResolvedComplexType,
        override val mdlContext: VComplexTypeScope.Member
    ) : ComplexModelBase(parent, rawPart, schema),
        Model {
        override val mdlAbstract: Boolean get() = false
        override val mdlProhibitedSubstitutions: Set<VDerivationControl.Complex> =
            schema.blockDefault.toDerivationSet()
        override val mdlFinal: Set<VDerivationControl.Complex> =
            schema.finalDefault.toDerivationSet()
    }
}

