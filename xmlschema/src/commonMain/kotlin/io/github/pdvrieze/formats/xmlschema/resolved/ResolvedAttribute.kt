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

import io.github.pdvrieze.formats.xmlschema.datatypes.AnySimpleType
import io.github.pdvrieze.formats.xmlschema.datatypes.primitiveInstances.VAnyURI
import io.github.pdvrieze.formats.xmlschema.datatypes.primitiveInstances.VID
import io.github.pdvrieze.formats.xmlschema.datatypes.primitiveInstances.VNCName
import io.github.pdvrieze.formats.xmlschema.datatypes.primitiveInstances.VString
import io.github.pdvrieze.formats.xmlschema.datatypes.serialization.XSAnnotation
import io.github.pdvrieze.formats.xmlschema.datatypes.serialization.XSAttrUse
import io.github.pdvrieze.formats.xmlschema.datatypes.serialization.XSAttribute
import nl.adaptivity.xmlutil.QName

sealed class ResolvedAttribute(
    override val schema: ResolvedSchemaLike
) : ResolvedAnnotated, ResolvedSimpleTypeContext {
    abstract override val rawPart: XSAttribute

    override val id: VID?
        get() = rawPart.id

    abstract val default: VString?

    abstract val fixed: VString?

    val mdlQName: QName
        get() = QName(mdlTargetNamespace?.value ?: "", mdlName.xmlString)

    open val type: QName? // TODO make abstract
        get() = (resolvedType as? ResolvedGlobalSimpleType)?.mdlQName

    final override val annotation: XSAnnotation?
        get() = rawPart.annotation


    val resolvedType: ResolvedSimpleType by lazy {
        rawPart.type?.let {
            require(rawPart.simpleType == null) { "3.2.3(4) both simpletype and type attribute present" }
            schema.simpleType(it)
        } ?: rawPart.simpleType?.let { ResolvedLocalSimpleType(it, schema, this) }
        ?: AnySimpleType
    }

    val valueConstraint: ValueConstraint? get() = mdlValueConstraint

    final override val mdlAnnotations: ResolvedAnnotation?
        get() = rawPart.annotation.models()


    abstract val mdlName: VNCName

    abstract val mdlTargetNamespace: VAnyURI?

    val mdlInheritable: Boolean get() = rawPart.inheritable ?: false

    abstract val mdlTypeDefinition: ResolvedSimpleType
    abstract val mdlScope: VAttributeScope
    val mdlValueConstraint: ValueConstraint? by lazy { ValueConstraint(rawPart) }
    abstract val targetNamespace: VAnyURI?

    override fun check(checkedTypes: MutableSet<QName>) {
        super.check(checkedTypes)

        resolvedType.check(checkedTypes)

        valueConstraint?.let { resolvedType.validate(it.value)}
    }

}

