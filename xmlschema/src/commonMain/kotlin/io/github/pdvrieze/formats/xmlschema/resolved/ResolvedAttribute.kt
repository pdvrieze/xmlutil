/*
 * Copyright (c) 2022.
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
import io.github.pdvrieze.formats.xmlschema.datatypes.serialization.XSAttribute
import nl.adaptivity.xmlutil.QName

sealed class ResolvedAttribute(
    override val schema: ResolvedSchemaLike
) : ResolvedAnnotated, VSimpleTypeScope.Member {
    abstract override val rawPart: XSAttribute
    protected abstract val model: Model

    override val id: VID? get() = rawPart.id

    val mdlInheritable: Boolean get() = rawPart.inheritable ?: false

    abstract val mdlScope: VAttributeScope

    val mdlQName: QName get() = model.mdlQName
    val mdlTypeDefinition: ResolvedSimpleType get() = model.mdlTypeDefinition
    final override val mdlAnnotations: ResolvedAnnotation? get() = model.mdlAnnotations
    val mdlValueConstraint: ValueConstraint? get() = model.mdlValueConstraint

    override fun check(checkedTypes: MutableSet<QName>) {
        super.check(checkedTypes)

        mdlTypeDefinition.check(checkedTypes)

        mdlValueConstraint?.let { mdlTypeDefinition.validate(it.value)}
    }

    protected abstract class Model(base: ResolvedAttribute, schema: ResolvedSchemaLike) {
        abstract val mdlQName: QName

        abstract val mdlTypeDefinition: ResolvedSimpleType

        val mdlValueConstraint: ValueConstraint? = ValueConstraint(base.rawPart)

        val mdlAnnotations: ResolvedAnnotation? = base.rawPart.annotation.models()
    }

}

