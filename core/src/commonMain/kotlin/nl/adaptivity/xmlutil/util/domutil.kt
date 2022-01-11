/*
 * Copyright (c) 2022.
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

package nl.adaptivity.xmlutil.util

import nl.adaptivity.xmlutil.dom.Node
import nl.adaptivity.xmlutil.dom.NodeList
import nl.adaptivity.xmlutil.dom.NamedNodeMap
import nl.adaptivity.xmlutil.dom.iterator as actualIteratorImpl

@Deprecated("Use the implementation in the dom package", ReplaceWith("iterator", "nl.adaptivity.xmlutil.dom.iterator"), DeprecationLevel.HIDDEN)
public operator fun NodeList.iterator(): Iterator<Node> {
    return actualIteratorImpl()
}

@Deprecated("Use the implementation in the dom package", ReplaceWith("iterator", "nl.adaptivity.xmlutil.dom.iterator"), DeprecationLevel.HIDDEN)
public operator fun NamedNodeMap.iterator(): Iterator<Node> {
    return actualIteratorImpl()
}
