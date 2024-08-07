/*
 * Copyright (c) 2023.
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

package io.github.pdvrieze.formats.xmlschema.regex.impl

import kotlin.AssertionError

/** Basic class for sets which have no complex next node handling. */
internal abstract class XRSimpleSet : XRAbstractSet {
    override var next: XRAbstractSet = dummyNext
    constructor()
    constructor(type : Int): super(type)
}

/**
 * Basic class for nodes, representing given regular expression.
 * Note: (Almost) All the classes representing nodes has 'set' suffix.
 */
internal abstract class XRAbstractSet(val type: Int = 0) {

    companion object {
        const val TYPE_LEAF = 1 shl 0
        const val TYPE_FSET = 1 shl 1
        const val TYPE_QUANT = 1 shl 3
        @Suppress("DEPRECATION")
        const val TYPE_DOTSET = 0x80000000.toInt() or '.'.toInt()

        val dummyNext = object : XRAbstractSet() {
            override var next: XRAbstractSet
                get() = throw AssertionError("This method is not expected to be called.")
                @Suppress("UNUSED_PARAMETER")
                set(value) {}
            override fun matches(startIndex: Int, testString: CharSequence, matchResult: XRMatchResultImpl) =
                throw AssertionError("This method is not expected to be called.")
            override fun hasConsumed(matchResult: XRMatchResultImpl): Boolean =
                throw AssertionError("This method is not expected to be called.")
            override fun processSecondPassInternal(): XRAbstractSet = this
            override fun processSecondPass(): XRAbstractSet = this
        }
    }

    var secondPassVisited = false
    abstract var next: XRAbstractSet

    protected open val name: String
        get() = ""

    /**
     * Checks if this node matches in given position and recursively call
     * next node matches on positive self match. Returns positive integer if
     * entire match succeed, negative otherwise.
     * @param startIndex - string index to start from.
     * @param testString  - input string.
     * @param matchResult - MatchResult to sore result into.
     * @return -1 if match fails or n > 0;
     */
    abstract fun matches(startIndex: Int, testString: CharSequence, matchResult: XRMatchResultImpl): Int

    /**
     * Attempts to apply pattern starting from this set/startIndex; returns
     * index this search was started from, if value is negative, this means that
     * this search didn't succeed, additional information could be obtained via
     * matchResult.
     *
     * Note: this is default implementation for find method, it's based on
     * matches, subclasses do not have to override find method unless
     * more effective find method exists for a particular node type
     * (sequence, i.e. substring, for example). Same applies for find back
     * method.
     *
     * @param startIndex - starting index.
     * @param testString - string to search in.
     * @param matchResult - result of the match.
     * @return last searched index.
     */
    open fun find(startIndex: Int, testString: CharSequence, matchResult: XRMatchResultImpl): Int
        = (startIndex..testString.length).firstOrNull { index -> matches(index, testString, matchResult) >= 0 }
          ?: -1

    /**
     * @param leftLimit - an index, to finish search back (left limit).
     * @param rightLimit - an index to start search from (right limit).
     * @param testString - test string.
     * @param matchResult - match result.
     * @return an index to start back search next time if this search fails(new left bound);
     *         if this search fails the value is negative.
     */
    open fun findBack(leftLimit: Int, rightLimit: Int, testString: CharSequence, matchResult: XRMatchResultImpl): Int
        = (rightLimit downTo leftLimit).firstOrNull { index -> matches(index, testString, matchResult) >= 0 }
          ?: -1

    /**
     * Returns `true` if this node consumes a constant number of characters and doesn't need backtracking to find a different match.
     * Otherwise, returns `false`.
     *
     * This information is used to avoid recursion when matching a quantifier node with this inner node.
     */
    open val consumesFixedLength: Boolean
        get() = false

    /**
     * Returns true, if this node has consumed any characters during
     * positive match attempt, for example node representing character always
     * consumes one character if it matches. If particular node matches
     * empty sting this method will return false.
     *
     * @param matchResult - match result;
     * @return true if the node consumes any character and false otherwise.
     */
    abstract fun hasConsumed(matchResult: XRMatchResultImpl): Boolean

    /**
     * Returns true if the given node intersects with this one, false otherwise.
     * This method is being used for quantifiers construction, lets consider the
     * following regular expression (a|b)*ccc. (a|b) does not intersects with "ccc"
     * and thus can be quantified greedily (w/o kickbacks), like *+ instead of *.

     * @param set - A node the intersection is checked for. Usually a previous node.
     * @return true if the given node intersects with this one, false otherwise.
     */
    open fun first(set: XRAbstractSet): Boolean = true

    /**
     * This method is used for replacement backreferenced sets.
     *
     * @return null if current node need not to be replaced,
     *         [JointSet] which is replacement of current node otherwise.
     */
    open fun processBackRefReplacement(): XRJointSet? {
        return null
    }

    /**
     * This method performs the second pass without checking if it's already performed or not.
     */
    protected open fun processSecondPassInternal(): XRAbstractSet {
        if (!next.secondPassVisited) {
            this.next = next.processSecondPass()
        }
        return processBackRefReplacement() ?: this
    }

    /**
     * This method is used for traversing nodes after the first stage of compilation.
     */
    open fun processSecondPass(): XRAbstractSet {
        secondPassVisited = true
        return processSecondPassInternal()
    }
}
