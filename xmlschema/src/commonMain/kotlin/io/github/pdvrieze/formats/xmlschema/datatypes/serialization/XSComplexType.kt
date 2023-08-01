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

package io.github.pdvrieze.formats.xmlschema.datatypes.serialization

import io.github.pdvrieze.formats.xmlschema.types.I_AttributeContainer
import io.github.pdvrieze.formats.xmlschema.types.T_ComplexType

interface XSIComplexType : T_ComplexType {
    override val content: XSComplexType.Content
}

sealed interface XSComplexType : XSIComplexType {

    interface Content : T_ComplexType.Content {
        val derivation: Derivation
    }

    interface Derivation: I_AttributeContainer {
        val attributes: List<XSLocalAttribute>
        val attributeGroups: List<XSAttributeGroupRef>
        val anyAttribute: XSAnyAttribute?
    }

    sealed interface ComplexBase : XSIComplexType {
        override val content: XSI_ComplexContent.Complex
    }

    sealed interface Complex : ComplexBase {
        override val content: XSComplexContent
    }

    sealed interface Shorthand : ComplexBase, T_ComplexType.Shorthand,
        XSI_ComplexContent.Complex,
        XSI_ComplexDerivation {
        override val content: Shorthand

        override val term: XSComplexContent.XSIDerivationParticle?
        override val asserts: List<XSAssert>
        override val attributes: List<XSLocalAttribute>
        override val attributeGroups: List<XSAttributeGroupRef>
        override val openContent: XSOpenContent?
    }

    interface Simple : XSIComplexType {
        override val content: XSSimpleContent
    }
}

