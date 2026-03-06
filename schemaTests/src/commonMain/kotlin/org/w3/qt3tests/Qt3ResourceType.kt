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
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import org.w3.qt3tests.attrGroups.Qt3EncodingAttr
import org.w3.qt3tests.attrGroups.Qt3FileAttr
import org.w3.qt3tests.attrGroups.Qt3MediaTypeAttr
import org.w3.qt3tests.attrGroups.Qt3UriAttr

/**
 * Defines the type of the `resource` element.
 */
@Serializable
abstract class Qt3ResourceType: Qt3BaseType, Qt3FileAttr, Qt3UriAttr, Qt3MediaTypeAttr, Qt3EncodingAttr {
    val description: Qt3Description?
    val created: Qt3Created?
    val modified: List<Qt3Modified>
    final override val file: XsdAnyURI?
    final override val uri: XsdAnyURI?

    @SerialName("media-type")
    final override val mediaType: String?
    final override val encoding: String?

    constructor(
        id: XsdID? = null,
        description: Qt3Description? = null,
        created: Qt3Created? = null,
        modified: List<Qt3Modified> = emptyList(),
        file: XsdAnyURI? = null,
        uri: XsdAnyURI? = null,
        mediaType: String? = null,
        encoding: String? = null,
    ) : super(id) {
        this.description = description
        this.created = created
        this.modified = modified
        this.file = file
        this.uri = uri
        this.mediaType = mediaType
        this.encoding = encoding
    }
}
