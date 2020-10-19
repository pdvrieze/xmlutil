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

package net.devrieze.serialization.examples.dynamictagnames

import kotlinx.serialization.serializer
import nl.adaptivity.xmlutil.serialization.XML

/**
 * This example shows how a custom serializer together with a filter can be used to support non-xml xml documents
 * where tag names are dynamic/unique. This example is a solution to the question in #41
 */
fun main() {
    val testElements = listOf(
        TestElement(123, "someData"),
        TestElement(456, "moreData")
                             )

    compat(testElements)
    println()
    println("non-compat")
    newExample(testElements)
}

private fun compat(testElements: List<TestElement>) {
    val data = Container(testElements)
    val serializer = CompatContainerSerializer

    val xml = XML {
        indent = 0
    }

    val string = xml.encodeToString(serializer, data)
    println("StringEncodingCompat:\n${string.prependIndent("    ")}")

    val deserializedData = xml.decodeFromString(CompatContainerSerializer, string)
    println("Deserialized container:\n  $deserializedData")
}

/** This example works with master, but not with the released version. */
private fun newExample(testElements: List<TestElement>) {
    val data = Container(testElements)
    val serializer = serializer<Container>()

    val xml = XML {
        indent = 2
    }

    val string = xml.encodeToString(serializer, data)
    println("StringEncodingCompat:\n${string.prependIndent("    ")}")

    val deserializedData = xml.decodeFromString(CompatContainerSerializer, string)
    println("Deserialized container:\n  $deserializedData")

}
