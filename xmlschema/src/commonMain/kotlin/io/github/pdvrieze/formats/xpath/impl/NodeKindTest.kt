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

import io.github.pdvrieze.formats.xpath.data.EvaluationException
import io.github.pdvrieze.formats.xpath.data.EvaluationException.ErrorCodes
import io.github.pdvrieze.formats.xpath.data.XdmNode
import io.github.pdvrieze.formats.xpath.data.XdmValue
import io.github.pdvrieze.formats.xpath.impl.token.NodeType
import io.github.pdvrieze.formats.xpath.impl.token.QNameSpec
import nl.adaptivity.xmlutil.QName
import nl.adaptivity.xmlutil.dom2.*
import nl.adaptivity.xmlutil.isEquivalent
import nl.adaptivity.xmlutil.localPart
import nl.adaptivity.xmlutil.dom2.Comment as Comment2

@XPathInternal
public sealed class NodeKindTest() : NodeTest(), ItemTypeTest {
    abstract val type: NodeType

    context(c: OutputContext)
    final override fun appendToString(builder: Appendable) {
        builder.append(type.literal).append("()")
    }

    @NeedsXPath2
    internal class DocumentTest(val arg: NodeKindTest? = null) : NodeKindTest() {
        override val type: NodeType get() = NodeType.DOCUMENT

        context(ctx: ExprEvalContext)
        override fun eval(it: XdmValue, index: Int, count: Int): Boolean {
            if (it !is XdmNode) return false
            val n = it.node
            if (n !is Document) return false
            if (arg == null) return true
            TODO("Document test with argument not supported yet")
        }

        context(ctx: ExprEvalContext)
        override fun isSubtypeOf(baseType: ItemTypeTest): Boolean {
            return baseType == AnyKind || baseType is DocumentTest
        }

        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other == null || this::class != other::class) return false

            other as DocumentTest

