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

package io.github.pdvrieze.formats.xmlschema.datatypes.primitiveInstances

import kotlinx.serialization.Serializable
import kotlinx.serialization.encoding.Decoder
import nl.adaptivity.xmlutil.XmlUtilInternal

@Serializable(VAnySimpleType.Serializer::class)
interface VAnySimpleType {// inherits any
    val xmlString: String

    private class Inst(val value: String): VAnySimpleType {
        override val xmlString: String get() = value
    }

    @OptIn(XmlUtilInternal::class)
    class Serializer: SimpleTypeSerializer<VAnySimpleType>("anySimpleType") {
        override fun deserialize(decoder: Decoder): VAnySimpleType {
            return Inst(decoder.decodeString())
        }
    }
}
