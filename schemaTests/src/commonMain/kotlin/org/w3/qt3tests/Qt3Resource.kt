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

import io.github.pdvrieze.xml.schematypes.values.XsdAnyURI
import io.github.pdvrieze.xml.schematypes.values.XsdID
import kotlinx.serialization.Serializable
import nl.adaptivity.xmlutil.serialization.XmlSerialName

/**
 * An element which provides information about a file that can be read as text, used as input to
 * test cases.
 *
 * The `uri` attributes indicate how the resource is made available to queries (as a URI that can
 * be supplied to the `unparsed-text()`, `unparsed-text-lines()` and `unparsed-text-available()`
 * functions.)
 *
 * The `file` attribute gives the relative location of the file containing the resource.
 *
 * The optional `media-type` attribute gives the media type of the resource.
 *
 * The optional `encoding` attribute gives the name of the encoding of the resource.
 *
 * The scope of the &lt;resource&gt; element is the parent &lt;environment&gt; element in which it
 * appears. A validated source document references the schema, which maps to the `@id` of the
 * Schema element.
 */
@Serializable
@XmlSerialName("resource", QT3TNS)
class Qt3Resource: Qt3ResourceType, Qt3Environment.Element {
    constructor(
        id: XsdID? = null,
        description: Qt3Description? = null,
        created: Qt3Created? = null,
        modified: List<Qt3Modified> = emptyList(),
        file: XsdAnyURI? = null,
        uri: XsdAnyURI? = null,
        mediaType: String? = null,
        encoding: String? = null,
    ) : super(id, description, created, modified, file, uri, mediaType, encoding)
}

