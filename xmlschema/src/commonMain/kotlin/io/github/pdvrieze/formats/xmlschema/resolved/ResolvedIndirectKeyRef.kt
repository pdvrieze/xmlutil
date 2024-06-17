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

import io.github.pdvrieze.formats.xmlschema.datatypes.serialization.XSKeyRef
import io.github.pdvrieze.formats.xmlschema.resolved.checking.CheckHelper
import nl.adaptivity.xmlutil.QName

class ResolvedIndirectKeyRef(rawPart: XSKeyRef, schema: ResolvedSchemaLike, owner: ResolvedElement) :
    ResolvedIndirectIdentityConstraint<ResolvedDirectKeyRef>(rawPart, schema, owner), ResolvedKeyRef {

    init {
        checkNotNull(rawPart.name)
    }

    override val constraint: ResolvedIndirectKeyRef get() = this

    override val _ref: Lazy<ResolvedDirectKeyRef> = lazy {
        when (val r = schema.identityConstraint(requireNotNull(rawPart.ref))) {
            is ResolvedDirectKeyRef -> r
            is ResolvedIndirectKeyRef -> r.ref
            else -> throw IllegalArgumentException("Keyref's ref property ${rawPart.ref} does not refer to a keyref")
        }
    }

    override val mdlQName: QName? = rawPart.name?.toQname(schema.targetNamespace)

    override val refer: Nothing? get() = null

    init {
        require(rawPart.name == null) { "A key reference can either have a name or ref" }
    }

    val referenced: ResolvedDirectReferenceable get() = ref.referenced

    override val mdlReferencedKey: ResolvedReferenceableConstraint get() = ref.mdlReferencedKey

    override fun checkConstraint(checkHelper: CheckHelper) {
        super.checkConstraint(checkHelper)
        checkNotNull(mdlReferencedKey)
        check(referenced.fields.size == fields.size) { "Key(${referenced.mdlQName}) and keyrefs(${ref.mdlQName}) must have equal field counts" }
    }

}
