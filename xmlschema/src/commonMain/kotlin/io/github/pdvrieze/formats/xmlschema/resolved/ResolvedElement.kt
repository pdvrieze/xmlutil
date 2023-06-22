/*
 * Copyright (c) 2021.
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
import io.github.pdvrieze.formats.xmlschema.datatypes.serialization.XSAnnotation
import io.github.pdvrieze.formats.xmlschema.datatypes.serialization.XSAttribute
import io.github.pdvrieze.formats.xmlschema.types.*
import nl.adaptivity.xmlutil.QName

sealed class ResolvedElement(final override val schema: ResolvedSchemaLike) : OptNamedPart, T_Element {
    abstract override val rawPart: T_Element
    abstract val scope: T_Scope

    override val type: QName?
        get() = rawPart.type
    override val nillable: Boolean get() = rawPart.nillable ?: false

    override val default: String? get() = rawPart.default
    override val fixed: String? get() = rawPart.fixed
    val valueConstraint: ValueConstraint? by lazy {
        val rawDefault = rawPart.default
        val rawFixed = rawPart.fixed
        when {
            rawDefault != null && rawFixed != null ->
                throw IllegalArgumentException("An element ${rawPart.name} cannot have default and fixed attributes both")

            rawDefault != null -> ValueConstraint.Default(rawDefault)
            rawFixed != null -> ValueConstraint.Fixed(rawFixed)
            else -> null
        }
    }
    override val id: VID? get() = rawPart.id

    override val localType: T_Type?
        get() = rawPart.localType

    override val name: VNCName get() = rawPart.name ?: error("Missing name")

    override val annotation: XSAnnotation? get() = rawPart.annotation

    override val alternatives: List<T_AltType> get() = rawPart.alternatives

    abstract override val uniques: List<ResolvedUnique>

    abstract override val keys: List<ResolvedKey>

    abstract override val keyrefs: List<ResolvedKeyRef>

    /**
     * disallowed substitutions
     */
    override val block: Set<T_BlockSetValues> get() = rawPart.block ?: schema.blockDefault

    override val otherAttrs: Map<QName, String>
        get() = rawPart.otherAttrs

    override fun check() {
        super<OptNamedPart>.check()
        for (keyref in keyrefs) {
            keyref.check()
            checkNotNull(keyref.referenced)
        }
    }
}

class TypeTable(alternatives: List<T_AltType>, default: T_AltType?)

sealed class ValueConstraint(val value: String) {
    class Default(value: String) : ValueConstraint(value)
    class Fixed(value: String) : ValueConstraint(value)

    companion object {
        operator fun invoke(attr: XSAttribute): ValueConstraint? {
            return when {
                attr.default != null -> { check(attr.use!=null); Default(attr.default) }
                attr.fixed != null -> Fixed(attr.fixed)
                else -> null
            }
        }
    }
}
