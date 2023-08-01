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

interface XSIComplexType : XSIType {
    val content: XSI_ComplexContent

    /**
     * May not have simpleContent child
     */
    val mixed: Boolean?

    /** Default: false */
    val defaultAttributesApply: Boolean? // default true
}

/**
 * This second interface has only 2 children to allow for sealed nature to work.
 */
sealed interface XSComplexType : XSIComplexType {

    interface Derivation {
        val attributes: List<XSLocalAttribute>
        val attributeGroups: List<XSAttributeGroupRef>
        val anyAttribute: XSAnyAttribute?
    }

    sealed interface ComplexBase : XSIComplexType {
        override val content: XSI_ComplexContent
    }

    sealed interface Complex : ComplexBase {
        override val content: XSComplexContent
    }

    sealed interface Shorthand : ComplexBase, XSI_ComplexDerivation, XSI_ComplexContent {
        override val content: Shorthand

        override val asserts: List<XSAssert>
    }

    interface Simple : XSIComplexType {
        override val content: XSSimpleContent
    }
}

