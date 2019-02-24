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

package nl.adaptivity.xmlutil.multiplatform

expect class SimpleQueue<E>() {

    val size: Int

    fun peekFirst(): E?
    fun peekLast(): E?

    fun removeFirst(): E
    fun removeLast(): E

    fun addLast(e:E)
    fun add(element: E): Boolean

    fun clear()
}

fun SimpleQueue<*>.isNotEmpty() = size > 0
fun <E> SimpleQueue<E>.addAll(elements: Iterable<E>):Boolean {
    return elements.fold(false) { acc, e ->
        acc or add(e)
    }
}