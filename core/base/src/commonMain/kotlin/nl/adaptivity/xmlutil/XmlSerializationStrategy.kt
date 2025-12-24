/*
 * Copyright (c) 2024-2025.
 *
 * This file is part of xmlutil.
 *
 * This file is licenced to you under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance
 * with the License.  You should have  received a copy of the license
 * with the source distribution. Alternatively, you may obtain a copy
 * of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or
 * implied.  See the License for the specific language governing
 * permissions and limitations under the License.
 */

package nl.adaptivity.xmlutil

import kotlinx.serialization.SerializationStrategy
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Encoder

/**
 * [SerializationStrategy] sub-interface that supports special serialization for the XML
 * format. The format will call [serializeXML] rather than [serialize] allowing for special
 * functionality.
 *
 * **Note** that if the serialization differs for different formats you must use an XML specific
 * descriptor by using the [SerialDescriptor.xml] extension function that allows for this special
 * casing. This is a bit of a hack to allow for it to be visible across wrapping descriptors.
 */
@ExperimentalXmlUtilApi
public interface XmlSerializationStrategy<in T> : SerializationStrategy<T> {
    /**
     * Deserialize the XML using the implementation. This is intended to be using the [XmlReader]
     * for special purposes. The format will attempt to shield the input to avoid erroneous
     * parsing.
     *
     * @param encoder The encoder for serialization
     * @param output The [XmlWriter] to use for special purpose writing. Note that mixing output and
     *   encoder based may not work correctly
     * @param isValueChild A parameter to indicate whether the element is a value child (the output)
     *   should still allow for writing attributes/namespace declarations on the container.
     */
    public fun serializeXML(encoder: Encoder, output: XmlWriter, value: T, isValueChild: Boolean = false)
}
