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

import io.github.pdvrieze.formats.xmlschema.datatypes.serialization.XSComplexType
import io.github.pdvrieze.formats.xmlschema.model.*
import io.github.pdvrieze.formats.xmlschema.types.T_ComplexType
import io.github.pdvrieze.formats.xmlschema.types.T_DerivationSet

sealed class ResolvedComplexType(
    final override val schema: ResolvedSchemaLike
) : ResolvedType, T_ComplexType, ResolvedLocalAttribute.Parent, ComplexTypeModel {
    abstract override val rawPart: XSComplexType

    abstract override val content: ResolvedComplexTypeContent

    protected abstract val model: Model

    override val mdlAbstract: Boolean get() = model.mdlAbstract
    override val mdlProhibitedSubstitutions: T_DerivationSet get() = model.mdlProhibitedSubstitutions
    override val mdlFinal: T_DerivationSet get() = model.mdlFinal
    override val mdlContentType: ResolvedGlobalComplexType.ResolvedContentType get() = model.mdlContentType
    override val mdlAttributeUses: Set<AttributeModel.Use> get() = model.mdlAttributeUses
    override val mdlAttributeWildcard: WildcardModel get() = model.mdlAttributeWildcard
    override val mdlBaseTypeDefinition: ResolvedType get() = model.mdlBaseTypeDefinition
    override val mdlDerivationMethod: ComplexTypeModel.DerivationMethod get() = model.mdlDerivationMethod
    override val mdlAnnotations: AnnotationModel? get() = model.mdlAnnotations

    sealed interface ResolvedDirectParticle<T: ResolvedTerm>: ResolvedParticle<T>, T_ComplexType.DirectParticle

    interface Model : ComplexTypeModel {
        override val mdlBaseTypeDefinition: ResolvedType
        override val mdlFinal: T_DerivationSet
        override val mdlContentType: ResolvedGlobalComplexType.ResolvedContentType
    }

    protected abstract class ModelBase(rawPart: XSComplexType, schema: ResolvedSchemaLike) : Model {
        override val mdlAnnotations: AnnotationModel? = rawPart.annotation.models()
    }
}
