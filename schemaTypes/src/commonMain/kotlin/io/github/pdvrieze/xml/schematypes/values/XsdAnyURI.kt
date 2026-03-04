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

package io.github.pdvrieze.xml.schematypes.values

import io.github.pdvrieze.xml.schematypes.impl.SimpleTypeSerializer
import io.github.pdvrieze.xml.schematypes.types.AnyURIType
import io.github.pdvrieze.xml.schematypes.values.instances.XsdParsedUri
import io.github.pdvrieze.xml.schematypes.values.instances.XsdRelaxedUri
import kotlinx.serialization.Serializable
import nl.adaptivity.xmlutil.ExperimentalXmlUtilApi
import nl.adaptivity.xmlutil.XmlReader
import nl.adaptivity.xmlutil.xmlCollapseWhitespace

@ExperimentalXmlUtilApi
@Serializable(XsdAnyURI.Companion::class)
interface XsdAnyURI : XsdAtomic, CharSequence {
    override val schemaType: AnyURIType<XsdAnyURI>

    val value: String get() = xmlString

    operator fun component1(): String = value

    companion object Companion : SimpleTypeSerializer<XsdAnyURI>("xsd.anyURI") {
        operator fun invoke(value: String) = value.toAnyUri()
        operator fun invoke(value: CharSequence) = value.toString().toAnyUri()

        override fun deserialize(
            raw: String,
            input: XmlReader?
        ): XsdAnyURI {
            return XsdAnyURI(raw)
        }

        val EMPTY: XsdAnyURI = "".toAnyUri()

    }
}

@ExperimentalXmlUtilApi
fun String.toAnyUri(): XsdAnyURI {
    val s = xmlCollapseWhitespace(this)
    return runCatching { XsdParsedUri(s) }.getOrElse { XsdRelaxedUri(s) }
}
