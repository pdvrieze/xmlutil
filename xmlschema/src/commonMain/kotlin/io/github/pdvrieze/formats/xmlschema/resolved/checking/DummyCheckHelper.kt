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

class DummyCheckHelper(schema: ResolvedSchemaLike) : CheckHelper(schema) {
    override val isLax = false
    override fun checkType(name: QName) {}

    override fun checkType(type: ResolvedType) {}

    override fun checkElement(name: QName) {}

    override fun checkElement(element: ResolvedElement) {}

    override fun checkAttribute(name: QName) {}

    override fun checkAttribute(attribute: ResolvedAttributeDef) {}

    override fun checkAttributeGroup(
        attributeGroup: ResolvedGlobalAttributeGroup,
        seen: MutableSet<ResolvedGlobalAttributeGroup>
    ) {}

    override fun checkConstraint(name: QName) {}

    override fun checkConstraint(constraint: ResolvedIdentityConstraint) {}

    override fun checkGroup(group: ResolvedGlobalGroup) {}

    override fun checkGroup(name: QName) {}

    override fun checkNotation(name: SerializableQName) {}
}