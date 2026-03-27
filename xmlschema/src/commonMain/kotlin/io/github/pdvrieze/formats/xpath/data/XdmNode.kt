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

package io.github.pdvrieze.formats.xpath.data

import io.github.pdvrieze.formats.xpath.impl.ExprEvalContext
import io.github.pdvrieze.formats.xpath.impl.NeedsXPath2
import io.github.pdvrieze.formats.xpath.impl.XPathInternal
import io.github.pdvrieze.xml.schematypes.types.UntypedType
import nl.adaptivity.xmlutil.dom2.*

@XPathInternal
@OptIn(NeedsXPath2::class)
class XdmNode(
    val node: Node,
    override val type: XdmType = XdmSchemaType(UntypedType.Instance)
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

    override fun asT(): XdmNode = this

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

    context(ctx: ExprEvalContext)
    override fun withType(type: XdmType): XdmValue {
        // TODO do some checks
        return XdmNode(node, type)
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
    override fun atomizeTo(receiver: MutableList<in XdmSingleValue<*>>) {
        receiver.add(atomize())
    }

    context(ctx: ExprEvalContext)
    override fun atomize(): XdmNode {
        // TODO add check that the value is not "typed" (there is an actual value in the node)
        // otherwise throw FOTY0012
        return this
    }

    override fun toString(): String {
        return buildString {
            append("XdmNode(")

            posSeq.joinTo(this, ",", "[", "], ") { it.toString() }

            append("type=").append(type).append(", ")
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
