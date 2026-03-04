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

import io.github.pdvrieze.formats.xmlschema.datatypes.primitiveTypes.isNCName
import io.github.pdvrieze.formats.xmlschema.datatypes.primitiveTypes.isNCName10
import io.github.pdvrieze.xml.schematypes.values.XsdNCName
import kotlinx.serialization.Serializable
import nl.adaptivity.xmlutil.QName
import nl.adaptivity.xmlutil.XmlReader
import nl.adaptivity.xmlutil.XmlUtilInternal
import nl.adaptivity.xmlutil.core.XmlVersion
import nl.adaptivity.xmlutil.xmlCollapseWhitespace
import kotlin.jvm.JvmInline
import kotlin.jvm.JvmName

typealias VNCName = XsdNCName
fun VNCName(str: String): XsdNCName = XsdNCName(str)

@Serializable(VNCNameXX.Serializer::class)
interface VNCNameXX : VName {

    fun toQname(targetNamespace: VAnyURI?): QName {
        return QName(targetNamespace?.value ?: "", xmlString)
    }


    @JvmInline
    private value class Inst private constructor(override val xmlString: String) : VNCNameXX {

        init {
            // This can not go through NCNameType as VNCNameXX is used in AtomicDatatype
            require(xmlString.isNCName()) { "'$xmlString' is not an NCName" }
        }

        constructor(xmlString: String, version: XmlVersion = XmlVersion.XML11) : this(xmlString) {
            when (version) {
                XmlVersion.XML10 -> require(xmlString.isNCName10()) { "'$xmlString' is not an NCName in XML 1.0" }
                XmlVersion.XML11 -> require(xmlString.isNCName()) { "'$xmlString' is not an NCName in XML 1.1" }
            }
        }

        override fun toString(): String = xmlString
    }

    @OptIn(XmlUtilInternal::class)
    class Serializer : SimpleVTypeSerializer<VNCNameXX>("VNCNameXX") {
        override fun deserialize(
            raw: String,
            input: XmlReader?
        ): VNCNameXX {
            val version = when {
                input?.version == "1.0" -> XmlVersion.XML10
                else -> XmlVersion.XML11
            }
            return Inst(xmlCollapseWhitespace(raw), version)
        }
    }

    companion object {
        operator fun invoke(value: String): VNCNameXX = Inst(value)
        @JvmName("invokeNullable")
        operator fun invoke(value: String?): VNCNameXX? = value?.let { Inst(it) }
    }
}
