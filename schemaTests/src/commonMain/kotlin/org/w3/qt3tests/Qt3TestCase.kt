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

import io.github.pdvrieze.formats.xmlschema.datatypes.primitiveInstances.VNCName
import io.github.pdvrieze.formats.xmlschema.datatypes.primitiveInstances.VToken
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import nl.adaptivity.xmlutil.serialization.XmlElement
import nl.adaptivity.xmlutil.serialization.XmlSerialName
import org.w3.qt3tests.attrGroups.Qt3Covers30Attr
import org.w3.qt3tests.attrGroups.Qt3CoversAttr
import org.w3.qt3tests.attrGroups.Qt3NameAttr

/**
 * Denotes an element that contains a test that must be run in a named environment, also contains
 * the expected result and description of the test, including author and creation date.
 */
@Serializable
@XmlSerialName("test-case", QT3TNS)
class Qt3TestCase(
    val description: Qt3Description? = null,
    val created: Qt3Created? = null,
    val modified: List<Qt3Modified> = emptyList(),
    val environment: Qt3Environment? = null,
    val modules: List<Qt3Module> = emptyList(),
    val dependencies: List<Qt3Dependency> = emptyList(),
    val test: Qt3Test? = null,
    val result: Qt3Result? = null,
    override val name: String? = null,
    @XmlElement(false)
    override val covers: List<VToken>? = emptyList(),
    @XmlElement(false)
    @SerialName("covers-30")
    override val covers30: List<VNCName>? = emptyList(),
): Qt3NameAttr, Qt3CoversAttr, Qt3Covers30Attr {

}

