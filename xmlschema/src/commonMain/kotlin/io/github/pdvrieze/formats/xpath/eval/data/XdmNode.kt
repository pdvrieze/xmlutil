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

package io.github.pdvrieze.formats.xpath.eval.data

import io.github.pdvrieze.formats.xpath.eval.ErrorCodes
import io.github.pdvrieze.formats.xpath.eval.EvaluationException
import io.github.pdvrieze.formats.xpath.eval.type.XdmSchemaType
import io.github.pdvrieze.formats.xpath.eval.type.XdmSingleType
import io.github.pdvrieze.formats.xpath.eval.typeTest.XdmNodeKindTest
import io.github.pdvrieze.formats.xpath.eval.typeTest.XdmTypeTest
import io.github.pdvrieze.formats.xpath.functions.Fn
import io.github.pdvrieze.formats.xpath.impl.ExprEvalContext
import io.github.pdvrieze.formats.xpath.impl.NeedsXPath2
import io.github.pdvrieze.formats.xpath.impl.NeedsXPath3_0
import io.github.pdvrieze.formats.xpath.impl.XPathInternal
import io.github.pdvrieze.xml.schematypes.types.AnyAtomicType
import io.github.pdvrieze.xml.schematypes.types.UntypedType
import io.github.pdvrieze.xml.schematypes.values.XsdAtomic
import io.github.pdvrieze.xml.schematypes.values.XsdBoolean
import io.github.pdvrieze.xml.schematypes.values.XsdString
import nl.adaptivity.xmlutil.XMLConstants
import nl.adaptivity.xmlutil.dom2.*

