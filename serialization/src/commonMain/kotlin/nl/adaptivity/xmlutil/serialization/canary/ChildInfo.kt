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

package nl.adaptivity.xmlutil.serialization.canary

import kotlinx.serialization.*

internal fun childInfoForClassDesc(desc: kotlinx.serialization.SerialDescriptor): Array<SerialDescriptor?> =
    when (desc.kind) {
        is PrimitiveKind,
        UnionKind.ENUM_KIND,
        UnionKind.OBJECT      -> emptyArray()

        StructureKind.MAP,
        StructureKind.LIST    -> arrayOf(null, DUMMYSERIALDESC)

        UnionKind.POLYMORPHIC -> arrayOfNulls(2)

        else                  -> arrayOfNulls(desc.elementsCount)
    }

internal val DUMMYCHILDINFO = DUMMYSERIALDESC

internal object DUMMYSERIALDESC : SerialDescriptor {
    override val name: String get() = throw AssertionError("Dummy descriptors have no names")
    @Suppress("OverridingDeprecatedMember")
    override val kind: SerialKind
        get() = throw AssertionError("Dummy descriptors have no kind")

    override val elementsCount: Int get() = 0

    override fun getElementName(index: Int) = throw AssertionError("No children in dummy")

    override fun getElementIndex(name: String) = throw AssertionError("No children in dummy")

    override fun getEntityAnnotations(): List<Annotation> = emptyList()

    override fun getElementAnnotations(index: Int) = throw AssertionError("No children in dummy")

    override fun getElementDescriptor(index: Int): SerialDescriptor {
        throw AssertionError("No children in dummy")
    }

    override val isNullable: Boolean get() = false

    override fun isElementOptional(index: Int) =
        throw AssertionError("No children in dummy")
}
