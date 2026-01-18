/*
 * Copyright (c) 2021-2026.
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

package io.github.pdvrieze.formats.xmlschema.datatypes.primitiveInstances

import io.github.pdvrieze.formats.xmlschema.datatypes.primitiveTypes.NMTokenType
import kotlinx.serialization.Serializable
import nl.adaptivity.xmlutil.XmlReader
import kotlin.jvm.JvmInline

@JvmInline
@Serializable(VNMToken.Serializer::class)
value class VNMToken private constructor(override val xmlString: String) : VToken {

    override fun toString(): String = xmlString

    private class Serializer : SimpleTypeSerializer<VNMToken>("NMTOKEN") {
        override fun deserialize(
            raw: String,
            input: XmlReader?
        ): VNMToken {
            return invoke(raw)
        }
    }

    companion object {
        operator fun invoke(raw: String): VNMToken {
            return VNMToken(
                NMTokenType.mdlFacets.validateRepresentationOnly(NMTokenType, VString(raw)).xmlString
            )
        }
    }
}
