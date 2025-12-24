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

import kotlinx.serialization.DeserializationStrategy
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder

/**
 * [DeserializationStrategy] sub-interface that supports special deserialization for the XML
 * format. The format will call [deserializeXML] rather than [deserialize] allowing for special
 * functionality.
 *
 * **Note** that if the serialization differs for different formats you must use an XML specific
 * descriptor by using the [SerialDescriptor.xml] extension function that allows for this special
 * casing. This is a bit of a hack to allow for it to be visible across wrapping descriptors.
 */
@ExperimentalXmlUtilApi
public interface XmlDeserializationStrategy<out T> : DeserializationStrategy<T> {
    /**
     * Deserialize the XML using the implementation. This is intended to be using the [XmlReader]
     * for special purposes. The format will attempt to shield the input to avoid erroneous
     * parsing.
     *
     * @param decoder The decoder for deserialization
     * @param input The [XmlReader] to use for special purpose parsing. May be a wrapper that ensures
     *   valid fallback on invalid parsing.
     * @param previousValue The value previously set for the element.
     * @param isValueChild A parameter to indicate whether the element is a value child (and we are
     * looking at the first child of the container - all **siblings** would be part of the value).
     */
    public fun deserializeXML(
        decoder: Decoder,
        input: XmlReader,
        previousValue: @UnsafeVariance T? = null,
        isValueChild: Boolean = false
    ): T
}
