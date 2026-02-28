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

import org.w3.qt3tests.*

class ResolvedQt3Environment(
    val name: String?,
    val schemas: List<Qt3Schema>,
    val sources: List<ResolvedQt3Source>,
    val resource: List<Qt3Resource>,
    val params: List<Qt3Param>,
    val contextItems: List<Qt3ContextItem>,
    val decimalFormats: List<Qt3DecimalFormat>,
    val namespaces: List<Qt3Namespace>,
    val functionLibraries: List<Qt3FunctionLibrary>,
    val collections: List<Qt3Collection>,
    val staticBaseUris: List<Qt3StaticBaseUri>,
    val collations: List<Qt3Collation>
) {
    init {
        require(name == null || name.isNotBlank()) { "Names can not be blank" }
    }
}
