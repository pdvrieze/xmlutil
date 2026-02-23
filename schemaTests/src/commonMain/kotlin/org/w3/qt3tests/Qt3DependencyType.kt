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

package org.w3.qt3tests

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
enum class Qt3DependencyType {
    @SerialName("calendar")
    CALENDAR,
    @SerialName("collection-stability")
    COLLECTION_STABILITY,
    @SerialName("default-language")
    DEFAULT_LANGUAGE,
    @SerialName("directory-as-collection-uri")
    DIRECTORY_AS_COLLECTION_URI,
    @SerialName("feature")
    FEATURE,
    @SerialName("format-integer-sequence")
    FORMAT_INTEGER_SEQUENCE,
    @SerialName("language")
    LANGUAGE,
    @SerialName("limits")
    LIMITS,
    @SerialName("spec")
    SPEC,
    @SerialName("schemaAware")
    SCHEMAAWARE,
    @SerialName("unicode-normalization-form")
    UNICODE_NORMALIZATION_FORM,
    @SerialName("unicode-version")
    UNICODE_VERSION,
    @SerialName("xml-version")
    XML_VERSION,
    @SerialName("xsd-version")
    XSD_VERSION,
}
