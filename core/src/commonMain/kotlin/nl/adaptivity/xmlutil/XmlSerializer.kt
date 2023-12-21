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
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerializationStrategy
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.SerialKind
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

@ExperimentalXmlUtilApi
public interface XmlDeserializationStrategy<out T> : DeserializationStrategy<T> {
    public fun deserializeXML(
        decoder: Decoder,
        input: XmlReader,
        previousValue: @UnsafeVariance T? = null,
        isValueChild: Boolean = false
    ): T
}

@ExperimentalXmlUtilApi
public interface XmlSerializationStrategy<in T> : SerializationStrategy<T> {
    public fun serializeXML(encoder: Encoder, output: XmlWriter, value: T, isValueChild: Boolean = false)
}

@ExperimentalXmlUtilApi
public interface XmlSerializer<T> : KSerializer<T>, XmlSerializationStrategy<T>, XmlDeserializationStrategy<T>

@ExperimentalXmlUtilApi
public fun SerialDescriptor.xml(
    xmlDescriptor: SerialDescriptor = this,
    serialQName: QName? = null
): XmlSerialDescriptor {
    return XmlSerialDescriptorImpl(this, xmlDescriptor, serialQName)
}

@ExperimentalXmlUtilApi
public interface XmlSerialDescriptor : SerialDescriptor {
    public val delegate: SerialDescriptor
    public val xmlDescriptor: SerialDescriptor
    public val serialQName: QName?


    @ExperimentalSerializationApi
    override val elementsCount: Int get() = delegate.elementsCount

    @ExperimentalSerializationApi
    override val kind: SerialKind get() = delegate.kind

    @ExperimentalSerializationApi
    override val serialName: String get() = delegate.serialName

    @ExperimentalSerializationApi
    override fun getElementAnnotations(index: Int): List<Annotation> = delegate.getElementAnnotations(index)

    @ExperimentalSerializationApi
    override fun getElementDescriptor(index: Int): SerialDescriptor = delegate.getElementDescriptor(index)

    @ExperimentalSerializationApi
    override fun getElementIndex(name: String): Int = delegate.getElementIndex(name)

    @ExperimentalSerializationApi
    override fun getElementName(index: Int): String = delegate.getElementName(index)

    @ExperimentalSerializationApi
    override fun isElementOptional(index: Int): Boolean {
        return delegate.isElementOptional(index)
    }
}

@ExperimentalXmlUtilApi
internal class XmlSerialDescriptorImpl internal constructor(
    override val delegate: SerialDescriptor,
    xmlDescriptor: SerialDescriptor = delegate,
    serialQName: QName? = null
) : XmlSerialDescriptor {
    override val serialQName: QName? get() = xmlDescriptor.serialQName

    override val xmlDescriptor: XmlSerialDescriptor = object : XmlSerialDescriptor {
        override val delegate: SerialDescriptor = xmlDescriptor

        override val xmlDescriptor: SerialDescriptor get() = this
        override val serialQName: QName? = serialQName
    }

}
