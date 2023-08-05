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

import io.github.pdvrieze.formats.xmlschema.datatypes.impl.SingleLinkedList
import io.github.pdvrieze.formats.xmlschema.resolved.*
import nl.adaptivity.xmlutil.QName

class CheckHelper(private val schema: ResolvedSchemaLike) {
    private val checkedTypes: MutableSet<QName> = HashSet()
    private val checkedElements: MutableSet<QName> = HashSet()
    private val checkedAttributes: MutableSet<QName> = HashSet()
    private val checkedAttributeGroups: MutableSet<QName> = HashSet()
    private val checkedConstraints: MutableSet<QName> = HashSet()
    private val checkedGroups: MutableSet<QName> = HashSet()

    private val checkHelper get() = this

    fun checkType(name: QName) {
        if (checkedTypes.add(name)) {
            schema.type(name).checkType(this)
        }
    }

    fun checkType(type: ResolvedType, inheritedTypes: SingleLinkedList<QName>) {
        when (type) {
            is ResolvedGlobalType -> {
                val name = type.mdlQName
                if (checkedTypes.add(name)) {
                    require(name !in inheritedTypes) { "Recursive presence of $name" }
                    type.checkType(this, inheritedTypes)
                }
            }

            else -> type.checkType(this, inheritedTypes)
        }
    }

    fun checkElement(name: QName) {
        if (checkedElements.add(name)) {
            schema.element(name).checkTerm(this)
        }
    }

    fun checkElement(element: ResolvedElement) {
        if (element !is ResolvedGlobalElement || checkedElements.add(element.mdlQName)) {
            element.checkTerm(this)
        }
    }

    fun checkAttribute(name: QName) {
        if (checkedAttributes.add(name)) {
            schema.attribute(name).checkAttribute(this)
        }
    }

    fun checkAttribute(attribute: ResolvedAttributeDef) {
        if (attribute !is ResolvedGlobalAttribute || checkedAttributes.add(attribute.mdlQName)) {
            attribute.checkAttribute(this)
        }
    }

    fun checkAttributeGroup(name: QName) {
        if (checkedAttributeGroups.add(name)) {
            schema.attributeGroup(name).checkAttributeGroup(this)
        }
    }

    fun checkAttributeGroup(attributeGroup: ResolvedGlobalAttributeGroup) {
        if (checkedAttributeGroups.add(attributeGroup.mdlQName)) {
            attributeGroup.checkAttributeGroup(this)
        }
    }

    fun checkConstraint(name: QName) {
        if (checkedConstraints.add(name)) {
            schema.identityConstraint(name).check(mutableSetOf())
        }
    }

    fun checkConstraint(constraint: ResolvedIdentityConstraint) {
        val name = constraint.mdlQName
        if (name == null || checkedConstraints.add(name)) {
            constraint.check(mutableSetOf())
        }
    }

    fun checkGroup(group: ResolvedGlobalGroup) {
        if (checkedGroups.add(group.mdlQName)) {
            group.checkGroup(this)
        }
    }

    fun checkGroup(name: QName) {
        if (checkedGroups.add(name)) {
            schema.modelGroup(name).checkGroup(this)
        }
    }
}
