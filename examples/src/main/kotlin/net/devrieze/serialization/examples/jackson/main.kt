/*
 * Copyright (c) 2020.
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

package net.devrieze.serialization.examples.jackson

import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.serializer
import nl.adaptivity.xmlutil.serialization.XML

fun main() {
    val t = Team(listOf(Person("Joe", 15)))
    val xml = XML {
        policy = JacksonPolicy
    }

    val encodedString = xml.encodeToString(t) // both versions are available
    println("jackson output:\n${encodedString.prependIndent("    ")}\n")

    // the inline reified version is is also available
    val reparsedData = xml.decodeFromString<Team>(encodedString)
    println("jackson input: $reparsedData")

}