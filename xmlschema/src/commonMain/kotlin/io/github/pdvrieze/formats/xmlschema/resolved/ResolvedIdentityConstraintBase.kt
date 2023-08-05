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

import io.github.pdvrieze.formats.xmlschema.datatypes.primitiveInstances.VAnyURI
import io.github.pdvrieze.formats.xmlschema.datatypes.primitiveInstances.VID
import io.github.pdvrieze.formats.xmlschema.datatypes.primitiveInstances.XPathExpression
import io.github.pdvrieze.formats.xmlschema.datatypes.serialization.XSAnnotation
import io.github.pdvrieze.formats.xmlschema.datatypes.serialization.XSIdentityConstraint
import nl.adaptivity.xmlutil.QName

sealed class ResolvedIdentityConstraintBase(
    rawPart: XSIdentityConstraint,
    override val schema: ResolvedSchemaLike,
    val owner: ResolvedElement
) : ResolvedAnnotated {

    final override val otherAttrs: Map<QName, String> = rawPart.resolvedOtherAttrs()

    abstract override val rawPart: XSIdentityConstraint
    override val id: VID? get() = rawPart.id

    val annotation: XSAnnotation? get() = rawPart.annotation

    val mdlTargetNamespace: VAnyURI? get() = schema.targetNamespace

    override val mdlAnnotations: ResolvedAnnotation?
        get() = rawPart.annotation.models()

    abstract val constraint: ResolvedIdentityConstraint
    abstract val mdlSelector: XPathExpression
    abstract val mdlFields: List<XPathExpression>

}
