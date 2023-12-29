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

import io.github.pdvrieze.formats.xmlschema.datatypes.serialization.XSAttributeGroupRef
import io.github.pdvrieze.formats.xmlschema.resolved.checking.CheckHelper
import nl.adaptivity.xmlutil.QName

class ResolvedAttributeGroupRef(
    rawPart: XSAttributeGroupRef,
    schema: ResolvedSchemaLike
) : ResolvedAnnotated {

    override val model: Model by lazy { Model(rawPart, schema) }

    val ref: QName = rawPart.ref

    val resolvedGroup: ResolvedGlobalAttributeGroup
        get() = model.resolvedGroup

    fun checkRef(checkHelper: CheckHelper) {
        checkHelper.checkAttributeGroup(resolvedGroup)
    }

    class Model(rawPart: XSAttributeGroupRef, schema: ResolvedSchemaLike): ResolvedAnnotated.Model(rawPart) {
        val resolvedGroup = schema.attributeGroup(rawPart.ref)
    }
}
