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

package nl.adaptivity.xmlutil.serialization.compat

import kotlinx.serialization.KSerialClassDesc
import kotlinx.serialization.KSerialClassKind
import nl.adaptivity.xmlutil.QName
import nl.adaptivity.xmlutil.localPart
import nl.adaptivity.xmlutil.multiplatform.assert
import nl.adaptivity.xmlutil.namespaceURI

interface SerialDescriptor: KSerialClassDesc {
    override val name: String
    @Deprecated("Use extKind instead", ReplaceWith("extKind"))
    override val kind: KSerialClassKind
    val extKind: SerialKind
    val elementsCount: Int
    @Deprecated("Use elementsCount", ReplaceWith("elementsCount"))
    override val associatedFieldsCount: Int
        get() = elementsCount

    override fun getElementName(index: Int): String
    override fun getElementIndex(name: String): Int
    fun getEntityAnnotations(): List<Annotation>

    @Deprecated("Use entityAnnotations", ReplaceWith("getEntityAnnotations()"))
    override fun getAnnotationsForClass() = getEntityAnnotations()
    fun getElementAnnotations(index: Int): List<Annotation>

    @Deprecated("Use getElementAnnotations", ReplaceWith("getElementAnnotations(index)"))
    override fun getAnnotationsForIndex(index: Int): List<Annotation> = getElementAnnotations(index)
    fun getElementDescriptor(index: Int): SerialDescriptor
    fun isNullable(index: Int): Boolean
    fun isElementOptional(index: Int): Boolean
}

class DummyParentDescriptor(private val serialName: QName?, private val childDesc: SerialDescriptor): SerialDescriptor {
    override val name: String get() = throw UnsupportedOperationException("Dummy has no name")

    override val kind: KSerialClassKind get() = KSerialClassKind.CLASS

    override val extKind: SerialKind get() = StructureKind.CLASS

    override val elementsCount: Int get() = 1

    override fun getElementName(index: Int): String {
        assert(index==0)
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
        assert(index==0)
        return childDesc
    }

    override fun isNullable(index: Int) = false

    override fun isElementOptional(index: Int) = false
}