            return arg == other.arg
        }

        override fun hashCode(): Int {
            return arg?.hashCode() ?: 0
        }
    }

    @NeedsXPath2
    internal class ElementTest private constructor(val elemName: QNameSpec?, val typeName: QName?, val isOptional: Boolean, dummy: Unit) : NodeKindTest() {
        constructor(name: QNameSpec? = null): this(name, null, false, Unit)
        constructor(name: QNameSpec, typeName: QName, isOptional: Boolean): this(name, typeName, isOptional, Unit)

        override val type: NodeType get() = NodeType.ELEMENT

        context(ctx: ExprEvalContext)
        override fun isSubtypeOf(baseType: ItemTypeTest): Boolean {
            if (baseType == AnyKind) return true
            if (baseType !is ElementTest) return false
            if (baseType.elemName == null && baseType.typeName == null) return true
            return false
        }

        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other == null || this::class != other::class) return false

            other as ElementTest

            if (elemName != other.elemName) return false
            if (typeName != other.typeName) return false

            return true
        }

        context(ctx: ExprEvalContext)
        override fun eval(it: XdmValue, index: Int, count: Int): Boolean {
            if (it !is XdmNode) return false
            val elem = it.node as? Element ?: return false
            if (elemName != null) {
                if (! elemName.eval(elem.namespaceURI, elem.localName)) return false

                if (typeName != null) {
                    val expectedSchemaType = ctx.resolveType(typeName)
                        ?: throw EvaluationException(ErrorCodes.XPTY0004_TYPE_ERROR, "Unknown type $typeName")

                    if (! it.type.isSubtypeOf(expectedSchemaType)) return false
                    if (elem.localName != typeName.localPart) return false
                }
            }
            return true
        }

        override fun hashCode(): Int {
            var result = elemName?.hashCode() ?: 0
            result = 31 * result + (typeName?.hashCode() ?: 0)
            return result
        }


    }

    @NeedsXPath2
    internal class AttributeTest private constructor(val elemName: QNameSpec?, val typeName: QName?, isOptional: Boolean, dummy: Unit) : NodeKindTest() {
        constructor(name: QNameSpec? = null): this(name, null, false, Unit)
        constructor(name: QNameSpec, typeName: QName, isOptional: Boolean): this(name, typeName, isOptional, Unit)

        override val type: NodeType get() = NodeType.ATTRIBUTE

        context(ctx: ExprEvalContext)
        override fun eval(it: XdmValue, index: Int, count: Int): Boolean {
            return it is XdmNode && it.node is Attr
        }

        context(ctx: ExprEvalContext)
        override fun isSubtypeOf(baseType: ItemTypeTest): Boolean {
            if (baseType == AnyKind) return true
            TODO("not implemented")
        }

        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other == null || this::class != other::class) return false

            other as AttributeTest

            if (elemName != other.elemName) return false
            if (typeName != other.typeName) return false

            return true
        }

        override fun hashCode(): Int {
            var result = elemName?.hashCode() ?: 0
            result = 31 * result + (typeName?.hashCode() ?: 0)
            return result
        }


    }

    @NeedsXPath2
    internal class SchemaElementTest(val name: QName) : NodeKindTest() {
        override val type: NodeType get() = NodeType.SCHEMA_ELEMENT

        context(ctx: ExprEvalContext)
        override fun eval(it: XdmValue, index: Int, count: Int): Boolean {
            if (it !is XdmNode) return false
            val n = it.node as? Element ?: return false
            TODO("Schema element matching not complete")
        }

        context(ctx: ExprEvalContext)
        override fun isSubtypeOf(baseType: ItemTypeTest): Boolean {
            if (baseType == AnyKind) return true
            TODO("not implemented. Needs substitution group comparison")
        }

        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other == null || this::class != other::class) return false

            other as SchemaElementTest

            return name == other.name
        }

        override fun hashCode(): Int {
            return name.hashCode()
        }

    }

    @NeedsXPath2
    internal class SchemaAttributeTest(val name: QName) : NodeKindTest() {
        override val type: NodeType get() = NodeType.SCHEMA_ATTRIBUTE

        context(ctx: ExprEvalContext)
        override fun eval(it: XdmValue, index: Int, count: Int): Boolean {
            if (it !is XdmNode) return false
            val n = it.node as? Attr ?: return false
            TODO("Schema element matching not complete")
        }

        context(ctx: ExprEvalContext)
        override fun isSubtypeOf(baseType: ItemTypeTest): Boolean {
            if (baseType == AnyKind) return true
            return baseType is SchemaAttributeTest && baseType.name.isEquivalent(name)
        }

        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other == null || this::class != other::class) return false

            other as SchemaAttributeTest

            return name == other.name
        }

        override fun hashCode(): Int {
            return name.hashCode()
        }

    }

    internal class ProcInstrTest private constructor(val name: QName?, val text: String?) : NodeKindTest() {
        constructor(): this(null, null)
        constructor(name: QName): this(name, null)
        constructor(text: String): this(null, text)

        override val type: NodeType get() = NodeType.PROCESSING_INSTRUCTION

        context(ctx: ExprEvalContext)
        override fun eval(it: XdmValue, index: Int, count: Int): Boolean {
            if (it !is XdmNode) return false
            val n = it.node as? ProcessingInstruction ?: return false
            if (name != null && ! name.isEquivalent(QName(n.target))) return false
            if (text != null && text != n.data) return false
            return true
        }

        context(ctx: ExprEvalContext)
        override fun isSubtypeOf(baseType: ItemTypeTest): Boolean {
            if (baseType == AnyKind) return true
            return baseType is ProcInstrTest && (baseType.name == name)
        }

        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other == null || this::class != other::class) return false

            other as ProcInstrTest

            if (name != other.name) return false
            if (text != other.text) return false

            return true
        }

        override fun hashCode(): Int {
            var result = name?.hashCode() ?: 0
            result = 31 * result + (text?.hashCode() ?: 0)
            return result
        }


    }

    internal object CommentTest : NodeKindTest() {
        override val type: NodeType get() = NodeType.COMMENT
        context(ctx: ExprEvalContext)
        override fun eval(
            it: XdmValue,
            index: Int,
            count: Int
        ): Boolean {
            return it is XdmNode && it.node is Comment2

        }

        context(ctx: ExprEvalContext)
        override fun isSubtypeOf(baseType: ItemTypeTest): Boolean =
            baseType == AnyKind || baseType == CommentTest
    }

    internal object TextTest : NodeKindTest() {
        override val type: NodeType get() = NodeType.TEXT

        context(ctx: ExprEvalContext)
        override fun eval(
            it: XdmValue,
            index: Int,
            count: Int
        ): Boolean {
            return it is XdmNode && it.node is Text
        }

        context(ctx: ExprEvalContext)
        override fun isSubtypeOf(baseType: ItemTypeTest): Boolean =
            baseType == AnyKind || baseType == TextTest
    }

    @NeedsXPath3_0
    internal object NamepaceNodeTest : NodeKindTest() {
        override val type: NodeType get() = NodeType.NAMESPACE_NODE

        context(ctx: ExprEvalContext)
        override fun eval(it: XdmValue, index: Int, count: Int): Boolean {
            throw EvaluationException(ErrorCodes.XQST0134_NS_AXIS_NOT_SUPPORTED, "Namespace node test not supported")
        }

        context(ctx: ExprEvalContext)
        override fun isSubtypeOf(baseType: ItemTypeTest): Boolean =
            baseType == AnyKind || baseType == NamepaceNodeTest
    }

    internal object AnyKind : NodeKindTest() {
        override val type: NodeType get() = NodeType.ANY_KIND

        context(ctx: ExprEvalContext)
        override fun eval(it: XdmValue, index: Int, count: Int): Boolean {
            return it is XdmNode
        }

        context(ctx: ExprEvalContext)
        override fun isSubtypeOf(baseType: ItemTypeTest): Boolean {
            return baseType == AnyKind
        }
    }


}
