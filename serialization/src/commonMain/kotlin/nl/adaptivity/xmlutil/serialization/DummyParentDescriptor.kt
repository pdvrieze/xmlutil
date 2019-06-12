/*
 * Copyright (c) 2018.
 *
 * This file is part of XmlUtil.
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

package nl.adaptivity.xmlutil.serialization

import kotlinx.serialization.SerialDescriptor
import kotlinx.serialization.SerialKind
import kotlinx.serialization.StructureKind
import nl.adaptivity.xmlutil.QName
import nl.adaptivity.xmlutil.localPart
import nl.adaptivity.xmlutil.core.impl.multiplatform.assert
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