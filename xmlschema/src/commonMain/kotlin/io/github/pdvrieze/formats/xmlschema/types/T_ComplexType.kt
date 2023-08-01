/*
 * Copyright (c) 2021.
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

package io.github.pdvrieze.formats.xmlschema.types

import io.github.pdvrieze.formats.xmlschema.datatypes.serialization.XSAnyAttribute
import io.github.pdvrieze.formats.xmlschema.datatypes.serialization.XSLocalAttribute
import io.github.pdvrieze.formats.xmlschema.model.I_Assertions

interface T_ComplexType : T_Type {
    /**
     * May not have simpleContent child
     */
    val mixed: Boolean?

    /** Default: false */
    val defaultAttributesApply: Boolean? // default true

    /** Either this or shorthand content */
    val content: Content

    interface Content

    sealed interface ContentSealed : Content

    interface SimpleContent : ContentSealed {
        val derivation: SimpleDerivation
    }

    interface SimpleDerivation : XSI_Annotated {

    }

    sealed interface SimpleDerivationBase : SimpleDerivation

    interface ComplexContent : ContentSealed {
        val derivation: T_ComplexDerivation
    }

    interface ShorthandContent : ContentSealed,
        ParticleProperties, I_AttributeContainer, I_Assertions {
        val attributes: List<XSLocalAttribute>

        /** Name elements AttributeGroup */
        val attributeGroups: List<T_AttributeGroupRef>
        val anyAttribute: XSAnyAttribute?
    }

    interface Simple : T_ComplexType {
        override val content: SimpleContent
    }

    interface Complex : T_ComplexType {
        override val content: ComplexContent
    }

    interface Shorthand : T_ComplexType, ShorthandContent {
        override val content: ShorthandContent get() = this
    }

    interface ParticleProperties {
//        val term: T_Particle?
    }

}

