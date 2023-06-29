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

package io.github.pdvrieze.formats.xmlschema.types

interface I_NestedParticles : I_NestedAllParticles {
    val choices: List<T_Choice>
    val sequences: List<T_Sequence>
}

interface I_NestedAllParticles {
    val elements: List<T_LocalElement>
    val groups: List<T_GroupRef>
    val anys: List<T_AnyElement>
}

interface T_NestedAllParticle: T_Particle

interface I_GroupParticles {
    val particles: List<T_ExplicitGroupParticle>
    val choices: List<T_Choice>
    val sequences: List<T_Sequence>
    val alls: List<T_All>
}
