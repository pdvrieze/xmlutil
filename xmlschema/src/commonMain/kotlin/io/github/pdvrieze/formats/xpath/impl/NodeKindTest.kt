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

import io.github.pdvrieze.formats.xpath.data.XdmValue
import io.github.pdvrieze.formats.xpath.impl.token.NodeType
import io.github.pdvrieze.formats.xpath.impl.token.QNameSpec
import nl.adaptivity.xmlutil.QName
import nl.adaptivity.xmlutil.isEquivalent

@XPathInternal
public sealed class NodeKindTest() : NodeTest(), ItemTypeTest {
    abstract val type: NodeType

    context(c: OutputContext)
    override fun appendToString(builder: Appendable) {
        builder.append(type.literal).append("()")
    }

    @NeedsXPath2
    internal class Document(val arg: NodeKindTest? = null) : NodeKindTest() {
        override val type: NodeType get() = NodeType.DOCUMENT

        context(ctx: ExprEvalContext)
        override fun isSubtypeOf(baseType: ItemTypeTest): Boolean {
            return baseType == AnyKind || baseType is Document
        }

        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other == null || this::class != other::class) return false

            other as Document

            return arg == other.arg
        }

        override fun hashCode(): Int {
            return arg?.hashCode() ?: 0
        }
    }

    @NeedsXPath2
    internal class Element private constructor(val elemName: QNameSpec?, val typeName: QName?, val isOptional: Boolean, dummy: Unit) : NodeKindTest() {
        constructor(name: QNameSpec? = null): this(name, null, false, Unit)
        constructor(name: QNameSpec, typeName: QName, isOptional: Boolean): this(name, typeName, isOptional, Unit)

        override val type: NodeType get() = NodeType.ELEMENT

        context(ctx: ExprEvalContext)
        override fun isSubtypeOf(baseType: ItemTypeTest): Boolean {
            if (baseType == AnyKind) return true
            if (baseType !is Element) return false
            if (baseType.elemName == null && baseType.typeName == null) return true
            return false
        }

        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other == null || this::class != other::class) return false

            other as Element

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
    internal class Attribute private constructor(val elemName: QNameSpec?, val typeName: QName?, isOptional: Boolean, dummy: Unit) : NodeKindTest() {
        constructor(name: QNameSpec? = null): this(name, null, false, Unit)
        constructor(name: QNameSpec, typeName: QName, isOptional: Boolean): this(name, typeName, isOptional, Unit)

        override val type: NodeType get() = NodeType.ATTRIBUTE

        context(ctx: ExprEvalContext)
        override fun isSubtypeOf(baseType: ItemTypeTest): Boolean {
            if (baseType == AnyKind) return true
            TODO("not implemented")
        }

        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other == null || this::class != other::class) return false

            other as Attribute

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
    internal class SchemaElement(val name: QName) : NodeKindTest() {
        override val type: NodeType get() = NodeType.SCHEMA_ELEMENT

        context(ctx: ExprEvalContext)
        override fun isSubtypeOf(baseType: ItemTypeTest): Boolean {
            if (baseType == AnyKind) return true
            TODO("not implemented. Needs substitution group comparison")
        }

        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other == null || this::class != other::class) return false

            other as SchemaElement

            return name == other.name
        }

        override fun hashCode(): Int {
            return name.hashCode()
        }

    }

    @NeedsXPath2
    internal class SchemaAttribute(val name: QName) : NodeKindTest() {
        override val type: NodeType get() = NodeType.SCHEMA_ATTRIBUTE

        context(ctx: ExprEvalContext)
        override fun isSubtypeOf(baseType: ItemTypeTest): Boolean {
            if (baseType == AnyKind) return true
            return baseType is SchemaAttribute && baseType.name.isEquivalent(name)
        }

        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other == null || this::class != other::class) return false

            other as SchemaAttribute

            return name == other.name
        }

        override fun hashCode(): Int {
            return name.hashCode()
        }

    }

    internal class ProcInstr private constructor(val name: QName?, val text: String?) : NodeKindTest() {
        constructor(): this(null, null)
        constructor(name: QName): this(name, null)
        constructor(text: String): this(null, text)

        override val type: NodeType get() = NodeType.PROCESSING_INSTRUCTION

        context(ctx: ExprEvalContext)
        override fun isSubtypeOf(baseType: ItemTypeTest): Boolean {
            if (baseType == AnyKind) return true
            return baseType is ProcInstr && (baseType.name == name)
        }

        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other == null || this::class != other::class) return false

            other as ProcInstr

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

    internal object Comment : NodeKindTest() {
        override val type: NodeType get() = NodeType.COMMENT

        context(ctx: ExprEvalContext)
        override fun isSubtypeOf(baseType: ItemTypeTest): Boolean =
            baseType == AnyKind || baseType == Comment
    }

    internal object Text : NodeKindTest() {
        override val type: NodeType get() = NodeType.TEXT

        context(ctx: ExprEvalContext)
        override fun isSubtypeOf(baseType: ItemTypeTest): Boolean =
            baseType == AnyKind || baseType == Text
    }

    @NeedsXPath3_0
    internal object NamepaceNode : NodeKindTest() {
        override val type: NodeType get() = NodeType.NAMESPACE_NODE

        context(ctx: ExprEvalContext)
        override fun isSubtypeOf(baseType: ItemTypeTest): Boolean =
            baseType == AnyKind || baseType == NamepaceNode
    }

    internal object AnyKind : NodeKindTest() {
        override val type: NodeType get() = NodeType.ANY_KIND

        override fun eval(it: XdmValue, index: Int, count: Int): Boolean = true

        context(ctx: ExprEvalContext)
        override fun isSubtypeOf(baseType: ItemTypeTest): Boolean {
            return baseType == AnyKind
        }
    }


}
