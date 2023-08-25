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

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.Serializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.buildClassSerialDescriptor
import kotlinx.serialization.encoding.*

public expect class QName {
    public constructor(namespaceURI: String, localPart: String, prefix: String)
    public constructor(namespaceURI: String, localPart: String)
    public constructor(localPart: String)

    public fun getPrefix(): String
    public fun getLocalPart(): String
    public fun getNamespaceURI(): String
}

public infix fun QName.isEquivalent(other: QName): Boolean {
    return getLocalPart() == other.getLocalPart() &&
            getNamespaceURI() == other.getNamespaceURI()
}

public inline val QName.prefix: String get() = getPrefix()
public inline val QName.localPart: String get() = getLocalPart()
public inline val QName.namespaceURI: String get() = getNamespaceURI()

public fun QName.toNamespace(): Namespace {
    return XmlEvent.NamespaceImpl(prefix, namespaceURI)
}

public typealias SerializableQName = @Serializable(QNameSerializer::class) QName

@OptIn(ExperimentalSerializationApi::class)
@Serializer(forClass = QName::class)
public object QNameSerializer : KSerializer<QName> {
    override val descriptor: SerialDescriptor = buildClassSerialDescriptor("javax.xml.namespace.QName") {
        val stringSerializer = String.serializer()
        element("namespace", stringSerializer.descriptor, isOptional = true)
        element("localPart", stringSerializer.descriptor)
        element("prefix", stringSerializer.descriptor, isOptional = true)
    }

    override fun deserialize(decoder: Decoder): QName = decoder.decodeStructure(descriptor) {
        var prefix = ""
        var namespace = ""
        lateinit var localPart: String

        loop@ while (true) {
            when (this.decodeElementIndex(descriptor)) {
                CompositeDecoder.DECODE_DONE -> break@loop
                0 -> namespace = decodeStringElement(descriptor, 0)
                1 -> localPart = decodeStringElement(descriptor, 1)
                2 -> prefix = decodeStringElement(descriptor, 2)
            }
        }
        QName(namespace, localPart, prefix)
    }

    @OptIn(ExperimentalSerializationApi::class)
    override fun serialize(encoder: Encoder, value: QName) {
        encoder.encodeStructure(descriptor) {
            value.namespaceURI.let { ns ->
                if (ns.isNotEmpty() || shouldEncodeElementDefault(descriptor, 0)) {
                    encodeStringElement(descriptor, 0, ns)
                }
            }

            encodeStringElement(descriptor, 1, value.localPart)

            value.prefix.let { prefix ->
                if (prefix.isNotEmpty() || shouldEncodeElementDefault(descriptor, 2)) {
                    encodeStringElement(descriptor, 2, prefix)
                }
            }
        }
    }
}
