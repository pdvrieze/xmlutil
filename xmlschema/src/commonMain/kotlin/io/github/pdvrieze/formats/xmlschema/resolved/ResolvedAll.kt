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

import io.github.pdvrieze.formats.xmlschema.datatypes.serialization.XSAll
import io.github.pdvrieze.formats.xmlschema.model.AllModel
import io.github.pdvrieze.formats.xmlschema.model.ModelGroupModel
import io.github.pdvrieze.formats.xmlschema.types.T_All
import io.github.pdvrieze.formats.xmlschema.types.T_AllNNI

class ResolvedAll(
    parent: ResolvedComplexType?,
    override val rawPart: XSAll,
    override val schema: ResolvedSchemaLike
) : ResolvedExplicitGroup<ResolvedAll>(parent, schema), T_All, ResolvedComplexType.ResolvedDirectParticle<ResolvedAll>,
    AllModel<ResolvedAll> {
    override val mdlParticles: List<ResolvedParticle<ResolvedAllTerm>> = DelegateList(rawPart.particles) {
        ResolvedParticle(parent, it, schema)
    }


    override val mdlTerm: ResolvedAll get() = this
    override val mdlCompositor: ModelGroupModel.Compositor get() = ModelGroupModel.Compositor.ALL
    override val maxOccurs: T_AllNNI.Value get() = super<ResolvedExplicitGroup>.maxOccurs as T_AllNNI.Value

    init {
        require(minOccurs.toUInt() <= 1.toUInt()) { "minOccurs must be 0 or 1, but was $minOccurs" }
        require(maxOccurs.let { it is T_AllNNI.Value && it.toUInt() <= 1.toUInt() }) { "maxOccurs must be 0 or 1, but was $maxOccurs" }
    }
}
