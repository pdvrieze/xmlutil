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

package nl.adaptivity.xmlutil.serialization.impl

import nl.adaptivity.xmlutil.XmlUtilInternal

@XmlUtilInternal
public class OrderMatrix(public val size: Int) {
    private val data: BooleanArray = BooleanArray(size*size)

    private inline operator fun BooleanArray.get(x: Int, y: Int): Boolean {
        return this[x + (y * this@OrderMatrix.size)]
    }

    private inline operator fun BooleanArray.set(x: Int, y: Int, value: Boolean) {
        this[x + (y * this@OrderMatrix.size)] = value
    }

    public fun isOrderedBefore(self: Int, ref:Int) : Boolean {
        require(self in 0 until size)
        require(ref in 0 until size)
        return data[ref, self]
    }

    public fun isOrderedAfter(self: Int, ref:Int) : Boolean {
        require(self in 0 until size)
        require(ref in 0 until size)
        return data[self, ref]
    }

    public fun setOrderedBefore(self: Int, ref: Int) {
        require(self in 0 until size)
        require(ref in 0 until size)
        setOrderedAfterImpl(ref, self)
    }

    public fun setOrderedAfter(self: Int, ref: Int): Boolean {
        require(self in 0 until size)
        require(ref in 0 until size)
        return setOrderedAfterImpl(self, ref)
    }

    private fun setOrderedAfterImpl(self: Int, ref: Int): Boolean {
        return when {
            data[self, ref] -> false
            else -> {
                data[self, ref] = true // self is ordered after ref
                // transitively set order given the existing state
                for (i in 0 until size) {
                    if (data[ref, i]) { // for all items i are ordered before ref
                        setOrderedAfterImpl(self, i) // mark self as ordered after i
                    }
                }
                for (i in 0 until size) {
                    if (data[i, self]) { // for all items i after self
                        setOrderedAfterImpl(i, ref) // mark i as ordered after ref
                    }
                }
                true
            }
        }
    }

    override fun toString(): String = buildString {
        val lblWidth = (size -1).toString().length
        val absent = "".padEnd(2*lblWidth+1,'.')
        (0 until size).joinTo(this, " ", "".padStart(lblWidth+1)) { it.toString().padStart(lblWidth + lblWidth/2+1).padEnd(lblWidth*2+1) }
        for (y in 0 until size) {
            val ys = y.toString().padEnd(lblWidth,'_')
            appendLine()
            val lbl = y.toString()
            for(i in 0..(lblWidth-lbl.length)) append(' ')
            append(lbl).append(' ')
            (0 until size).joinTo(this," ") { x-> if(data[x, y]) "${x.toString().padStart(lblWidth, ' ')}>$ys" else absent}
        }
    }

}
