/*
 * Copyright (c) 2026.
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

package nl.adaptivity.xmlutil.test.util

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.encoding.AbstractEncoder
import kotlinx.serialization.modules.EmptySerializersModule
import kotlinx.serialization.modules.SerializersModule

@OptIn(ExperimentalSerializationApi::class)
internal class ValueCapturingEncoder : AbstractEncoder() {
    var capturedString: String? = null
    var capturedBoolean: Boolean? = null
    var capturedDouble: Double? = null
    var capturedFloat: Float? = null

    override val serializersModule: SerializersModule = EmptySerializersModule()
    override fun encodeString(value: String) { capturedString = value }
    override fun encodeBoolean(value: Boolean) { capturedBoolean = value }
    override fun encodeDouble(value: Double) { capturedDouble = value }
    override fun encodeFloat(value: Float) { capturedFloat = value }
}
