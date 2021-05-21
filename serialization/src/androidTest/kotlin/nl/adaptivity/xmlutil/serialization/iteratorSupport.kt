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

package nl.adaptivity.xmlutil.serialization

import org.w3c.dom.NamedNodeMap
import org.w3c.dom.Node
import org.w3c.dom.NodeList


internal operator fun NodeList.iterator() = object : Iterator<Node> {
    val list = this@iterator
    private var nextIdx = 0

    override fun hasNext() = nextIdx < list.length

    override fun next(): Node = list.item(nextIdx++)
}

internal operator fun NamedNodeMap.iterator() = object : Iterator<Node> {
    private val map = this@iterator
    private var nextIdx = 0

    override fun hasNext(): Boolean = nextIdx < map.length

    override fun next(): Node {
        val idx = nextIdx++ // update nextIdx afterwards
        return map.item(idx) as Node
    }

}
