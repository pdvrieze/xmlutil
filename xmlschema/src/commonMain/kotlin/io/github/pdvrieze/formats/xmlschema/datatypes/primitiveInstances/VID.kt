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

import io.github.pdvrieze.formats.xmlschema.datatypes.primitiveTypes.IDType
import kotlinx.serialization.Serializable
import nl.adaptivity.xmlutil.XmlReader
import kotlin.jvm.JvmInline

@JvmInline
@Serializable(VID.Serializer::class)
value class VID private constructor(override val xmlString: String) : VNCName {

    init {
        IDType.mdlFacets.validateValue(this)
    }

    override fun toString(): String = xmlString

    private class Serializer : SimpleTypeSerializer<VID>("ID") {
        override fun deserialize(
            raw: String,
            input: XmlReader?
        ): VID {
            return VID.invoke(raw)
        }
    }

    companion object {

        operator fun invoke(rawId: String): VID {
            val r = IDType.mdlFacets.validateRepresentationOnly(
                IDType,
                VString(rawId)
            )
            return VID(r.xmlString)
        }

    }

}


