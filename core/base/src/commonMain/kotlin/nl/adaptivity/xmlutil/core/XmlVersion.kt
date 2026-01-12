/*
 * Copyright (c) 2024-2026.
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

package nl.adaptivity.xmlutil.core

import nl.adaptivity.xmlutil.ExperimentalXmlUtilApi
import kotlin.jvm.JvmStatic

/**
 * Enum with supported XML versions.
 *
 * @property versionString String representation of the version.
 */
public enum class XmlVersion(public val versionString: String) {
    /** XML 1.0 */
    XML10("1.0"),
    /** XML 1.1 */
    XML11("1.1");

    @ExperimentalXmlUtilApi
    public companion object {
        @JvmStatic
        @ExperimentalXmlUtilApi
        public fun fromStringOrNull(versionString: String): XmlVersion? = entries.find { it.versionString == versionString }
    }
}
