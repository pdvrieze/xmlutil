/*
 * Copyright (c) 2018.
 *
 * This file is part of XmlUtil.
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

package nl.adaptivity.xmlutil.impl

internal class CharArraySequence(private val data: CharArray,
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