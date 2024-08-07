/*
 * Copyright (c) 2023.
 *
 * This file is part of XmlUtil.
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

import io.github.pdvrieze.formats.xmlschema.datatypes.serialization.XSUnique
import nl.adaptivity.xmlutil.QName

class ResolvedIndirectUnique(
    rawPart: XSUnique,
    schema: ResolvedSchemaLike,
    owner: ResolvedElement,
): ResolvedIndirectIdentityConstraint<ResolvedDirectUnique>(rawPart, schema, owner), ResolvedUnique {
    init {
        check(rawPart.name == null)
    }
    override val constraint: ResolvedIndirectUnique
        get() = this

    override val mdlQName: QName? = rawPart.name?.toQname(schema.targetNamespace)

    override val _ref: Lazy<ResolvedDirectUnique> = lazy {
        when (val r = schema.identityConstraint(requireNotNull(rawPart.ref))) {
            is ResolvedDirectUnique -> r
            is ResolvedIndirectUnique -> r.ref
            else -> throw IllegalArgumentException("Unique's ref property ${rawPart.ref} does not refer to a unique")
        }
    }

}
