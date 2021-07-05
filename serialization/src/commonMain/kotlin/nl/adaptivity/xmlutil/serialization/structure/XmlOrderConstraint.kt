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

/**
 * Class that holds an ordering constraint. The [before] attribute is the element Index of the element that is to be
 * ordered before the element at index [after].
 *
 * @property before The element ordered before
 * @property after The element ordered after
 */
public data class XmlOrderConstraint(val before: Int, val after: Int) {
    public inline fun <R> map(transform: (Int) -> R): Pair<R, R> = Pair(transform(before), transform(after))
}
