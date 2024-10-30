/*
 * Copyright (c) 2024.
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

package net.devrieze.serialization.examples.typedUnion

import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.polymorphic
import kotlinx.serialization.modules.subclass
import nl.adaptivity.xmlutil.XmlDeclMode
import nl.adaptivity.xmlutil.serialization.XML


@Serializable
sealed interface Response<out T> {

    @Serializable
    data class Success<T>(val data: T) : Response<T>

    @Serializable
    data class Error(val message: String): Response<Nothing>
}

fun main() {
    val xml = XML(SerializersModule {
        polymorphic(Any::class) {
            subclass(String.serializer())
        }
    }) {
        recommended_0_90_2() {
            xmlDeclMode = XmlDeclMode.None
        }
    }

    println(xml.encodeToString<Response<String>>(Response.Success("Good")))
    println(xml.encodeToString<Response<String>>(Response.Error("Bad")))

    println((xml.decodeFromString<Response<String>>("<Success><xsd:string xmlns:xsd=\"http://www.w3.org/2001/XMLSchema\">Better</xsd:string></Success>") as Response.Success<String>).data)
}
