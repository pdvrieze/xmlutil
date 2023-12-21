/*
 * Copyright (c) 2023.
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

package nl.adaptivity.xmlutil

import kotlinx.serialization.DeserializationStrategy
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerializationStrategy
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

public interface XmlDeserializationStrategy<out T> : DeserializationStrategy<T> {
    public fun deserializeXML(
        decoder: Decoder,
        input: XmlReader,
        previousValue: @UnsafeVariance T? = null,
        isValueChild: Boolean = false
    ): T
}

public interface XmlSerializationStrategy<in T> : SerializationStrategy<T> {
    public fun serializeXML(encoder: Encoder, output: XmlWriter, value: T, isValueChild: Boolean = false)
}

public interface XmlSerializer<T> : KSerializer<T>, XmlSerializationStrategy<T>, XmlDeserializationStrategy<T>
