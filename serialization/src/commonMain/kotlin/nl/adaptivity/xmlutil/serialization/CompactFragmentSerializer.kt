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

import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import nl.adaptivity.xmlutil.XmlReader
import nl.adaptivity.xmlutil.XmlSerializer
import nl.adaptivity.xmlutil.XmlWriter
import nl.adaptivity.xmlutil.util.CompactFragment
import nl.adaptivity.xmlutil.util.ICompactFragment

@Suppress("EXTENSION_SHADOWED_BY_MEMBER")
@Deprecated("Use serializer member",
    ReplaceWith("CompactFragment.serializer()", "nl.adaptivity.xmlutil.util.CompactFragment"),
    level = DeprecationLevel.HIDDEN
)
public fun CompactFragment.Companion.serializer(): KSerializer<CompactFragment> =
    serializer()

@Deprecated("Use the serializer defined in the core module",
    ReplaceWith(
        "nl.adaptivity.xmlutil.util.CompactFragmentSerializer",
        "nl.adaptivity.xmlutil.util.CompactFragmentSerializer"
    )
)
@OptIn(kotlinx.serialization.ExperimentalSerializationApi::class)
public object CompactFragmentSerializer : AbstractXmlSerializer<CompactFragment>() {
    private val delegate = CompactFragment.serializer() as XmlSerializer<CompactFragment>

    override val descriptor: SerialDescriptor = SerialDescriptor("nl.adaptivity.xmlutil.util.CompactFragment\\\$Compat", delegate.descriptor)

    override fun deserializeXML(
        decoder: Decoder,
        input: XmlReader,
        previousValue: CompactFragment?,
        isValueChild: Boolean
    ): CompactFragment {
        return delegate.deserializeXML(decoder, input, previousValue, isValueChild)
    }

    override fun deserializeNonXML(decoder: Decoder): CompactFragment {
        return delegate.deserialize(decoder)
    }

    override fun serializeXML(encoder: Encoder, output: XmlWriter, value: CompactFragment, isValueChild: Boolean) {
        delegate.serializeXML(encoder, output, value, isValueChild)
    }

    override fun serializeNonXML(encoder: Encoder, value: CompactFragment) {
        delegate.serialize(encoder, value)
    }

    public fun serialize(encoder: Encoder, value: ICompactFragment) {
        ICompactFragment.serializer().serialize(encoder, value)
    }

}

@Deprecated("Use the serializer defined in the core module",
    ReplaceWith(
        "nl.adaptivity.xmlutil.util.ICompactFragmentSerializer",
        "nl.adaptivity.xmlutil.util.ICompactFragmentSerializer"
    )
)
public object ICompactFragmentSerializer : AbstractXmlSerializer<ICompactFragment>() {
    private val delegate= ICompactFragment.serializer() as XmlSerializer<ICompactFragment>

    override val descriptor: SerialDescriptor get() = delegate.descriptor

    override fun serializeXML(encoder: Encoder, output: XmlWriter, value: ICompactFragment, isValueChild: Boolean) {
        delegate.serializeXML(encoder, output, value, isValueChild)
    }

    override fun serializeNonXML(encoder: Encoder, value: ICompactFragment) {
        delegate.serialize(encoder, value)
    }

    override fun deserializeXML(
        decoder: Decoder,
        input: XmlReader,
        previousValue: ICompactFragment?,
        isValueChild: Boolean
    ): ICompactFragment {
        return delegate.deserializeXML(decoder, input, previousValue, isValueChild)
    }

    override fun deserializeNonXML(decoder: Decoder): ICompactFragment {
        return delegate.deserialize(decoder)
    }
}

