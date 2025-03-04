/*
 * Copyright (c) 2024.
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

package nl.adaptivity.xmlutil.util

import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import nl.adaptivity.xmlutil.XmlReader
import nl.adaptivity.xmlutil.XmlSerializer
import nl.adaptivity.xmlutil.XmlWriter


public object ICompactFragmentSerializer : XmlSerializer<ICompactFragment> {
    private val delegate = CompactFragmentSerializer

    @Suppress("OPT_IN_USAGE")
    override val descriptor: SerialDescriptor by lazy(LazyThreadSafetyMode.PUBLICATION) {
        // needed for wasm initialisation
        SerialDescriptor("ICompactFragment", delegate.descriptor)
    }

    override fun serialize(encoder: Encoder, value: ICompactFragment) {
        delegate.serializeImpl(encoder, value)
    }

    override fun serializeXML(encoder: Encoder, output: XmlWriter, value: ICompactFragment, isValueChild: Boolean) {
        delegate.serializeXMLImpl(encoder, output, value, isValueChild)
    }

    override fun deserialize(decoder: Decoder): ICompactFragment {
        return delegate.deserialize(decoder)
    }

    override fun deserializeXML(
        decoder: Decoder,
        input: XmlReader,
        previousValue: ICompactFragment?,
        isValueChild: Boolean
    ): ICompactFragment {
        return delegate.deserializeXML(decoder, input, previousValue as CompactFragment?, isValueChild)
    }
}
