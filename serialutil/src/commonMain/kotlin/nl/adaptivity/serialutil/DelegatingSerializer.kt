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

package nl.adaptivity.serialutil

import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

abstract class DelegatingSerializer<T, D>(val delegateSerializer: KSerializer<D>): KSerializer<T> {

    abstract fun fromDelegate(delegate: D): T

    abstract fun T.toDelegate(): D

    override fun deserialize(decoder: Decoder): T {
        return fromDelegate(delegateSerializer.deserialize(decoder))
    }

    override val descriptor: SerialDescriptor get() = delegateSerializer.descriptor

    override fun serialize(encoder: Encoder, value: T) {
        delegateSerializer.serialize(encoder, value.toDelegate())
    }
}
