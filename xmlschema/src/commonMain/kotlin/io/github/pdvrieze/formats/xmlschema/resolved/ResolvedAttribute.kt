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
import io.github.pdvrieze.formats.xmlschema.datatypes.primitiveInstances.VNCName
import io.github.pdvrieze.formats.xmlschema.datatypes.serialization.XSAnnotation
import io.github.pdvrieze.formats.xmlschema.datatypes.serialization.XSAttrUse
import io.github.pdvrieze.formats.xmlschema.datatypes.serialization.XSAttribute
import io.github.pdvrieze.formats.xmlschema.model.AnnotationModel
import io.github.pdvrieze.formats.xmlschema.model.AttributeModel
import io.github.pdvrieze.formats.xmlschema.model.SimpleTypeContext
import io.github.pdvrieze.formats.xmlschema.types.T_AttributeBase
import io.github.pdvrieze.formats.xmlschema.types.T_FormChoice
import nl.adaptivity.xmlutil.QName
import nl.adaptivity.xmlutil.qname

sealed class ResolvedAttribute(
    override val schema: ResolvedSchemaLike
) : ResolvedPart, T_AttributeBase, SimpleTypeContext, ResolvedAttributeDecl {
    abstract override val rawPart: XSAttribute

    abstract override val name: VNCName

    val mdlQName: QName
        get() = qname((targetNamespace ?: schema.targetNamespace)?.value, name.xmlString)

    override val type: QName? // TODO make abstract
        get() = (resolvedType as? ResolvedGlobalSimpleType)?.qName

    final override val annotation: XSAnnotation?
        get() = rawPart.annotation


    val resolvedType: ResolvedSimpleType by lazy {
        rawPart.type?.let { schema.simpleType(it) }
            ?: rawPart.simpleType?.let { ResolvedLocalSimpleType(it, schema, this) }
            ?: AnySimpleType
    }

    val valueConstraint: ValueConstraint? by lazy { ValueConstraint(rawPart) }

    final override val mdlTargetNamespace: VAnyURI? by lazy {
        targetNamespace ?: when {
            (rawPart.form ?: (schema as ResolvedSchema)) == T_FormChoice.QUALIFIED ->
                schema.targetNamespace

            else -> null
        }
    }


    final override val mdlInheritable: Boolean
        get() = rawPart.inheritable ?: false

    final override val mdlAnnotations: AnnotationModel?
        get() = rawPart.annotation.models()

    override fun check() {
        super<ResolvedPart>.check()
        resolvedType.check()
        check (fixed==null || default==null) { "Attributes may not have both default and fixed values" }
        check (default == null || use == null || use == XSAttrUse.OPTIONAL) {
            "For attributes with default and use must have optional as use value"
        }
    }

    interface ResolvedScope : AttributeModel.ScopeModel
}

