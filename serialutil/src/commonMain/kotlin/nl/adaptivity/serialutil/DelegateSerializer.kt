/*
 * Copyright (c) 2019-2025.
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

package nl.adaptivity.serialutil

import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

@Deprecated("This serializer doesn't do anything, just forward to the passed in serializer", ReplaceWith("DelegatingSerializer"))
public abstract class DelegateSerializer<T>(val delegate: KSerializer<T>) : KSerializer<T> {
    override val descriptor: SerialDescriptor =
        SerialDescriptor("${delegate.descriptor.serialName}.delegate", delegate.descriptor)

    override fun deserialize(decoder: Decoder): T = delegate.deserialize(decoder)

    override fun serialize(encoder: Encoder, value: T) = delegate.serialize(encoder, value)
}
