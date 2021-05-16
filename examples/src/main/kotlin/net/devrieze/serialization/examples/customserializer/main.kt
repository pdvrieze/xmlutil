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

package net.devrieze.serialization.examples.customserializer

import nl.adaptivity.xmlutil.serialization.XML

fun main() {
    val delegateOriginal = MyXmlDelegate("everything is awsome")
    val delegated = XML.encodeToString(delegateOriginal)
    println("The delegated serialized version of $(original) = \n${delegated.prependIndent("    ")}\n")

    val delegateCopy = XML.decodeFromString<MyXmlDelegate>(delegated)
    println("After delegated decoding the copy is: $delegateCopy\n\n")

    val manualOriginal = MyXmlManual("It is even awesomer")

    val manual = XML.encodeToString(manualOriginal)
    println("The manually serialized version of $(original) = \n${manual.prependIndent("    ")}\n")

    val manualCopy = XML.decodeFromString<MyXmlManual>(manual)
    println("After manually decoding the copy is: $manualCopy\n\n")
}