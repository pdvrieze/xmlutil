/*
 * Copyright (c) 2026.
 *
 * This file is part of xmlutil.
 *
 * This file is licenced to you under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance
 * with the License.  You should have  received a copy of the license
 * with the source distribution. Alternatively, you may obtain a copy
 * of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or
 * implied.  See the License for the specific language governing
 * permissions and limitations under the License.
 */

package org.w3.qt3tests.resolved

import io.github.pdvrieze.xml.schematypes.values.XsdToken
import org.w3.qt3tests.*

class ResolvedQt3TestCase(
    val description: Qt3Description? = null,
    val created: Qt3Created? = null,
    val modified: List<Qt3Modified> = emptyList(),
    val environment: Qt3Environment? = null,
    val modules: List<Qt3Module> = emptyList(),
    val dependencies: List<Qt3Dependency> = emptyList(),
    val test: ResolvedQt3Test? = null,
    val result: ResolvedQt3Result? = null,
    val name: String? = null,
    val covers: List<XsdToken>? = emptyList(),
    val covers30: List<io.github.pdvrieze.xml.schematypes.values.XsdNCName>? = emptyList(),
) {

}
