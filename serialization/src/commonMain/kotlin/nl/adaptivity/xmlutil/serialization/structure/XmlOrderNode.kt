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

internal class XmlOrderNode(elementIdx: Int) {
    val elementIdx: Int = elementIdx
    val predecessors: MutableList<XmlOrderNode> =
        mutableListOf<XmlOrderNode>()
    val successors: MutableList<XmlOrderNode> =
        mutableListOf<XmlOrderNode>()

    fun addSuccessors(vararg nodes: XmlOrderNode) {
        for (node in nodes) {
            if (node !in successors) {
                successors.add(node)
                node.addPredecessors(this)
            }
        }
    }

    fun addPredecessors(vararg nodes: XmlOrderNode) {
        for (node in nodes) {
            if (node !in predecessors) {
                predecessors.add(node)
                node.addSuccessors(this)
            }
        }
    }


    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is XmlOrderNode) return false

        if (elementIdx != other.elementIdx) return false

        return true
    }

    override fun hashCode(): Int {
        return elementIdx
    }

    override fun toString(): String {
        return "($elementIdx, p=[${predecessors.joinToString { it.elementIdx.toString() }}], s=[${successors.joinToString { it.elementIdx.toString() }}])"
    }

}

internal fun Iterable<XmlOrderConstraint>.sequenceStarts(childCount: Int): Collection<XmlOrderNode> {
    val nodes = Array(childCount) { XmlOrderNode(it) }
    forEach { constraint ->
        val (before, after) = constraint.map { nodes[it] }

        before.addSuccessors(after)
    }
    return nodes.filter { it.predecessors.isEmpty() }
}

/**
 * This function creates a list with all the nodes reachable from the receiver in (breath first) order
 */
internal fun XmlOrderNode.flatten(): List<XmlOrderNode> {
    fun XmlOrderNode.lastIndex(): Int {
        var lastIndex = elementIdx
        for (successor in successors) {
            val c = successor.lastIndex()
            if (c > lastIndex) { lastIndex = c }
        }
        return lastIndex
    }

    val seen = BooleanArray(lastIndex()+1)

    fun XmlOrderNode.flattenSuccessorsTo(receiver: MutableList<XmlOrderNode>) {
        val unseenSuccessors = successors.filter { !seen[it.elementIdx] }
        for (successor in unseenSuccessors) {
            receiver.add(successor)
            seen[successor.elementIdx] = true
        }
        for (successor in unseenSuccessors) {
            successor.flattenSuccessorsTo(receiver)
        }
    }

    return mutableListOf<XmlOrderNode>().also {
        it.add(this)
        flattenSuccessorsTo(it)
    }
}

/**
 *
 */
internal fun Collection<XmlOrderNode>.fullFlatten(
    serialDescriptor: SerialDescriptor,
    children: List<XmlDescriptor>
                                        ): IntArray {
    val originalOrderNodes = arrayOfNulls<XmlOrderNode>(serialDescriptor.elementsCount)

    fun addTransitive(node: XmlOrderNode) {
        if (originalOrderNodes[node.elementIdx] == null) {
            originalOrderNodes[node.elementIdx] = node

            for (next in node.successors) {
                addTransitive(next)
            }
        }
    }

    val allNodes = mutableListOf<XmlOrderNode>()

    for (node in asSequence().filter { it.predecessors.isEmpty() }) {
        addTransitive(node)
        allNodes.add(node)
    }

    for (i in originalOrderNodes.indices) {
        if (originalOrderNodes[i] == null) {
            val node = XmlOrderNode(i)
            originalOrderNodes[i] = node
            allNodes.add(node)
        }
    }

//    val (attributeHeadNodes, memberHeadNodes) = headNodes.partition { children[it.elementIdx].outputKind == OutputKind.Attribute }
    val (attributes, members) = allNodes.partition { children[it.elementIdx].outputKind == OutputKind.Attribute }

    val finalToDeclMap =
        IntArray(serialDescriptor.elementsCount) { -1 }
    val declToOrderMap =
        IntArray(serialDescriptor.elementsCount) { -1 }
    var nextElemIdx = 0

    for (base in arrayOf(attributes, members)) {
        val queue = base.toMutableList()
        //                .apply { sortBy { it.child } }
        while (queue.isNotEmpty()) {
            val nextIdx = queue.indexOfMinBy {
                // In the case that the predecessors are not sorted yet
                if (it.predecessors.any { declToOrderMap[it.elementIdx] < 0 }) {
                    serialDescriptor.elementsCount // Put at end of queue
                } else {
                    it.elementIdx
                }
            }
            val next = queue.removeAt(nextIdx)
            finalToDeclMap[nextElemIdx] = next.elementIdx
            declToOrderMap[next.elementIdx] = nextElemIdx
            nextElemIdx++
            for (successor in next.successors) {
                if (successor !in queue) {
                    queue.add(successor)
                }
            }
        }
    }


    return declToOrderMap
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
