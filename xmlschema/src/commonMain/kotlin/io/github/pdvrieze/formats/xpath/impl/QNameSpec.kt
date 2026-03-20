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

package io.github.pdvrieze.formats.xpath.impl

import io.github.pdvrieze.formats.xpath.XPathVersion
import io.github.pdvrieze.formats.xpath.impl.token.NodeType
import io.github.pdvrieze.xml.schematypes.values.XsdAnyURI
import io.github.pdvrieze.xml.schematypes.values.XsdNCName
import nl.adaptivity.xmlutil.QName

@XPathInternal
internal sealed interface QNameSpec {

    fun asNodeTest(version: XPathVersion): NodeTest

    @XPathInternal
    context(c: OutputContext)
    fun appendToString(builder: Appendable)


    @XPathInternal
    class EQName @NeedsXPath3_0 constructor(
        val namespace: String?,
        val localName: String,
        val prefix: String?
    ) : QNameSpec {
        override fun asNodeTest(version: XPathVersion): NodeTest {
            if (namespace == null && prefix == null) {
                NodeType.Companion.maybeValueOf(localName, version)?.let {
                    return NodeTypeTest(it)
                }
            }
            return NodeTest.QNameTest(asQName())
        }

        fun asQName(): QName {
            return QName(namespace ?: "", localName, prefix ?: "")
        }

        context(c: OutputContext)
        @XPathInternal
        override fun appendToString(builder: Appendable) {
            if (prefix != null) {
                builder.append(prefix).append(':').append(localName)
            } else if (namespace != null) {
                builder.append("Q{").append(namespace).append("}").append(localName)
            } else {
                builder.append(localName)
            }
        }

        override fun toString() = buildString {
            append("QNameSpec(")
            context(OutputContext.EMPTY) {
                appendToString(this)
            }
            append(")")
        }
    }


    sealed interface WildCard : QNameSpec {
        override fun asNodeTest(version: XPathVersion): NodeTest = asNodeTest()
        fun asNodeTest(): NodeTest
    }

    object Any : WildCard {
        override fun asNodeTest(): NodeTest = NodeTest.AnyNameTest

        context(c: OutputContext)
        @XPathInternal
        override fun appendToString(builder: Appendable) {
            builder.append('*')
        }

        override fun toString(): String = "QNameSpec(*)"
    }

    class LocalNameWC(val localName: String) : WildCard {
        override fun asNodeTest(): NodeTest {
            return NodeTest.LocalNameTest(localName)
        }

        context(c: OutputContext)
        @XPathInternal
        override fun appendToString(builder: Appendable) {
            builder.append("*:").append(localName)
        }

        override fun toString(): String {
            return "QNameSpec(*:$localName)"
        }
    }

    class Namespace(val namespace: String, val prefix: String? = null) : WildCard {
        context(c: OutputContext)
        @XPathInternal
        override fun appendToString(builder: Appendable) {
            builder.append("Q{").append(namespace).append("}*")
        }

        override fun asNodeTest(): NodeTest {
            return NodeTest.NSTest(XsdAnyURI.Companion(namespace), prefix?.let { XsdNCName.Companion(it) })
        }

        override fun toString() = buildString {
            append("QNameSpec(")
            context(OutputContext.EMPTY) {
                appendToString(this)
            }
            append(")")
        }

    }
}
