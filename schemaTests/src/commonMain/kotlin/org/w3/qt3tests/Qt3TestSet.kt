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

package org.w3.qt3tests

import io.github.pdvrieze.formats.xmlschema.datatypes.primitiveInstances.VID
import io.github.pdvrieze.formats.xmlschema.datatypes.primitiveInstances.VNCName
import io.github.pdvrieze.formats.xmlschema.datatypes.primitiveInstances.VToken
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import nl.adaptivity.xmlutil.XMLConstants
import nl.adaptivity.xmlutil.serialization.XmlElement
import nl.adaptivity.xmlutil.serialization.XmlSerialName
import org.w3.qt3tests.attrGroups.Qt3Covers30Attr
import org.w3.qt3tests.attrGroups.Qt3CoversAttr
import org.w3.qt3tests.attrGroups.Qt3NameAttr

@Serializable
abstract class Qt3BaseType(
    @XmlSerialName("id", XMLConstants.XML_NS_URI, XMLConstants.XML_NS_PREFIX)
    val id: VID?
)


/**
 * Denotes an element which provides a sequence of test-case entries for a particular function.
 * Within this element we provide data needed to run the test for that function.
 */
@Serializable
@XmlSerialName("test-set", QT3TNS)
class Qt3TestSet(
    override val name: String,
    @XmlElement(false) override val covers: List<VToken>?,
    @SerialName("covers-30")
    @XmlElement(false) override val covers30: List<VNCName>?,
    val elements: List<Element>,
    val testCase: List<Qt3TestCase>,
) : Qt3NameAttr, Qt3CoversAttr, Qt3Covers30Attr {

    @Serializable
    sealed interface Element

}
