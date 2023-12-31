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

package org.w3.xml.xmschematestsuite

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
enum class TSValidityOutcome {
    /**
     * The schema is valid, or the document is valid according to the schema.
     */
    @SerialName("valid")
    VALID,

    /**
     * The schema is not valid (check fails or fails to parse) or instance is not valid according to
     * the schema.
     */
    @SerialName("invalid")
    INVALID,

    /**
     * The validity is not known
     */
    @SerialName("notknown")
    NOTKNOWN,

    /**
     * The schema can only validate/check according to lax checks (ignoring missing elements).
     */
    @SerialName("lax")
    LAX,

    @SerialName("indeterminate")
    INDETERMINATE,

    @SerialName("implementation-defined")
    IMPLEMENTATION_DEFINED,

    @SerialName("implementation-dependent")
    IMPLEMENTATION_DEPENDENT,

    @SerialName("invalid-latent")
    INVALID_LATENT,

    @SerialName("runtime-schema-error")
    RUNTIME_SCHEMA_ERROR,
}

