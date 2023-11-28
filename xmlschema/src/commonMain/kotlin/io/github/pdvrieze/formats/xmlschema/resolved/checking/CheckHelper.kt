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

class CheckHelper(private val schema: ResolvedSchemaLike) {
    private val checkedTypes: MutableSet<ResolvedType> = HashSet()
    private val checkedElements: MutableSet<ResolvedGlobalElement> = HashSet()
    private val checkedAttributes: MutableSet<ResolvedGlobalAttribute> = HashSet()
    private val checkedAttributeGroups: MutableSet<ResolvedGlobalAttributeGroup> = HashSet()
    private val checkedConstraints: MutableSet<ResolvedIdentityConstraint> = HashSet()
    private val checkedGroups: MutableSet<ResolvedGlobalGroup> = HashSet()
    private val checkedNotations: MutableSet<ResolvedNotation> = HashSet()

    val version: SchemaVersion get() = schema.version

    private val checkHelper get() = this

    fun checkType(name: QName) {
        checkType(schema.type(name))
    }

    fun checkType(type: ResolvedType) {
        when (type) {
            is ResolvedGlobalType -> {
                if (checkedTypes.add(type)) {
                    type.checkType(this)
                }
            }

            else -> type.checkType(this)
        }
    }

    fun checkElement(name: QName) {
        val element = schema.element(name)
        if (checkedElements.add(element)) {
            element.checkTerm(this)
        }
    }

    fun checkElement(element: ResolvedElement) {
        if (element !is ResolvedGlobalElement || checkedElements.add(element)) {
            element.checkTerm(this)
        }
    }

    fun checkAttribute(name: QName) {
        val attribute = schema.attribute(name)
        if (checkedAttributes.add(attribute)) {
            attribute.checkAttribute(this)
        }
    }

    fun checkAttribute(attribute: ResolvedAttributeDef) {
        if (attribute !is ResolvedGlobalAttribute || checkedAttributes.add(attribute)) {
            attribute.checkAttribute(this)
        }
    }

    fun checkAttributeGroup(
        attributeGroup: ResolvedGlobalAttributeGroup,
        seen: MutableSet<ResolvedGlobalAttributeGroup> = mutableSetOf()
    ) {
        if (version == SchemaVersion.V1_0 && attributeGroup in seen) {
            throw IllegalStateException("Circular attribute group (in 1.0 mode): ${attributeGroup.mdlQName}")
        } else {
            seen.add(attributeGroup)
        }
        if (checkedAttributeGroups.add(attributeGroup)) {
            attributeGroup.checkAttributeGroup(this, seen)
        }
    }

    fun checkConstraint(name: QName) {
        checkConstraint(schema.identityConstraint(name))
    }

    fun checkConstraint(constraint: ResolvedIdentityConstraint) {
        if (checkedConstraints.add(constraint)) {
            constraint.checkConstraint(this)
        }
    }

    fun checkGroup(group: ResolvedGlobalGroup) {
        if (checkedGroups.add(group)) {
            group.checkGroup(this)
        }
    }

    fun checkGroup(name: QName) {
        checkGroup(schema.modelGroup(name))
    }

    fun checkNotation(name: SerializableQName) {
        val notation = schema.notation(name)
        if (checkedNotations.add(notation)) {
            notation.check()
        }
    }
}
