/*
 * Copyright (c) 2023-2026.
 *
 * This file is part of xmlutil.
 *
 * This file is licenced to you under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance
 * with the License.  You should have  received a copy of the license
 * with the source distribution. Alternatively, you may obtain a copy
 * of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or
 * implied.  See the License for the specific language governing
 * permissions and limitations under the License.
 */

package io.github.pdvrieze.formats.xmlschema.datatypes.primitiveInstances

import io.github.pdvrieze.formats.xmlschema.datatypes.primitiveTypes.AnyURIType
import io.github.pdvrieze.formats.xmlschema.datatypes.primitiveTypes.NCNameType
import nl.adaptivity.xmlutil.QName

class VQName(namespaceUri: String, localName: String, prefix: String) : VAnySimpleType {
    constructor(localName: String) : this("", localName, "")

//    init {
//        NCNameType.mdlFacets.validateRepresentationOnly(NCNameType, VString(localName))
//        if (prefix.isNotEmpty()) NCNameType.mdlFacets.validateRepresentationOnly(NCNameType, VString(prefix))
//        if (namespaceUri.isNotEmpty()) AnyURIType.mdlFacets.validateRepresentationOnly(NCNameType, VString(namespaceUri))
//    }

    val namespaceUri: String = when {
        namespaceUri.isEmpty() -> ""
        else -> AnyURIType.mdlFacets.validateRepresentationOnly(NCNameType, VString(namespaceUri)).xmlString
    }

    val localName: String =
        NCNameType.mdlFacets.validateRepresentationOnly(NCNameType, VString(localName)).xmlString

    val prefix: String = when {
        prefix.isEmpty() -> ""
        else -> NCNameType.mdlFacets.validateRepresentationOnly(NCNameType, VString(prefix)).xmlString
    }

    override val xmlString: String
        get() = "$prefix:$localName"

    fun toQName(): QName = QName(namespaceUri, localName, prefix)



    override fun toString(): String = when {
        prefix.isEmpty() -> "{${namespaceUri}$localName}"
        else -> "{${namespaceUri}$prefix:$localName}"
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || this::class != other::class) return false

        other as VQName

        if (namespaceUri != other.namespaceUri) return false
        if (localName != other.localName) return false
        if (prefix != other.prefix) return false

        return true
    }

    override fun hashCode(): Int {
        var result = namespaceUri.hashCode()
        result = 31 * result + localName.hashCode()
        result = 31 * result + prefix.hashCode()
        return result
    }

}
