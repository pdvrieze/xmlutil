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

package io.github.pdvrieze.formats.xpath.impl.token

import io.github.pdvrieze.formats.xpath.XPathVersion
import io.github.pdvrieze.formats.xpath.impl.*
import io.github.pdvrieze.xml.schematypes.values.XsdAnyURI
import io.github.pdvrieze.xml.schematypes.values.XsdNCName
import io.github.pdvrieze.xml.schematypes.values.XsdQName
import nl.adaptivity.xmlutil.QName
import nl.adaptivity.xmlutil.localPart
import nl.adaptivity.xmlutil.namespaceURI
import nl.adaptivity.xmlutil.prefix

@XPathInternal
internal sealed interface UnresolvedQNameSpec {
    context(c: XQueryParser.ParseContext)
    fun resolveElementType(): QNameSpec

    context(c: XQueryParser.ParseContext)
    fun resolveFunction(): QNameSpec


    @XPathInternal
    @OptIn(NeedsXPath3_0::class)
    class UnresolvedQName(override val localName: String, override val prefix: String): NamedQNameSpec {
        context(c: XQueryParser.ParseContext)
        override fun resolveElementType(): QNameSpec.EQName {
            val ns = when (prefix) {
                "" -> c.defaultElementTypeNamespace
                else -> requireNotNull(c.namespaceContext.getNamespaceURI(prefix)) { "Missing namespace for prefix '$prefix'" }
            }
            return QNameSpec.ResolvedQName(QName(ns, localName, prefix))
        }

        context(c: XQueryParser.ParseContext)
        override fun resolveFunction(): QNameSpec.EQName {
            val ns = when (prefix) {
                "" -> c.defaultFunctionNamespace
                else -> requireNotNull(c.namespaceContext.getNamespaceURI(prefix)) { "Missing namespace for prefix '$prefix'" }
            }
            return QNameSpec.ResolvedQName(QName(ns, localName, prefix))
        }
    }

}

@OptIn(XPathInternal::class)
internal sealed interface NamedQNameSpec: UnresolvedQNameSpec {
    val localName: String
    val prefix: String?

    context(c: XQueryParser.ParseContext)
    override fun resolveElementType(): QNameSpec.EQName

    context(c: XQueryParser.ParseContext)
    override fun resolveFunction(): QNameSpec.EQName

}

@XPathInternal
internal sealed interface QNameSpec : UnresolvedQNameSpec {

    fun asNodeTest(version: XPathVersion): NodeTest

    @XPathInternal
    context(c: OutputContext)
    fun appendToString(builder: Appendable)

    context(ctx: ExprEvalContext)
    fun eval(namespaceURI: String?, localName: String): Boolean
    fun isAssignableFrom(elemName: QNameSpec): Boolean

    context(c: XQueryParser.ParseContext)
    override fun resolveElementType(): QNameSpec = this

    context(c: XQueryParser.ParseContext)
    override fun resolveFunction(): QNameSpec = this

    @XPathInternal
    sealed class EQName: QNameSpec, NamedQNameSpec {
        abstract val namespace: String
        abstract override val localName: String
        abstract override val prefix: String?

        override fun isAssignableFrom(elemName: QNameSpec): Boolean = when {
            elemName !is EQName -> false
            namespace != elemName.namespace -> false
            localName != elemName.localName -> false
            else -> true
        }

        context(c: XQueryParser.ParseContext)
        override fun resolveElementType(): EQName = this

        context(c: XQueryParser.ParseContext)
        override fun resolveFunction(): EQName = this
    }

    @XPathInternal
    class UriQualifiedName(override val namespace: String, override val localName: String) : EQName() {
        override val prefix: Nothing?
            get() = null

        override fun asNodeTest(version: XPathVersion): NodeTest {
            return NodeTest.QNameTest(XsdQName(namespace, localName, "").toQName())
        }

        context(ctx: ExprEvalContext)
        override fun eval(namespaceURI: String?, localName: String): Boolean {
            return namespace == (namespaceURI ?: "") && localName == this.localName
        }

        @XPathInternal
        context(c: OutputContext)
        override fun appendToString(builder: Appendable) {
            builder.append("Q{").append(namespace).append("}").append(localName)
        }
    }

    @XPathInternal
    class ResolvedQName @NeedsXPath3_0 constructor(val name: QName) : EQName() {
        override val namespace: String get() = name.namespaceURI
        override val localName: String get() = name.localPart
        override val prefix: String get() = name.prefix

        override fun asNodeTest(version: XPathVersion): NodeTest {
            return NodeTest.QNameTest(asQName())
        }

        context(ctx: ExprEvalContext)
        override fun eval(namespaceURI: String?, localName: String): Boolean {
            return namespace == (namespaceURI ?: "") && localName == this.localName
        }

        fun asQName(): QName {
            return name
        }

        @XPathInternal
        context(c: OutputContext)
        override fun appendToString(builder: Appendable) {
            if (name.prefix.isNotEmpty()) {
                builder.append(name.prefix).append(':').append(name.localPart)
            } else if (name.namespaceURI.isNotEmpty()) {
                builder.append("Q{").append(name.namespaceURI).append("}").append(name.localPart)
            } else {
                builder.append(name.localPart)
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

        context(c: XQueryParser.ParseContext)
        override fun resolveFunction(): QNameSpec {
            throw UnsupportedOperationException("Function names cannot be wildcards")
        }
    }

    object Any : WildCard {
        override fun asNodeTest(): NodeTest = NodeTest.AnyNameTest

        override fun isAssignableFrom(elemName: QNameSpec): Boolean = true

        context(ctx: ExprEvalContext)
        override fun eval(namespaceURI: String?, localName: String): Boolean = true

        @XPathInternal
        context(c: OutputContext)
        override fun appendToString(builder: Appendable) {
            builder.append('*')
        }

        override fun toString(): String = "QNameSpec(*)"
    }

    class LocalNameWC(val localName: String) : WildCard {
        override fun asNodeTest(): NodeTest {
            return NodeTest.LocalNameTest(localName)
        }

        override fun isAssignableFrom(elemName: QNameSpec): Boolean = when (elemName) {
            is EQName -> localName == elemName.localName
            is LocalNameWC -> localName == elemName.localName
            else -> false
        }

        context(ctx: ExprEvalContext)
        override fun eval(namespaceURI: String?, localName: String): Boolean {
            return localName == this.localName
        }

        @XPathInternal
        context(c: OutputContext)
        override fun appendToString(builder: Appendable) {
            builder.append("*:").append(localName)
        }

        override fun toString(): String {
            return "QNameSpec(*:$localName)"
        }
    }

    class Namespace(val namespace: String, val prefix: String? = null) : WildCard {

        override fun isAssignableFrom(elemName: QNameSpec): Boolean = when (elemName) {
            is EQName -> namespace == elemName.namespace
            is Namespace -> namespace == elemName.namespace
            else -> false
        }

        @XPathInternal
        context(c: OutputContext)
        override fun appendToString(builder: Appendable) {
            builder.append("Q{").append(namespace).append("}*")
        }

        context(ctx: ExprEvalContext)
        override fun eval(namespaceURI: String?, localName: String): Boolean {
            return namespace == (namespaceURI ?: "")
        }

        override fun asNodeTest(): NodeTest {
            return NodeTest.NSTest(XsdAnyURI(namespace), prefix?.let { XsdNCName(it) })
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
