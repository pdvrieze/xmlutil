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
import io.github.pdvrieze.formats.xmlschema.datatypes.primitiveInstances.VNCName
import io.github.pdvrieze.formats.xmlschema.datatypes.serialization.XSKey
import nl.adaptivity.xmlutil.QName
import nl.adaptivity.xmlutil.localPart

class ResolvedIndirectKey(
    override val rawPart: XSKey,
    schema: ResolvedSchemaLike,
    owner: ResolvedElement,
): ResolvedIndirectIdentityConstraint(schema, owner), ResolvedKey {
    override val mdlIdentityConstraintCategory: ResolvedIdentityConstraint.Category get() = super.mdlIdentityConstraintCategory

    override val ref: ResolvedDirectKey = when (val r = schema.identityConstraint(requireNotNull(rawPart.ref))) {
        is ResolvedDirectKey -> r
        is ResolvedIndirectKey -> r.ref
        else -> throw IllegalArgumentException("Key's ref property ${rawPart.ref} does not refer to a key")
    }

    override val mdlQName: QName? = rawPart.name?.toQname(schema.targetNamespace)

    override val constraint: ResolvedIndirectKey
        get() = this

    override fun check(checkedTypes: MutableSet<QName>) {
        super<ResolvedIndirectIdentityConstraint>.check(checkedTypes)
        check(rawPart.name == null)
    }

}
