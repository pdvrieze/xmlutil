/*
 * Copyright (c) 2018.
 *
 * This file is part of XmlUtil.
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

package nl.adaptivity.xmlutil

import kotlinx.serialization.*
import kotlinx.serialization.internal.StringSerializer
import nl.adaptivity.xmlutil.serialization.readBegin
import nl.adaptivity.xmlutil.serialization.readElements
import nl.adaptivity.xmlutil.serialization.simpleSerialClassDesc
import nl.adaptivity.xmlutil.serialization.writeStructure

//@Serializable
interface Namespace {

    /**
     * Gets the prefix, returns "" if this is a default
     * namespace declaration.
     */
    val prefix: String

    operator fun component1() = prefix

    /**
     * Gets the uri bound to the prefix of this namespace
     */
    val namespaceURI: String
    operator fun component2() = namespaceURI

    @Serializer(forClass = Namespace::class)
    companion object: KSerializer<Namespace> {
        override val descriptor: SerialDescriptor
            = simpleSerialClassDesc<Namespace>("prefix" to StringSerializer,
                                               "namespaceURI" to StringSerializer)

        override fun deserialize(decoder: Decoder): Namespace {
            lateinit var prefix: String
            lateinit var namespaceUri: String
            decoder.readBegin(descriptor) { desc ->
                readElements(this) {
                    when (it) {
                        0 -> prefix = decodeStringElement(desc, it)
                        1 -> namespaceUri = decodeStringElement(desc, it)
                    }
                }
            }
            return XmlEvent.NamespaceImpl(prefix, namespaceUri)
        }

        override fun serialize(encoder: Encoder, obj : Namespace) {
            encoder.writeStructure(descriptor) {
                encodeStringElement(descriptor, 0, obj.prefix)
                encodeStringElement(descriptor, 1, obj.namespaceURI)
            }
        }

    }
}

@Suppress("NOTHING_TO_INLINE")
@Deprecated("Use the property version", ReplaceWith("this.prefix"))
inline fun Namespace.getPrefix() = prefix

@Suppress("NOTHING_TO_INLINE")
@Deprecated("Use the property version", ReplaceWith("this.namespaceURI"))
inline fun Namespace.getNamespaceURI() = namespaceURI
