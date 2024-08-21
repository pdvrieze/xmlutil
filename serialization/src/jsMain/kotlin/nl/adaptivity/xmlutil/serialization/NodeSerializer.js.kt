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

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import nl.adaptivity.xmlutil.QName
import nl.adaptivity.xmlutil.XmlReader
import nl.adaptivity.xmlutil.XmlSerializer
import nl.adaptivity.xmlutil.XmlWriter
import nl.adaptivity.xmlutil.core.impl.idom.IDocument
import nl.adaptivity.xmlutil.core.impl.idom.INode
import nl.adaptivity.xmlutil.util.impl.createDocument
import nl.adaptivity.xmlutil.dom.Node as Node1
import nl.adaptivity.xmlutil.dom2.Node as Node2

@Deprecated(
    "used for more cross-platform stable version",
    ReplaceWith("Node.serializer()", "nl.adaptivity.xmlutil.dom2.Node.serializer()")
)
public actual object NodeSerializer : XmlSerializer<Node1> {
    private val delegate = Node2.serializer() as XmlSerializer<Node2>
    private val helperDoc = createDocument(QName("XX")) as IDocument

    @OptIn(ExperimentalSerializationApi::class)
    actual override val descriptor: SerialDescriptor = SerialDescriptor("org.w3c.dom.node", delegate.descriptor)

    actual override fun serialize(encoder: Encoder, value: Node1) {
        val v = (value as? Node2) ?: helperDoc.adoptNode(value)
        delegate.serialize(encoder, v)
    }

    actual override fun serializeXML(encoder: Encoder, output: XmlWriter, value: Node1, isValueChild: Boolean) {
        val v = (value as? Node2) ?: helperDoc.adoptNode(value)
        delegate.serializeXML(encoder, output, v, isValueChild)
    }

    actual override fun deserialize(decoder: Decoder): Node1 {
        return delegate.deserialize(decoder) as INode
    }

    actual override fun deserializeXML(
        decoder: Decoder,
        input: XmlReader,
        previousValue: Node1?,
        isValueChild: Boolean
    ): Node1 {
        return delegate.deserializeXML(decoder, input, previousValue as INode?, isValueChild) as INode
    }

}
