/*
 * Copyright (c) 2018.
 *
 * This file is part of xmlutil.
 *
 * xmlutil is free software: you can redistribute it and/or modify it under the terms of version 3 of the
 * GNU Lesser General Public License as published by the Free Software Foundation.
 *
 * xmlutil is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even
 * the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License along with xmlutil.  If not,
 * see <http://www.gnu.org/licenses/>.
 */

package nl.adaptivity.xmlutil.serialization

import kotlinx.serialization.SerialDescriptor
import kotlinx.serialization.SerialKind
import kotlinx.serialization.StructureKind
import nl.adaptivity.xmlutil.QName
import nl.adaptivity.xmlutil.localPart
import nl.adaptivity.xmlutil.multiplatform.assert
import nl.adaptivity.xmlutil.namespaceURI

internal class DummyParentDescriptor(private val serialName: QName?, private val childDesc: SerialDescriptor):
    SerialDescriptor {
    /** This merely mirrors the parent name as tags need a basis and this object is only used at top level. */
    override val name: String get() = childDesc.name

    override val kind: SerialKind get() = StructureKind.CLASS

    override val elementsCount: Int get() = 1

    override fun getElementName(index: Int): String {
        assert(index == 0)
        return when {
            serialName == null -> childDesc.name
            serialName.namespaceURI=="" -> serialName.localPart
            else -> "{${serialName.namespaceURI}}${serialName.localPart}"
        }
    }

    override fun getElementIndex(name: String): Int = 0

    override fun getEntityAnnotations(): List<Annotation> = emptyList()

    // The element annotations are use site annotations, Entity annotations are declaration site
    override fun getElementAnnotations(index: Int): List<Annotation>  = emptyList()

    override fun getElementDescriptor(index: Int): SerialDescriptor {
        assert(index == 0)
        return childDesc
    }

    override val isNullable: Boolean get() = false

    override fun isElementOptional(index: Int) = false
}