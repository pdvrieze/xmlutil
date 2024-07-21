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

package io.github.pdvrieze.formats.xmlschema.resolved.checking

import io.github.pdvrieze.formats.xmlschema.resolved.*
import nl.adaptivity.xmlutil.QName
import nl.adaptivity.xmlutil.SerializableQName

fun CheckHelper(schema: ResolvedSchemaLike, isLax: Boolean): CheckHelper =
    CheckHelperImpl(schema, isLax)

abstract class CheckHelper protected constructor(
    internal val schema: ResolvedSchemaLike
) {
    internal abstract val isLax: Boolean
    val version: SchemaVersion get() = schema.version
    abstract fun checkType(name: QName)
    abstract fun checkType(type: ResolvedType)
    abstract fun checkElement(name: QName)
    abstract fun checkElement(element: ResolvedElement)
    abstract fun checkAttribute(name: QName)
    abstract fun checkAttribute(attribute: ResolvedAttributeDef)
    abstract fun checkAttributeGroup(
        attributeGroup: ResolvedGlobalAttributeGroup,
        seen: MutableSet<ResolvedGlobalAttributeGroup> = mutableSetOf()
    )

    abstract fun checkConstraint(name: QName)
    abstract fun checkConstraint(constraint: ResolvedIdentityConstraint)
    abstract fun checkGroup(group: ResolvedGlobalGroup)
    abstract fun checkGroup(name: QName)
    abstract fun checkNotation(name: SerializableQName)

    fun checkLax(t: Throwable): Unit {
        if (!isLax) throw t
    }
}

