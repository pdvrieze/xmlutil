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

import io.github.pdvrieze.formats.xmlschema.datatypes.primitiveInstances.VID
import io.github.pdvrieze.formats.xmlschema.datatypes.primitiveInstances.VNCName
import io.github.pdvrieze.formats.xmlschema.datatypes.serialization.*
import io.github.pdvrieze.formats.xmlschema.types.T_KeyRef
import io.github.pdvrieze.formats.xmlschema.types.T_IdentityConstraint
import io.github.pdvrieze.formats.xmlschema.types.T_Key
import io.github.pdvrieze.formats.xmlschema.types.T_Unique
import nl.adaptivity.xmlutil.QName

sealed class ResolvedIdentityConstraint(
    override val schema: ResolvedSchemaLike,
    val owner: ResolvedElement
) : OptNamedPart, T_IdentityConstraint {
    abstract override val rawPart: T_IdentityConstraint
    override val name: VNCName? get() = rawPart.name
}

class ResolvedKeyRef(
    override val rawPart: T_KeyRef,
    schema: ResolvedSchemaLike,
    owner: ResolvedElement,
): ResolvedIdentityConstraint(schema, owner), OptNamedPart, T_KeyRef {
    override val id: VID? get() = rawPart.id

    override val name: VNCName get() = checkNotNull(rawPart.name)

    override val refer: QName get() = rawPart.refer

    val referenced: ResolvedKey by lazy {
        schema.identityConstraint(refer) as? ResolvedKey ?: error("A keyref must reference a key")
    }


    override val selector: XSSelector? get() = rawPart.selector

    override val fields: List<XSField> get() = rawPart.fields
    override val ref: QName?
        get() = rawPart.ref
    override val annotation: XSAnnotation? get() = rawPart.annotation
    override val otherAttrs: Map<QName, String> get() = rawPart.otherAttrs

    override fun check() {
        super<ResolvedIdentityConstraint>.check()
        checkNotNull(rawPart.name)
        check (referenced.fields.size == fields.size) { "Key(${referenced.qName}) and keyrefs(${qName}) must have equal field counts" }
    }

}

class ResolvedKey(
    override val rawPart: XSKey,
    schema: ResolvedSchemaLike,
    owner: ResolvedElement,
): ResolvedIdentityConstraint(schema, owner), OptNamedPart, T_Key {
    override val id: VID? get() = rawPart.id

    override val name: VNCName get() = checkNotNull(rawPart.name)

    override val selector: XSSelector? get() = rawPart.selector

    override val fields: List<XSField> get() = rawPart.fields
    override val ref: QName?
        get() = rawPart.ref
    override val annotation: XSAnnotation? get() = rawPart.annotation
    override val otherAttrs: Map<QName, String> get() = rawPart.otherAttrs

    override fun check() {
        super<ResolvedIdentityConstraint>.check()
        checkNotNull(rawPart.name)
    }

}

class ResolvedUnique(
    override val rawPart: XSUnique,
    schema: ResolvedSchemaLike,
    owner: ResolvedElement,
): ResolvedIdentityConstraint(schema, owner), OptNamedPart, T_Unique {
    override val id: VID? get() = rawPart.id

    override val name: VNCName get() = checkNotNull(rawPart.name)

    override val selector: XSSelector? get() = rawPart.selector

    override val fields: List<XSField> get() = rawPart.fields
    override val ref: QName? get() = rawPart.ref
    override val annotation: XSAnnotation? get() = rawPart.annotation
    override val otherAttrs: Map<QName, String> get() = rawPart.otherAttrs

    override fun check() {
        super<ResolvedIdentityConstraint>.check()
        checkNotNull(rawPart.name)
    }

}
