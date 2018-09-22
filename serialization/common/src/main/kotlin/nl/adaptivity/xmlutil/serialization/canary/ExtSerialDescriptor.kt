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

package nl.adaptivity.xmlutil.serialization.canary

import kotlinx.serialization.KSerialClassDesc
import kotlinx.serialization.KSerialClassKind
import nl.adaptivity.xmlutil.serialization.XmlDefault
import nl.adaptivity.xmlutil.serialization.compat.SerialDescriptor
import nl.adaptivity.xmlutil.serialization.compat.SerialKind

class ExtSerialDescriptor(private val base: KSerialClassDesc,
                          override val extKind: SerialKind,
                          private val isChildNullable: BooleanArray,
                          private val childDescriptors: Array<SerialDescriptor>): SerialDescriptor {

    override val name: String get() = base.name
    override val kind: KSerialClassKind get() = base.kind

    override fun getElementName(index: Int): String = base.getElementName(index)
    override fun getElementIndex(name: String): Int = base.getElementIndex(name)

    override fun getEntityAnnotations(): List<Annotation> = base.getAnnotationsForClass()

    override fun getElementAnnotations(index: Int) = if(index<elementsCount) base.getAnnotationsForIndex(index) else emptyList()

    override val elementsCount:Int get() = base.associatedFieldsCount

    override fun getElementDescriptor(index: Int): SerialDescriptor = childDescriptors[index]

    override fun isNullable(index:Int): Boolean = isChildNullable[index]
    override fun isElementOptional(index: Int): Boolean = getElementAnnotations(index).any { it is XmlDefault }
}