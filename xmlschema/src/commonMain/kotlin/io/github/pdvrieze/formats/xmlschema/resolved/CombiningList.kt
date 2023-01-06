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

package io.github.pdvrieze.formats.xmlschema.resolved

/** List that presents multiple part lists as a single whole */
class CombiningList<T>(private vararg val parts: List<T>): AbstractList<T>() {
    override val size: Int
        get() = parts.sumOf { it.size }

    override fun get(index: Int): T {
        var start = 0
        var listIdx = 0
        while (listIdx<parts.size && (start+parts[listIdx].size) <= index) {
            start+=parts[listIdx].size
            ++listIdx
        }
        if (listIdx>=parts.size) throw IndexOutOfBoundsException("Index $index not found in combining list")

        return parts[listIdx][index-start]
    }

    override fun contains(element: T): Boolean = parts.any { it.contains(element) }

    override fun containsAll(elements: Collection<T>): Boolean {
        return super.containsAll(elements)
    }

    override fun indexOf(element: T): Int {
        return super.indexOf(element)
    }

    override fun lastIndexOf(element: T): Int {
        return super.lastIndexOf(element)
    }

    override fun iterator(): Iterator<T> {
        return listIterator(0)
    }

    override fun listIterator(index: Int): ListIterator<T> {
        return CombiningListIterator(index)
    }

    private inner class CombiningListIterator(
        startIndex: Int
    ) : ListIterator<T> {
        private var currentListIdx: Int = 0
        private var currentEntryIdx: Int = 0
        private var currentListStart: Int = 0

        init {
            while (currentListIdx<parts.size && (currentListStart+parts[currentListIdx].size) <= startIndex) {
                currentListStart+=parts[currentListIdx].size
                ++currentListIdx
            }
            currentEntryIdx = startIndex - currentListStart
        }

        override fun hasNext(): Boolean {
            if (currentEntryIdx + 1 < parts[currentListIdx].size) return true
            var nextListIdx = currentListIdx + 1
            while (nextListIdx < parts.size && parts[nextListIdx].isEmpty()) {
                ++nextListIdx
            }
            return nextListIdx < parts.size
        }

        override fun hasPrevious(): Boolean {
            return (currentListStart + currentEntryIdx) > 0
        }

        override fun nextIndex(): Int {
            return currentListStart + currentEntryIdx + 1
        }

        override fun previousIndex(): Int {
            return currentListStart + currentEntryIdx - 1
        }

        override fun next(): T {
            ++currentEntryIdx
            while (currentEntryIdx >= parts[currentListIdx].size) {
                currentEntryIdx = 0
                currentListStart += parts[currentListIdx].size
                ++currentListIdx
                if (currentListIdx>=parts.size) throw NoSuchElementException("Iterating beyond end of combining list")
            }
            return parts[currentListIdx][currentEntryIdx]
        }

        override fun previous(): T {
            --currentEntryIdx
            while (currentEntryIdx<0) {
                --currentListIdx
                if (currentListIdx<0) throw NoSuchElementException("Iterating before start of combining list")

                val listSize = parts[currentListIdx].size
                currentListStart-= listSize
                currentEntryIdx = listSize -1
            }
            return parts[currentListIdx][currentEntryIdx]
        }
    }
}
