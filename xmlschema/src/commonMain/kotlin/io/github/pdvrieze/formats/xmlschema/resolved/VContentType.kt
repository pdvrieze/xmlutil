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

interface VContentType {
    val mdlVariety: ResolvedComplexType.Variety

    interface Empty : VContentType {
        override val mdlVariety: ResolvedComplexType.Variety get() = ResolvedComplexType.Variety.EMPTY
    }

    interface Simple : VContentType {
        override val mdlVariety: ResolvedComplexType.Variety get() = ResolvedComplexType.Variety.SIMPLE
        val mdlSimpleTypeDefinition: ResolvedSimpleType
    }

    interface ElementBase : VContentType {
        val mdlParticle: ResolvedParticle<ResolvedTerm>
        val openContent: ResolvedOpenContent?
    }

    interface ElementOnly : ElementBase {
        override val mdlVariety: ResolvedComplexType.Variety get() = ResolvedComplexType.Variety.ELEMENT_ONLY
    }

    interface Mixed : ElementBase {
        override val mdlVariety: ResolvedComplexType.Variety get() = ResolvedComplexType.Variety.MIXED
    }
}
