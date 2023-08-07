/*
 * Copyright (c) 2023.
 *
 * This file is part of xmlutil.
 *
 * This file is licenced to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You should have received a copy of the license with the source distribution.
 * Alternatively, you may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package io.github.pdvrieze.formats.xmlschema.resolved

import io.github.pdvrieze.formats.xmlschema.datatypes.primitiveInstances.VAnyURI
import io.github.pdvrieze.formats.xmlschema.datatypes.primitiveInstances.VToken
import io.github.pdvrieze.formats.xmlschema.datatypes.serialization.XSNotation
import nl.adaptivity.xmlutil.QName

class ResolvedNotation(
    rawPart: XSNotation,
    val schema: ResolvedSchemaLike,
    val location: String,
) : NamedPart {
    fun check() {
        //TODO add checks
    }

    override val mdlQName: QName = rawPart.name.toQname(schema.targetNamespace)

    val public: VToken = rawPart.public
    val system: VAnyURI? = rawPart.system

    internal constructor(rawPart: SchemaAssociatedElement<XSNotation>, schema: ResolvedSchemaLike) :
            this(rawPart.element, schema, rawPart.schemaLocation)
}
