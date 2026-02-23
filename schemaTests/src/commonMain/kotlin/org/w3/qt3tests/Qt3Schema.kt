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
import org.w3.qt3tests.resolved.ResolutionContext
import org.w3.qt3tests.resolved.ResolvedQt3Schema

/**
 * An element which provides information about a schema to be used to validate a source document.
 * The scope of the &lt;schema&gt; element is the parent &lt;environment&gt; element in which it
 * appears. The test drivers should assume a dependency on schema-awareness.
 */
@Serializable
@XmlSerialName("schema", QT3TNS)
class Qt3Schema : Qt3SchemaType, Qt3Environment.Element {
    constructor(
        id: VID?,
        description: Qt3Description?,
        created: Qt3Created?,
        modified: List<Qt3Modified>,
        uri: VAnyURI?,
        file: VAnyURI?,
        xsdVersion: String ="1.0",
        role: String?
    ) : super(id, description, created, modified, uri, file, xsdVersion, role)

    context(ctx: ResolutionContext)
    fun resolve(): ResolvedQt3Schema {
        return ResolvedQt3Schema()
    }
}
