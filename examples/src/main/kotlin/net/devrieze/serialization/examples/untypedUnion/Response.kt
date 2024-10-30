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

package net.devrieze.serialization.examples.untypedUnion

import kotlinx.serialization.*
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.encoding.decodeStructure
import kotlinx.serialization.encoding.encodeStructure
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.polymorphic
import kotlinx.serialization.modules.subclass
import nl.adaptivity.xmlutil.XmlDeclMode
import nl.adaptivity.xmlutil.serialization.XML
import kotlinx.serialization.serializer as kserializer


@Serializable(ResponseSerializer::class)
sealed interface Response {

    data class Success<T>(val delegateSerializer: KSerializer<T>, val data: T) : Response {
        companion object {
            inline operator fun <reified T> invoke(data: T): Success<T> {
                return Success<T>(kserializer(), data)
            }
        }
    }

    @Serializable
    data class Error(val message: String): Response
}

object ResponseSerializer : KSerializer<Response> {
    val errorSer = Response.Error.serializer()

    @OptIn(InternalSerializationApi::class)
    // Use a simimilar descriptor to PolymorphicSerializer
    override val descriptor: SerialDescriptor =
        /*SerialDescriptor("org.example.Response",*/ PolymorphicSerializer(Any::class).descriptor//)

    override fun deserialize(decoder: Decoder): Response {
        return decoder.decodeStructure(descriptor) {
            val idx = decodeElementIndex(descriptor)
            check (idx == 0)
            when(val type = decodeStringElement(descriptor, 0)) {
                errorSer.descriptor.serialName -> {
                    check(decodeElementIndex(descriptor) == 1)
                    decodeSerializableElement(descriptor, 1, errorSer)
                }
                else -> {
                    val childSer = serializersModule.getPolymorphic(Any::class, serializedClassName = type)
                    val data = decodeSerializableElement(descriptor, 1, childSer as KSerializer<Any>)
                    Response.Success(childSer, data)
                }
            }
        }
    }

    override fun serialize(encoder: Encoder, value: Response) {
        encoder.encodeStructure(descriptor) {
            when (value) {
                is Response.Error -> {
                    encodeStringElement(descriptor, 0, errorSer.descriptor.serialName)
                    encodeSerializableElement(descriptor, 1, errorSer, value)
                }
                is Response.Success<*> -> {
                    encodeStringElement(descriptor, 0, value.delegateSerializer.descriptor.serialName)
                    // Cast is needed here due to type limits
                    encodeSerializableElement(descriptor, 1, value.delegateSerializer as KSerializer<Any?>, value.data)
                }
            }
        }
    }
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

    println(xml.encodeToString<Response>(Response.Success(String.serializer(),"Good")))
    println((xml.decodeFromString<Response>("<xsd:string xmlns:xsd=\"http://www.w3.org/2001/XMLSchema\">Better</xsd:string>") as Response.Success<String>).data)
}
