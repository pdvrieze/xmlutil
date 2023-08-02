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
import io.github.pdvrieze.formats.xmlschema.datatypes.serialization.XSKeyRef
import io.github.pdvrieze.formats.xmlschema.datatypes.serialization.XSSelector
import nl.adaptivity.xmlutil.QName
import nl.adaptivity.xmlutil.qname

class ResolvedDirectKeyRef(override val rawPart: XSKeyRef, schema: ResolvedSchemaLike, owner: ResolvedElement) :
    ResolvedNamedIdentityConstraint(schema, owner), ResolvedKeyRef {
    override val name: VNCName = requireNotNull(rawPart.name)

    override val constraint: ResolvedDirectKeyRef
        get() = this

    init {
        require(rawPart.ref == null) { "A key reference can either have a name or ref" }
    }

    override val refer: QName get() = requireNotNull(rawPart.refer)

    override val qName: QName get() = qname(schema.targetNamespace?.value, mdlName.xmlString)

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
        super<ResolvedNamedIdentityConstraint>.check(checkedTypes)
        checkNotNull(rawPart.name)
        check(referenced.fields.size == fields.size) { "Key(${referenced.qName}) and keyrefs(${qName}) must have equal field counts" }
    }

}
