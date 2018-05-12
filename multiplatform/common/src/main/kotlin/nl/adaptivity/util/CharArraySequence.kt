/*
 * Copyright (c) 2018.
 *
 * This file is part of ProcessManager.
 *
 * ProcessManager is free software: you can redistribute it and/or modify it under the terms of version 3 of the
 * GNU Lesser General Public License as published by the Free Software Foundation.
 *
 * ProcessManager is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even
 * the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License along with ProcessManager.  If not,
 * see <http://www.gnu.org/licenses/>.
 */

package nl.adaptivity.util

class CharArraySequence(private val data: CharArray,
                                 private val offset: Int = 0,
                                 override val length: Int = data.size - offset) : CharSequence {

    override fun get(index: Int): Char {
        if (index < 0 || index >= (offset + length)) throw IndexOutOfBoundsException("$index")
        return data[offset + index]
    }

    override fun subSequence(startIndex: Int, endIndex: Int): CharSequence {
        if (startIndex < 0 || startIndex > length) throw IndexOutOfBoundsException("startIndex: $startIndex")
        if (endIndex < startIndex || endIndex > length) throw IndexOutOfBoundsException("endIndex: $endIndex")
        return CharArraySequence(data, offset + startIndex, endIndex - startIndex)
    }

    override fun toString(): String {
        return StringBuilder(this).toString()
    }
}