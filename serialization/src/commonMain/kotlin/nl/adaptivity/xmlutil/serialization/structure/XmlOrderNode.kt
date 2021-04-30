/*
 * Copyright (c) 2021.
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

package nl.adaptivity.xmlutil.serialization.structure

import kotlinx.serialization.descriptors.SerialDescriptor
import nl.adaptivity.xmlutil.serialization.OutputKind

class XmlOrderNode(child: Int) {
    val child: Int = child
    val predecessors: MutableList<XmlOrderNode> =
        mutableListOf<XmlOrderNode>()
    val successors: MutableList<XmlOrderNode> =
        mutableListOf<XmlOrderNode>()

    fun addBefore(vararg nodes: XmlOrderNode) {
        for (node in nodes) {
            if (node !in successors) {
                successors.add(node)
                node.addAfter(this)
            }
        }
    }

    fun addAfter(vararg nodes: XmlOrderNode) {
        for (node in nodes) {
            if (node !in predecessors) {
                predecessors.add(node)
                node.addBefore(this)
            }
        }
    }


    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is XmlOrderNode) return false

        if (child != other.child) return false

        return true
    }

    override fun hashCode(): Int {
        return child
    }

    override fun toString(): String {
        return "($child, p=[${predecessors.joinToString { it.child.toString() }}], s=[${successors.joinToString { it.child.toString() }}])"
    }

}

fun List<XmlOrderNode>.flatten(
    serialDescriptor: SerialDescriptor,
    children: List<XmlDescriptor>
                              ): IntArray =
    with(filter { it.predecessors.isEmpty() }) {

        fun Array<XmlOrderNode?>.addTransitive(node: XmlOrderNode) {
            if (get(node.child) == null) {
                set(node.child, node)
                for (next in node.successors) {
                    addTransitive(next)
                }
            }
        }

        val fullNodes =
            arrayOfNulls<XmlOrderNode>(serialDescriptor.elementsCount)
        for (node in this) {
            fullNodes.addTransitive(node)
        }
        val unOrderedNodes = mutableListOf<XmlOrderNode>()
        for (i in fullNodes.indices) {
            if (fullNodes[i] == null) {
                val node = XmlOrderNode(i)
                fullNodes[i] = node
                unOrderedNodes.add(node)
            }
        }

        val (attributeHeadNodes, memberHeadNodes) = partition { children[it.child].outputKind == OutputKind.Attribute }
        val (unorderedAttributes, unorderedMembers) = unOrderedNodes.partition { children[it.child].outputKind == OutputKind.Attribute }

        val finalToDeclMap =
            IntArray(serialDescriptor.elementsCount) { -1 }
        val declToOrderMap =
            IntArray(serialDescriptor.elementsCount) { -1 }
        var nextElemIdx = 0

        for (base in arrayOf(
            attributeHeadNodes + unorderedAttributes,
            memberHeadNodes + unorderedMembers
                            )) {
            val queue = base.toMutableList()
//                .apply { sortBy { it.child } }
            while (queue.isNotEmpty()) {
                val nextIdx = queue.indexOfMinBy {
                    // In the case that the predecessors are not sorted yet
                    if (it.predecessors.any { declToOrderMap[it.child] < 0 }) {
                        serialDescriptor.elementsCount // Put at end of queue
                    } else {
                        it.child
                    }
                }
                val next = queue.removeAt(nextIdx)
                finalToDeclMap[nextElemIdx] = next.child
                declToOrderMap[next.child] = nextElemIdx
                nextElemIdx++
                for (successor in next.successors) {
                    if (successor !in queue) {
                        queue.add(successor)
                    }
                }
            }
        }


        declToOrderMap
    }

private inline fun <E, R : Comparable<R>> List<E>.indexOfMinBy(selector: (E) -> R): Int {
    if (size == 0) return -1
    if (size == 1) return 0
    var idx = 1
    var minIdx = 0
    var minValue = selector(get(0))
    do {
        val v = selector(get(idx))
        if (minValue > v) {
            minIdx = idx
            minValue = v
        }
        idx++
    } while (idx < size)
    return minIdx
}
