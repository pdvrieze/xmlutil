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
import nl.adaptivity.xmlutil.serialization.XmlElement
import org.w3.qt3tests.attrGroups.Qt3FileAttr
import org.w3.qt3tests.attrGroups.Qt3RoleAttr
import org.w3.qt3tests.attrGroups.Qt3UriAttr
import org.w3.qt3tests.attrGroups.Qt3ValidationAttr

/**
 * Defines the type of the <code>source</code> element.
 */
@Serializable
abstract class Qt3SourceType: Qt3BaseType, Qt3RoleAttr, Qt3FileAttr, Qt3UriAttr, Qt3ValidationAttr {

    val description: Qt3Description?
    val created: Qt3Created?
    val modified: List<Qt3Modified>
    final override val role: String?
    final override val file: VAnyURI
    final override val uri: VAnyURI?
    @XmlElement(false) final override val validation: Qt3Validations?

    constructor(
        file: VAnyURI,
        id: VID? = null,
        description: Qt3Description? = null,
        created: Qt3Created? = null,
        modified: List<Qt3Modified> = emptyList(),
        role: String? = null,
        uri: VAnyURI? = null,
        validation: Qt3Validations? = null,
    ) : super(id) {
        this.description = description
        this.created = created
        this.modified = modified
        this.role = role
        this.file = file
        this.uri = uri
        this.validation = validation
    }
}
