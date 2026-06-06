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

package io.github.pdvrieze.formats.xpath.impl

import io.github.pdvrieze.formats.xmlschema.datatypes.primitiveInstances.VAnyURI
import io.github.pdvrieze.formats.xmlschema.datatypes.primitiveInstances.VNCName
import nl.adaptivity.xmlutil.QName
import nl.adaptivity.xmlutil.XmlWriter
import nl.adaptivity.xmlutil.localPart
import nl.adaptivity.xmlutil.namespaceURI

@XPathInternal
internal sealed class NodeTest {
    sealed class NameTest() : NodeTest()

    class NodeTypeTest(val type: NodeType, args: List<Expr> = emptyList()) : NodeTest(), ItemType {
        val args: List<Expr> = args.toList()

        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other !is NodeTypeTest) return false

            if (type != other.type) return false

            return true
        }

        override fun hashCode(): Int {
            return type.hashCode()
        }

        override fun appendToString(builder: StringBuilder, output: XmlWriter?) {
            builder.append(type.literal).append("()")
        }
    }

    sealed class NameOrLiteral {
        abstract fun appendToString(builder: StringBuilder)

        class Literal(val literal: String) : NameOrLiteral() {
            override fun appendToString(builder: StringBuilder) {
                StringLiteral(literal).appendToString(builder, null)
            }
        }

        class NCName(val name: String) : NameOrLiteral() {
            override fun appendToString(builder: StringBuilder) {
                builder.append(name)
            }
        }
    }



    class ProcessingInstructionTest(val literal: NameOrLiteral? = null) : NodeTest() {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other !is ProcessingInstructionTest) return false

            if (literal != other.literal) return false

            return true
        }

        override fun hashCode(): Int {
            return literal?.hashCode() ?: 0
        }

        override fun appendToString(builder: StringBuilder, output: XmlWriter?) {
            builder.append("processing-instruction(")
            if (literal != null) {
                literal.appendToString(builder)
            }
            builder.append(")")
        }
    }

    class QNameTest(val qName: QName) : NameTest() {
        override fun appendToString(builder: StringBuilder, output: XmlWriter?) {
            if (qName.namespaceURI.isNotEmpty()) {
                builder.append("Q{").append(qName.namespaceURI).append("}")
            }
            builder.append(qName.localPart)
        }

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

        override fun appendToString(builder: StringBuilder, output: XmlWriter?) {
            val pr = when {
                output == null -> prefix.xmlString
                else -> {
                    output.getOrCreatePrefix(namespace.xmlString, prefix.xmlString)
                }
            }
            builder.append(pr).append(":*")
        }

    }

    object AnyNameTest : NameTest() {
        override fun appendToString(builder: StringBuilder, output: XmlWriter?) {
            builder.append("*")
        }
    }

    abstract fun appendToString(builder: StringBuilder, output: XmlWriter?)
    override fun toString(): String = buildString {
        appendToString(this, null)
    }
}
