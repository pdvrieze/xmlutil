/*
 * Copyright (c) 2026.
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

package io.github.pdvrieze.xml.schematypes.types

import io.github.pdvrieze.xml.schematypes.values.XsdQName
import nl.adaptivity.xmlutil.XMLConstants

interface AnyType {
    fun derivesFrom(expectedBaseType: AnyType): Boolean {
        val expectedName = expectedBaseType.name ?: return false
        var t = this
        do {
            val tn = t.name
            if (tn != null) {
                if (tn.isEquivalent(expectedName)) return true
                if (tn.isEquivalent(Instance.name)) return false
            }
            t = t.baseType
        } while (true)
    }

    val name: XsdQName?
    val baseType: AnyType


    object Instance: BuiltinType {
        override val name: XsdQName = XsdQName(XMLConstants.XSD_NS_URI, "any", "xs")
        override val baseType: AnyType get() = this

        override fun derivesFrom(expectedBaseType: AnyType): Boolean {
            return expectedBaseType.name?.isEquivalent(name) ?: false
        }

        override fun toString(): String = "xs:any"

        override fun hashCode(): Int {
            return name.hashCode()
        }

        override fun equals(other: Any?): Boolean {
            return other is AnyType && other.name == name
        }
    }
}
