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

import io.github.pdvrieze.formats.xmlschema.datatypes.serialization.XSField
import io.github.pdvrieze.formats.xmlschema.datatypes.serialization.XSKeyRef
import io.github.pdvrieze.formats.xmlschema.datatypes.serialization.XSSelector
import io.github.pdvrieze.formats.xmlschema.impl.invariantNotNull
import nl.adaptivity.xmlutil.QName

class ResolvedDirectKeyRef(override val rawPart: XSKeyRef, schema: ResolvedSchemaLike, owner: ResolvedElement) :
    ResolvedNamedIdentityConstraint(rawPart, schema, owner), ResolvedKeyRef {

    override val constraint: ResolvedDirectKeyRef
        get() = this

    init {
        require(rawPart.ref == null) { "A key reference can either have a name or ref" }
        requireNotNull(rawPart.refer)
    }

    override val refer: QName get() = invariantNotNull(rawPart.refer)

    override val mdlQName: QName = requireNotNull(rawPart.name).toQname(schema.targetNamespace)

    override val selector: XSSelector get() = rawPart.selector

    override val fields: List<XSField> get() = rawPart.fields

    val referenced: ResolvedDirectKey by lazy {
        schema.identityConstraint(refer) as? ResolvedDirectKey
            ?: throw NoSuchElementException("No identity constraint with name ${refer} exists")
    }

    override val mdlReferencedKey: ResolvedReferenceableConstraint
        get() = schema.identityConstraint(refer).let {
            check(it is ResolvedReferenceableConstraint) {
                "keyref can only refer to key or unique elements, not to other keyrefs"
            }
            it
        }

    override fun check(checkedTypes: MutableSet<QName>) {
        super.check(checkedTypes)
        checkNotNull(rawPart.name)
        check(referenced.fields.size == fields.size) { "Key(${referenced.mdlQName}) and keyrefs(${mdlQName}) must have equal field counts" }
    }

}
