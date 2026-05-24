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

package nl.adaptivity.xmlutil.dom2.impl

import nl.adaptivity.xmlutil.ExperimentalXmlUtilApi
import nl.adaptivity.xmlutil.dom2.nextSibling

@ExperimentalXmlUtilApi
public class LinkedNodeList<out N : IAbstractNode<N, P>, out P : IAbstractParentNode<N, P>>(private val head: N) :
    AbstractNodeList<N, P> {

    private var _size = -1
    override fun getLength(): Int {
        if (_size >= 0) return _size
        val s = iterator().asSequence().count()
        _size = s
        return s
    }

    override fun iterator(): Iterator<N> {
        return SiblingIterator<N, P>(head)
    }
}

internal class SiblingIterator<out N : IAbstractNode<N, P>, out P : IAbstractParentNode<N, P>>(private var current: N?) : Iterator<N> {
    override fun hasNext(): Boolean = current != null

    override fun next(): N {
        return (current ?: throw NoSuchElementException()).also {
            @Suppress("UNCHECKED_CAST")
            current = it.nextSibling as N
        }
    }
}
