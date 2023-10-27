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

package net.devrieze.serialization.examples.soap

import kotlinx.serialization.encodeToString
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.polymorphic
import kotlinx.serialization.serializer
import nl.adaptivity.xmlutil.XmlDeclMode
import nl.adaptivity.xmlutil.serialization.XML
import kotlin.reflect.KClass

/**
 * This is a simple example representing issue #42 on the parsing of soap messages. Note that it doesn't address
 * the idea of making
 */
fun main() {
    val data = Envelope(GeResult(0, GeResultData("get", "p")))

    @Suppress("UNCHECKED_CAST")
    val module = SerializersModule {
        polymorphic(Any::class) {
            subclass(GeResult::class as KClass<GeResult<GeResultData>>, serializer())
        }
    }
    val xml = XML(module) {
        indentString = "    "
        xmlDeclMode = XmlDeclMode.Minimal
        autoPolymorphic = true
    }

    val serializer = serializer<Envelope<GeResult<GeResultData>>>()
    println("SOAP descriptor:\n${xml.xmlDescriptor(serializer).toString().prependIndent("    ")}")

    val encodedString = xml.encodeToString(/*serializer, */data) // both versions are available
    println("SOAP output:\n${encodedString.prependIndent("    ")}\n")

    // the inline reified version is also available
    val reparsedData = xml.decodeFromString(serializer, encodedString)
    println("SOAP input: $reparsedData")
}
