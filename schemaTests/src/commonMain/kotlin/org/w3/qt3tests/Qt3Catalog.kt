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

import io.github.pdvrieze.formats.xmlschema.datatypes.primitiveInstances.VAnyURI
import io.github.pdvrieze.formats.xmlschema.datatypes.primitiveInstances.VID
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import nl.adaptivity.xmlutil.serialization.XmlSerialName
import org.w3.qt3tests.attrGroups.Qt3FileAttr
import org.w3.qt3tests.attrGroups.Qt3NameAttr

/**
 * Denotes the root element of the catalog document.  The catalog lists all test-sets that are
 * to be run and also contains an environment of assorted schemas and source documents.
 *
 * @property version Identifies the version of the test suite. Should be incremented each time
 * the test suite is released.
 * @property testSuite Identifies this test suite (in case there are several test suites using this format)
 */
@Serializable
@XmlSerialName("catalog", QT3TNS)
data class Qt3Catalog(
    val version: String,
    @SerialName("test-suite")
    val testSuite: String,
    val environments: List<Qt3Environment>,
    val testSets: List<Qt3CatalogTestSet>,
) {
}


@Serializable
@XmlSerialName("test-set", QT3TNS)
class Qt3CatalogTestSet : Qt3BaseType, Qt3NameAttr, Qt3FileAttr {
    override val name: String
    override val file: VAnyURI?

    constructor(name: String, file: VAnyURI? = null, id: VID? = null) : super(id) {
        this.name = name
        this.file = file
    }
}


const val QT3TNS="http://www.w3.org/2010/09/qt-fots-catalog"
