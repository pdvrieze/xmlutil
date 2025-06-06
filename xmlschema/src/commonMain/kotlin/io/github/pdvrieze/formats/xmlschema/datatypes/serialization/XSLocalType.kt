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

package io.github.pdvrieze.formats.xmlschema.datatypes.serialization

import io.github.pdvrieze.formats.xmlschema.datatypes.primitiveInstances.VNCName
import io.github.pdvrieze.formats.xmlschema.types.*
import kotlinx.serialization.InternalSerializationApi
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SealedClassSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.encoding.encodeStructure

sealed interface XSIType: XSI_Annotated

sealed interface XSGlobalType: XSIType {
    val name: VNCName
}

@Serializable(XSLocalType.Serializer::class)
sealed class XSLocalType: XSIType {

    companion object Serializer: KSerializer<XSLocalType> {
        // multiple copies are valid and should be identical.
        @OptIn(InternalSerializationApi::class)
        private val delegate: KSerializer<XSLocalType> by lazy(LazyThreadSafetyMode.PUBLICATION) {
            SealedClassSerializer(
                "io.github.pdvrieze.formats.xmlschema.datatypes.serialization.XSLocalType",
                XSLocalType::class,
                arrayOf(XSLocalSimpleType::class, XSLocalComplexType::class),
                arrayOf(XSLocalSimpleType.serializer(), XSLocalComplexType.serializer())
            )
        }

        override val descriptor: SerialDescriptor get() = delegate.descriptor

        override fun serialize(encoder: Encoder, value: XSLocalType) {
            when (value) {
                is XSLocalSimpleType -> {
                    val actualSerializer = XSLocalSimpleType.serializer()
                    encoder.encodeStructure(descriptor) {
                        encodeStringElement(descriptor, 0, actualSerializer.descriptor.serialName)
                        encodeSerializableElement(descriptor, 1, actualSerializer, value)
                    }
                }
                is XSLocalComplexType -> {
                    val actualSerializer = XSLocalComplexType
                    encoder.encodeStructure(descriptor) {
                        encodeStringElement(descriptor, 0, actualSerializer.descriptor.serialName)
                        encodeSerializableElement(descriptor, 1, actualSerializer, value)
                    }
                }
            }
        }

        override fun deserialize(decoder: Decoder): XSLocalType {
            return delegate.deserialize(decoder)
        }
    }
}
