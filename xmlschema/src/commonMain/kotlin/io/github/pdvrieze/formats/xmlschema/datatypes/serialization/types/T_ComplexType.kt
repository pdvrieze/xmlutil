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

package io.github.pdvrieze.formats.xmlschema.datatypes.serialization.types

import io.github.pdvrieze.formats.xmlschema.datatypes.serialization.groups.G_ComplexTypeModel

interface T_ComplexType: T_Type, G_ComplexTypeModel, T_Element.Complex {
    /**
     * May not have simpleContent child
     */
    val mixed: Boolean?

    /** Default: false */
    val defaultAttributesApply: Boolean? // default true

    /** Either this or shorthand content */
    override val content: Content

    interface Content : G_ComplexTypeModel.Base

    sealed interface ContentSealed: Content
    interface SimpleContent : ContentSealed, G_ComplexTypeModel.SimpleContent {
        val derivation: SimpleDerivation
    }

    interface SimpleDerivation : XSI_Annotated {

    }

    sealed interface SimpleDerivationBase: SimpleDerivation

    interface ComplexContent : ContentSealed, G_ComplexTypeModel.ComplexContent {
        val derivation: T_ComplexDerivation
    }

    interface ShorthandContent :
        ContentSealed,
        G_ComplexTypeModel.Shorthand

    interface Simple: T_ComplexType {
        override val content: SimpleContent
    }

    interface Complex: T_ComplexType {
        override val content: ComplexContent
    }

    interface Shorthand: T_ComplexType, ShorthandContent {
        override val content: ShorthandContent get() = this
    }

}

