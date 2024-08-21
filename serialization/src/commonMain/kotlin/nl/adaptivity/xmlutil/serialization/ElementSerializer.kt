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

@file:Suppress("DEPRECATION")

package nl.adaptivity.xmlutil.serialization

import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import nl.adaptivity.xmlutil.XmlReader
import nl.adaptivity.xmlutil.XmlSerializer
import nl.adaptivity.xmlutil.XmlWriter
import nl.adaptivity.xmlutil.dom.Element

public typealias SerializableElement=@Serializable(ElementSerializer::class) Element

@Deprecated(
    "used for more cross-platform stable version",
    ReplaceWith("Element.serializer()", "nl.adaptivity.xmlutil.dom2.Element.serializer()")
)
public expect object ElementSerializer : XmlSerializer<Element> {
    override val descriptor: SerialDescriptor

    override fun serialize(encoder: Encoder, value: Element)
    override fun serializeXML(encoder: Encoder, output: XmlWriter, value: Element, isValueChild: Boolean)

    override fun deserialize(decoder: Decoder): Element
    override fun deserializeXML(decoder: Decoder, input: XmlReader, previousValue: Element?, isValueChild: Boolean): Element
}

