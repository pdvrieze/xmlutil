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

package io.github.pdvrieze.formats.xmlschema.regex.impl

import io.github.pdvrieze.formats.xmlschema.resolved.SchemaVersion

enum class RegexVariant {
    Schema1_0,
    Schema1_1,
    XPath_2_0,
    ;

    fun equals(other: SchemaVersion): Boolean {
        return when (other) {
            SchemaVersion.V1_0 -> this == Schema1_0
            SchemaVersion.V1_1 -> this == Schema1_1
        }
    }

    companion object {
        fun of(version: SchemaVersion): RegexVariant = when (version) {
            SchemaVersion.V1_0 -> Schema1_0
            SchemaVersion.V1_1 -> Schema1_1
        }
    }
}
