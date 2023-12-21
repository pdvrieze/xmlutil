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

package nl.adaptivity.xmlutil.serialization

import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import nl.adaptivity.xmlutil.XmlSerializer

public abstract class AbstractXmlSerializer<T> : XmlSerializer<T> {

    final override fun serialize(encoder: Encoder, value: T): Unit = when (encoder) {
        is XML.XmlOutput -> serializeXML(encoder, encoder.target, value)
        else -> serializeNonXML(encoder, value)
    }

    final override fun deserialize(decoder: Decoder): T = when (decoder) {
        is XML.XmlInput -> deserializeXML(decoder, decoder.input)
        else -> deserializeNonXML(decoder)
    }

    public abstract fun serializeNonXML(encoder: Encoder, value: T)

    public abstract fun deserializeNonXML(decoder: Decoder): T
}
