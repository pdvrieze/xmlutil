/*
 * Copyright (c) 2023.
 *
 * This file is part of xmlutil.
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

package io.github.pdvrieze.formats.xpath.impl

import io.github.pdvrieze.formats.xmlschema.datatypes.primitiveInstances.VAnyURI
import io.github.pdvrieze.formats.xmlschema.datatypes.primitiveInstances.VNCName
import nl.adaptivity.xmlutil.QName

@XPathInternal
internal sealed class NodeTest {
    sealed class NameTest() : NodeTest()
    class NodeTypeTest(val type: NodeType) : NodeTest() {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other !is NodeTypeTest) return false

            if (type != other.type) return false

            return true
        }

        override fun hashCode(): Int {
            return type.hashCode()
        }

        override fun toString(): String {
            return "${type.literal}()"
        }
    }
    class ProcessingInstructionTest(val literal: String? = null) : NodeTest() {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other !is ProcessingInstructionTest) return false

            if (literal != other.literal) return false

            return true
        }

        override fun hashCode(): Int {
            return literal?.hashCode() ?: 0
        }
    }
    class QNameTest(val qName: QName) : NameTest() {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other !is QNameTest) return false

            if (qName != other.qName) return false

            return true
        }

        override fun hashCode(): Int {
            return qName.hashCode()
        }
    }
    class NSTest(val namespace: VAnyURI, val prefix: VNCName) : NameTest() {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other !is NSTest) return false

            if (namespace != other.namespace) return false

            return true
        }

        override fun hashCode(): Int {
            return namespace.hashCode()
        }

        override fun toString(): String = "${prefix}:*"

    }
    object AnyNameTest : NameTest() {
        override fun toString(): String = "*"
    }
}
