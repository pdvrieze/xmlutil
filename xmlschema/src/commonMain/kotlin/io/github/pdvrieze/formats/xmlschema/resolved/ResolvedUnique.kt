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
import io.github.pdvrieze.formats.xmlschema.datatypes.serialization.XSSelector
import io.github.pdvrieze.formats.xmlschema.datatypes.serialization.XSUnique
import io.github.pdvrieze.formats.xmlschema.model.IdentityConstraintModel
import io.github.pdvrieze.formats.xmlschema.types.T_Unique
import nl.adaptivity.xmlutil.QName
import nl.adaptivity.xmlutil.qname

fun ResolvedUnique(
    rawPart: XSUnique,
    schema: ResolvedSchemaLike,
    owner: ResolvedElement,
): ResolvedUnique = when(rawPart.name) {
    null -> ResolvedIndirectUnique(rawPart, schema, owner)
    else -> ResolvedDirectUnique(rawPart, schema, owner)
}


interface ResolvedUnique: T_Unique, IdentityConstraintModel.Unique, ResolvedIdentityConstraint {
    override val rawPart: XSUnique
}

class ResolvedDirectUnique(
    override val rawPart: XSUnique,
    schema: ResolvedSchemaLike,
    owner: ResolvedElement,
): ResolvedNamedIdentityConstraint(schema, owner), T_Unique, ResolvedUnique, IdentityConstraintModel.Unique {

    override val constraint: ResolvedDirectUnique
        get() = this

    override val name: VNCName = checkNotNull(rawPart.name)

    val qName: QName get() = qname(schema.targetNamespace?.value, name.xmlString)

    override val selector: XSSelector get() = rawPart.selector

    override val fields: List<XSField> get() = rawPart.fields

    override val mdlIdentityConstraintCategory: IdentityConstraintModel.Category
        get() = IdentityConstraintModel.Category.UNIQUE

    override fun check() {
        super<ResolvedNamedIdentityConstraint>.check()
        checkNotNull(rawPart.name)
    }

}

class ResolvedIndirectUnique(
    override val rawPart: XSUnique,
    schema: ResolvedSchemaLike,
    owner: ResolvedElement,
): ResolvedIndirectIdentityConstraint(schema, owner), T_Unique, ResolvedUnique, IdentityConstraintModel.Unique {

    override val constraint: ResolvedIndirectUnique
        get() = this

    override val ref: ResolvedDirectUnique = when (val r = schema.identityConstraint(requireNotNull(rawPart.ref))) {
        is ResolvedDirectUnique -> r
        is ResolvedIndirectUnique -> r.ref
        else -> throw IllegalArgumentException("Unique's ref property ${rawPart.ref} does not refer to a unique")
    }

    override fun check() {
        super<ResolvedIndirectIdentityConstraint>.check()
        check(rawPart.name == null)
    }

}
