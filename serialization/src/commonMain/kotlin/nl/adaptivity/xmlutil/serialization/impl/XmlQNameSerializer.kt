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

package nl.adaptivity.xmlutil.serialization.impl

import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import nl.adaptivity.xmlutil.QName
import nl.adaptivity.xmlutil.QNameSerializer
import nl.adaptivity.xmlutil.XmlReader
import nl.adaptivity.xmlutil.XmlWriter
import nl.adaptivity.xmlutil.serialization.AbstractXmlSerializer

internal object XmlQNameSerializer : AbstractXmlSerializer<QName>() {
    override val descriptor: SerialDescriptor
        get() = QNameSerializer.descriptor
//        get() = XmlQNameSerializerOld.descriptor

    override fun deserializeXML(
        decoder: Decoder,
        input: XmlReader,
        previousValue: QName?,
        isValueChild: Boolean
    ): QName {
        return QNameSerializer.deserializeXML(decoder, input, previousValue, isValueChild)
    }

    override fun serializeXML(encoder: Encoder, output: XmlWriter, value: QName, isValueChild: Boolean) {
        QNameSerializer.serializeXML(encoder, output, value, isValueChild)
    }

    override fun serializeNonXML(encoder: Encoder, value: QName) {
        QNameSerializer.serialize(encoder, value)
    }

    override fun deserializeNonXML(decoder: Decoder): QName {
        return QNameSerializer.deserialize(decoder)
    }
}