@XPathInternal
@OptIn(NeedsXPath2::class)
class XdmNode(
    val node: Node,
    override val staticType: XdmSingleType = XdmSchemaType(UntypedType.Instance)
) : XdmSingleValue<XdmNode>() {
    val posSeq: IntArray

    init {
        val depth = generateSequence(node) { it.parentNode as? Element }.count()
        posSeq = IntArray(depth -1) //skip the document element
        if (depth >1) { // we don't position the root node
            var i = depth - 2
            // skip 0 as it is
            var n: Node? = node
            do {
                posSeq[i] = generateSequence(n) { it.previousSibling }.count()-1
                n = n!!.getParentNode() as? Element
                i -= 1
            } while (i >= 0)
        }
    }

    // TODO actually use schema types for this
    override val dynamicType: XdmSingleType get() = staticType

    override fun asT(): XdmNode = this

    context(ctx: ExprEvalContext)
    fun typedValue(): XdmValue = when (node) {
        is Attr -> staticType.fromString(node.value)
        else -> throw EvaluationException(ctx.expr, "Node has no value")
    }


    fun descendantsSequence(): Sequence<XdmNode> {
        return sequence {
            if (node is Element || node is Document) {
                for (c in node.getChildNodes()) {
                    val value = XdmNode(c)
                    yield(value)
                    yieldAll(value.descendantsSequence())
                }
            }
        }
    }

    context(ctx: ExprEvalContext)
    override fun toBoolean(): Boolean {
        return false
//        if (type.isSubtypeOf(BooleanType.Instance))
    }

    @OptIn(NeedsXPath3_0::class)
    context(ctx: ExprEvalContext)
    override fun treatAsNonEmpty(type: XdmTypeTest): XdmValue {
        if (type !is XdmNodeKindTest) throw EvaluationException(ErrorCodes.XPDY0050_INVALID_TYPE_IN_TREAT_AS, "Cannot cast node to $type")
        if (! type.nodeKind.matches(node)) throw EvaluationException(ErrorCodes.XPDY0050_INVALID_TYPE_IN_TREAT_AS, "Cannot cast $node to (${type.nodeKind})")
        // TODO do some checks
        return XdmNode(node, type.toValueType(staticType).single)
    }

    override fun hashCode(): Int {
        return node.hashCode()
    }

    override fun equals(other: Any?): Boolean {
        return node == (other as? XdmNode)?.node
    }

    override fun isValEqual(expected: XdmValue): Boolean {
        return equals(expected)
    }

    context(ctx: ExprEvalContext)
    override fun atomizeTo(receiver: MutableList<in XdmAtomic<XsdAtomic>>) {
        val a = atomize()
        if (a is XdmAtomic<*>) receiver.add(a)
    }

    context(ctx: ExprEvalContext)
    override fun atomize(): XdmAtomicOrSequence<XdmAtomic<XsdAtomic>> {
        return when (node) {
            is Attr -> (staticType).fromString(node.value) as XdmAtomic<*>
            is ProcessingInstruction -> XdmAtomic(XsdString(node.getData()))
            is Comment -> XdmAtomic(XsdString(node.getData()))
            is Text -> XdmAtomic(XsdString(node.getData()))
            is Element if node.isNil -> XdmSequence.empty(staticType)

            is Document -> Fn.string(this).atomize() as XdmAtomic<*>

            is Element if staticType.isAssignableTo(AnyAtomicType.Instance) ->
                staticType.fromString(node.textContent ?: "") as XdmAtomic<*>
            is Element if dynamicType == XdmSchemaType.UNTYPED -> Fn.string(this).atomize() as XdmAtomic<*>
            is Element -> throw EvaluationException(ErrorCodes.FOTY0012,"Cannot atomize a typed element to non-atomic type yet")
            else -> throw UnsupportedOperationException("Unsupported node type: ${node.getNodetype()}")
        }

        // TODO add check that the value is not "typed" (there is an actual value in the node)
        // otherwise throw FOTY0012
    }


    context(ctx: ExprEvalContext)
    @XPathInternal
    override fun normalizeToArithmetic(): XdmValue = when (node) {
        is Attr -> dynamicType.fromString(node.value)
        else -> super.normalizeToArithmetic()
    }

    override fun toString(): String {
        return buildString {
            append("XdmNode(")

            posSeq.joinTo(this, ",", "[", "], ") { it.toString() }

            append("type=").append(dynamicType).append(", ")
            append("node=").append(node).append(", ")
            append("hashcode=").append(hashCode().toString(16))
            append(")")
        }
    }

    companion object {
        val DOCUMENT_ORDER: Comparator<XdmNode> = object : Comparator<XdmNode> {
            fun compare(a: Node, b: Node): Int {
                if (a === b) return 0

                // we include the node itself
                val ancestorsA = generateSequence(a) { it.parentNode as? Element }.toList().reversed()
                val ancestorsB = generateSequence(b) { it.parentNode as? Element }.toList().reversed()
                if (a in ancestorsB) return -1 // must be before
                if (b in ancestorsA) return 1 // must be after

                if (ancestorsA[0] != ancestorsB[0])
                    throw IllegalArgumentException("Nodes have no common ancestor, and thus no document order")

                // loop through all ancestors (not considering the actual node)
                for (i in 1 until minOf(ancestorsA.size, ancestorsB.size)) {
                    if (ancestorsA[i] != ancestorsB[i]) {
                        // We look in the previous siblings of the ancestors to optimize for already sorted
                        var x = ancestorsB[i].previousSibling
                        while (x != null) {
                            if (x === ancestorsA[i]) return -1
                            x = x.previousSibling
                        }
                        return 1
                    }
                }
                // should not happen as this implies the list is a subset
                return ancestorsB.size - ancestorsA.size
            }

            override fun compare(a: XdmNode, b: XdmNode): Int {
                return compare(a.node, b.node)
            }
        }
    }


}

internal val Element.isNil: Boolean get() {
    val attrValue = getAttributeNS(XMLConstants.XSI_NS_URI, "nil") ?: return false
    if (attrValue.isEmpty()) return false
    return XsdBoolean(attrValue).value
}
