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
import kotlinx.serialization.Serializable
import nl.adaptivity.xmlutil.serialization.XmlSerialName

/**
 * An element which provides information about a source xml file used as input to test cases.
 *
 * The `role` and `uri` attributes indicate how the source document is made available to queries
 * (as the context item, as the value of an external variable, or as a URI that can be supplied
 * to the `doc()` function.)
 *
 * The `file` attribute gives the relative location of the file containing the XML source.
 *
 * The scope of the &lt;source&gt; element is the parent &lt;environment&gt; element in which it
 * appears. A validated source document references the schema, which maps to the `@id` of the Schema
 * element.
 */
@Serializable
@XmlSerialName("source", QT3TNS)
class Qt3Source: Qt3SourceType, Qt3Environment.Element {

    constructor(
        id: VID?,
        description: Qt3Description?,
        created: Qt3Created?,
        modified: List<Qt3Modified>,
        role: String?,
        file: VAnyURI?,
        uri: VAnyURI?,
        validation: Qt3Validations?
    ) : super(id, description, created, modified, role, file, uri, validation)
}

