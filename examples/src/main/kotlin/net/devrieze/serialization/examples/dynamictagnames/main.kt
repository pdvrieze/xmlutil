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

import kotlinx.serialization.UseSerializers
import kotlinx.serialization.serializer
import nl.adaptivity.xmlutil.serialization.XML

/**
 * This example shows how a custom serializer together with a filter can be used to support non-xml xml documents
 * where tag names are dynamic/unique. This example is a solution to the question in #41.
 *
 * There are 2 versions, one is the CompatContainerSerializer. This version works on 0.80.0 and 0.80.1 but has
 * limitations in that it cannot inherit configuration or serializerModules. The improved version uses new properties
 * in the XML.XmlInput and XML.XmlOutput interfaces that allow new xml serializers to be created based on the
 * configuration of the encoder/decoder.
 */
fun main() {
    /*
     * Some test data that is used for both versions of the serializer.
     */
    val testElements = listOf(
        TestElement(123, 42, "someData"),
        TestElement(456, 71, "moreData")
                             )

    // Execute the example code for the compatible serializer
    println("# Compatible")
    compat(testElements)

    // Execute the example code for the improved serializer
    println()
    println("# Improved version")
    newExample(testElements)
}

private fun compat(testElements: List<TestElement>) {
    val data = Container(testElements)

    // Instead of using the serializer for the type we use the custom one. In normal cases there would only be one
    // serializer
    val serializer = CompatContainerSerializer

    /*
     * Set an indent here to show that it is not effective (as the serialization of the child does not have access to
     * the configuration).
     */
    val xml = XML { indent = 2 }

    // Encode and print the output of serialization
    val string = xml.encodeToString(serializer, data)
    println("StringEncodingCompat:\n${string.prependIndent("    ")}")

    // Parse and print the result of deserialization
    val deserializedData = xml.decodeFromString(serializer, string)
    println("Deserialized container:\n  $deserializedData")
}

/** This example works with master, but not with the released version. */
private fun newExample(testElements: List<TestElement>) {
    val data = Container(testElements)
    val serializer = serializer<Container>() // use the default serializer

    // Create the configuration for (de)serialization
    val xml = XML { indent = 2 }

    // Encode and print the output of serialization
    val string = xml.encodeToString(serializer, data)
    println("StringEncodingCompat:\n${string.prependIndent("    ")}")

    // Parse and print the result of deserialization
    val deserializedData = xml.decodeFromString(serializer, string)
    println("Deserialized container:\n  $deserializedData")

}
