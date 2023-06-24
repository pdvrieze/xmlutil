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

import io.github.pdvrieze.formats.xmlschema.datatypes.primitiveInstances.VNCName
import io.github.pdvrieze.formats.xmlschema.datatypes.serialization.XSField
import io.github.pdvrieze.formats.xmlschema.datatypes.serialization.XSKey
import io.github.pdvrieze.formats.xmlschema.datatypes.serialization.XSSelector
import io.github.pdvrieze.formats.xmlschema.model.IdentityConstraintModel
import io.github.pdvrieze.formats.xmlschema.types.T_Key
import nl.adaptivity.xmlutil.QName
import nl.adaptivity.xmlutil.qname

fun ResolvedKey(
    rawPart: XSKey,
    schema: ResolvedSchemaLike,
    owner: ResolvedElement,
): ResolvedKey = when(rawPart.name) {
    null -> ResolvedIndirectKey(rawPart, schema, owner)
    else -> ResolvedDirectKey(rawPart, schema, owner)
}

interface ResolvedKey : T_Key, IdentityConstraintModel.Key, ResolvedPart {
    override val rawPart: XSKey
}

class ResolvedDirectKey(
    override val rawPart: XSKey,
    schema: ResolvedSchemaLike,
    owner: ResolvedElement,
): ResolvedNamedIdentityConstraint(schema, owner), T_Key, ResolvedKey {
    override val name: VNCName = checkNotNull(rawPart.name)

    override val qName: QName get() = qname(schema.targetNamespace?.value, name.xmlString)

    override val selector: XSSelector get() = rawPart.selector

    override val fields: List<XSField> get() = rawPart.fields

    override val mdlIdentityConstraintCategory: IdentityConstraintModel.Category
        get() = IdentityConstraintModel.Category.KEY

    override fun check() {
        super<ResolvedNamedIdentityConstraint>.check()
        checkNotNull(rawPart.name)
    }

}

class ResolvedIndirectKey(
    override val rawPart: XSKey,
    schema: ResolvedSchemaLike,
    owner: ResolvedElement,
): ResolvedIndirectIdentityConstraint(schema, owner), T_Key, ResolvedKey {

    override val ref: ResolvedDirectKey = when (val r = schema.identityConstraint(requireNotNull(rawPart.ref))) {
        is ResolvedDirectKey -> r
        is ResolvedIndirectKey -> r.ref
        else -> throw IllegalArgumentException("Key's ref property ${rawPart.ref} does not refer to a key")
    }

    override fun check() {
        super<ResolvedIndirectIdentityConstraint>.check()
        check(rawPart.name == null)
    }

}
