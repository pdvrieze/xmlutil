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

import io.github.pdvrieze.formats.xmlschema.datatypes.primitiveTypes.NameType
import io.github.pdvrieze.formats.xmlschema.datatypes.primitiveTypes.isXmlName
import kotlinx.serialization.Serializable
import nl.adaptivity.xmlutil.XmlReader
import nl.adaptivity.xmlutil.XmlUtilInternal
import kotlin.jvm.JvmInline

@Serializable(VName.Serializer::class)
interface VName : VToken {

    @JvmInline
    private value class Inst(override val xmlString: String) : VName {

        override fun toString(): String = xmlString

    }

    @OptIn(XmlUtilInternal::class)
    class Serializer : SimpleTypeSerializer<VName>("VName") {
        override fun deserialize(
            raw: String,
            input: XmlReader?
        ): VName {
            check(raw.isXmlName()) { "'$raw' is not an xml name" }
            return invoke(raw)
        }
    }

    companion object {

        operator fun invoke(value: String): VName {
            val validated = NameType.mdlFacets.validateRepresentationOnly(NameType, VString(value))
            return Inst(validated.xmlString)
        }

    }

}
