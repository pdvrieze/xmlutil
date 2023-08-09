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

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.descriptors.SerialDescriptor
import nl.adaptivity.xmlutil.serialization.OutputKind
import nl.adaptivity.xmlutil.serialization.structure.XmlOrderNode.OrderWildcard.*

internal class XmlOrderNode(val elementIdx: Int) {
    val predecessors: MutableList<XmlOrderNode> =
        mutableListOf()
    val successors: MutableList<XmlOrderNode> =
        mutableListOf()
    var wildCard : OrderWildcard = NONE

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


    override fun toString(): String {
        return "($elementIdx, p=[${predecessors.joinToString { it.elementIdx.toString() }}], s=[${successors.joinToString { it.elementIdx.toString() }}])"
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || this::class != other::class) return false

        other as XmlOrderNode

        if (elementIdx != other.elementIdx) return false
        if (predecessors != other.predecessors) return false
        if (successors != other.successors) return false
        return wildCard == other.wildCard
    }

    override fun hashCode(): Int {
        var result = elementIdx
        result = 31 * result + predecessors.hashCode()
        result = 31 * result + successors.hashCode()
        result = 31 * result + wildCard.hashCode()
        return result
    }

    enum class OrderWildcard {
        BEFORE, NONE, AFTER
    }

}

internal fun Iterable<XmlOrderConstraint>.sequenceStarts(childCount: Int): Collection<XmlOrderNode> {
    // Nodes with wildcard before are in a group that always goes first.
    val beforeAny = BooleanArray(childCount)
    var hasWildCard = false
    val afterAny = BooleanArray(childCount)
    val nodes = Array(childCount) { XmlOrderNode(it) }

    forEach { constraint ->
        if (constraint.after == XmlOrderConstraint.OTHERS) {
            hasWildCard = true
            beforeAny[constraint.before] = true
        } else if (constraint.before == XmlOrderConstraint.OTHERS) {
            hasWildCard = true
            afterAny[constraint.after] = true
        } else  {
            val (before, after) = constraint.map { nodes[it] }

            before.addSuccessors(after)
            after.addPredecessors(before)
        }
    }

    if (hasWildCard) {
        for (idx in beforeAny.indices) {
            if(beforeAny[idx]) {
                nodes[idx].wildCard = BEFORE
            } else if(afterAny[idx]) {
                nodes[idx].wildCard = AFTER
            }
        }

        run { // If a node is before a wildcard node it is also a wildcard node.
            val beforesToCheck = ArrayDeque<Int>()
            beforesToCheck.addAll(beforeAny.indices.filter { beforeAny[it] })

            while (beforesToCheck.isNotEmpty()) {
                val idx = beforesToCheck.removeFirst()
                nodes[idx].predecessors.asSequence().filter { !beforeAny[it.elementIdx] }
                    .forEach { pred ->
                        pred.wildCard = BEFORE
                        beforesToCheck.add(pred.elementIdx)
                    }
            }
        }

        run { // If a node is after a wildcard node it is also a wildcard node
            val aftersToCheck = ArrayDeque<Int>()
            aftersToCheck.addAll(afterAny.indices.filter { afterAny[it] })

            while (aftersToCheck.isNotEmpty()) {
                val idx = aftersToCheck.removeFirst()
                nodes[idx].successors.asSequence().filter { !afterAny[it.elementIdx] }
                    .forEach { succ ->
                        succ.wildCard = AFTER
                        aftersToCheck.add(succ.elementIdx)
                    }
            }
        }
        /*

                for (primary in beforeAny.indices) {
                    for (secondary in nodes.indices) {
                        if (primary != secondary) {
                            if (beforeAny[primary] && !beforeAny[secondary]) {
                                nodes[primary].addSuccessors(nodes[secondary])
                                nodes[secondary].addPredecessors(nodes[primary])
                            } else if (afterAny[primary] && !afterAny[secondary]) {
                                nodes[primary].addPredecessors(nodes[secondary])
                                nodes[secondary].addSuccessors(nodes[primary])
                            }
                        }
                    }
                }
        */
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
            if (c > lastIndex) lastIndex = c
        }
        return lastIndex
    }

    val seen = BooleanArray(lastIndex() + 1)

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
@OptIn(ExperimentalSerializationApi::class)
internal fun Collection<XmlOrderNode>.fullFlatten(
    serialDescriptor: SerialDescriptor,
    children: List<XmlDescriptor>
): Pair<Collection<XmlOrderConstraint>, IntArray> {
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

    val constraints = mutableListOf<XmlOrderConstraint>()
    for (attrOrMembers in arrayOf(attributes, members)) {
        // After having split into different output kinds, then split by wildcard.
        val before = mutableListOf<XmlOrderNode>()
        val general = mutableListOf<XmlOrderNode>()
        val after = mutableListOf<XmlOrderNode>()
        for (node in attrOrMembers) {
            when (node.wildCard) {
                BEFORE -> before.add(node)
                NONE -> general.add(node)
                AFTER -> after.add(node)
            }
        }
        for (node in before) {
            constraints.add(XmlOrderConstraint(node.elementIdx, XmlOrderConstraint.OTHERS))
        }
        for (node in after) {
            constraints.add(XmlOrderConstraint(XmlOrderConstraint.OTHERS, node.elementIdx))
        }

        for (base in arrayOf(before, general, after)) {

            val queue = base.toMutableList()
            //                .apply { sortBy { it.child } }
            while (queue.isNotEmpty()) {
                val nextIdx = queue.indexOfMinBy { node ->
                    // In the case that the predecessors are not sorted yet
                    if (node.predecessors.any { pred -> declToOrderMap[pred.elementIdx] < 0 }) {
                        serialDescriptor.elementsCount // Order as if at end of queue
                    } else {
                        node.elementIdx
                    }
                }
                val next = queue.removeAt(nextIdx)
                finalToDeclMap[nextElemIdx] = next.elementIdx
                declToOrderMap[next.elementIdx] = nextElemIdx
                nextElemIdx++
                for (successor in next.successors) {
                    if (successor in base) { // This ensures 2*3 independent partitions
                        constraints.add(XmlOrderConstraint(next.elementIdx, successor.elementIdx))
                    }
                    if (successor !in queue) {
                        queue.add(successor)
                    }
                }
            }
        }
    }


    return constraints to declToOrderMap
}

private inline fun <E, R : Comparable<R>> List<E>.indexOfMinBy(selector: (E) -> R): Int {
    if (isEmpty()) return -1
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
