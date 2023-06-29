/*
 * Copyright (c) 2021.
 *
 * This file is part of ProcessManager.
 *
 * ProcessManager is free software: you can redistribute it and/or modify it under the terms of version 3 of the
 * GNU Lesser General Public License as published by the Free Software Foundation.
 *
 * ProcessManager is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even
 * the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License along with ProcessManager.  If not,
 * see <http://www.gnu.org/licenses/>.
 */

package io.github.pdvrieze.formats.xmlschema.types

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
        ParticleProperties, I_AttributeContainer, I_Assertions

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
        val term: DirectParticle?
    }
    interface DirectParticle: T_Particle {

    }
}

