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

import kotlinx.serialization.KSerialClassKind
import nl.adaptivity.xmlutil.QName


internal interface TypeInfo {
    /** The name declared for the type in kserialclassdesc */
    val typeName: String?
    /** The kind declared for the type in kserialclassdesc */
    val serialClassKind: KSerialClassKind
}

internal interface ExtTypeInfo: TypeInfo {
    /** The annotations declared for the type */
    val declAnnotations: List<Annotation>
}


internal interface UseInfo {
    fun replaceUnknownOutputWith(newKind: OutputKind)

    /** Optionally a qname derived from the use site */
    val useQName: QName?
    /** The index of the child in the list */
    val index: Int
    /** The declared child name on the parent classdesc */
    val declChildName: String
    /** The dhild name requested another way */
    val childName: QName?
    /** Annotations provided at the use site */
    val useAnnotations: List<Annotation>
    /** The effective tag name to use */
    val tagName: QName
    /** The way to write the tag */
    val outputKind: OutputKind
}