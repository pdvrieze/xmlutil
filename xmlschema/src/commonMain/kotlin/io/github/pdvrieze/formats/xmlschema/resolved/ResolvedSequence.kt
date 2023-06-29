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

import io.github.pdvrieze.formats.xmlschema.datatypes.serialization.XSSequence
import io.github.pdvrieze.formats.xmlschema.model.ChoiceSeqTerm
import io.github.pdvrieze.formats.xmlschema.model.ModelGroupModel
import io.github.pdvrieze.formats.xmlschema.model.SequenceModel
import io.github.pdvrieze.formats.xmlschema.types.T_Sequence

class ResolvedSequence(
    parent: ResolvedComplexType?,
    override val rawPart: XSSequence,
    override val schema: ResolvedSchemaLike
) : ResolvedExplicitGroup<ResolvedSequence>(parent, schema), T_Sequence,
    ResolvedComplexType.ResolvedDirectParticle<ResolvedSequence>,
    ResolvedChoiceSeqTerm,
    SequenceModel<ResolvedSequence>, ChoiceSeqTerm {

    override val mdlCompositor: ModelGroupModel.Compositor get() = ModelGroupModel.Compositor.SEQUENCE
    override val mdlTerm: ResolvedSequence get() = this

    override val mdlParticles: List<ResolvedParticle<*>> = DelegateList(rawPart.particles) {
        ResolvedParticle(parent, it, schema)
    }

    /*
        override val choices: List<ResolvedChoice> =
            DelegateList(rawPart.choices) { ResolvedChoice(parent, it, schema) }

        override val sequences: List<ResolvedSequence> =
            DelegateList(rawPart.sequences) { ResolvedSequence(parent, it, schema) }
    */

    /*
        init {
            require(minOccurs.toUInt() <= 1.toUInt()) { "minOccurs must be 0 or 1, but was $minOccurs"}
            require(maxOccurs.toUInt() <= 1.toUInt()) { "maxOccurs must be 0 or 1, but was $maxOccurs"}
        }
    */
}
