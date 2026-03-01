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

package io.github.pdvrieze.xml.schematypes.values.instances

import io.github.pdvrieze.xml.schematypes.primitive.isNCName
import io.github.pdvrieze.xml.schematypes.primitive.isNCName10
import io.github.pdvrieze.xml.schematypes.values.XSNCName
import nl.adaptivity.xmlutil.XmlUtilInternal
import nl.adaptivity.xmlutil.core.XmlVersion
import kotlin.jvm.JvmInline

@JvmInline
@XmlUtilInternal
value class XSNCNameImpl private constructor(override val xmlString: String) : XSNCName {

    init {
        // This can not go through NCNameType as VNCName is used in AtomicDatatype
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
