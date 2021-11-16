/*
 * Copyright (c) 2021.
 *
 * This file is part of ProcessManager.
 *
 * ProcessManager is free software: you can redistribute it and/or modify it under the terms of version 3 of the
 * GNU Lesser General Public License as published by the Free Software Foundation.
 *
 * ProcessManager is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even
 * the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License along with ProcessManager.  If not,
 * see <http://www.gnu.org/licenses/>.
 */

@file:UseSerializers(QNameSerializer::class)

package io.github.pdvrieze.formats.xmlschema.datatypes.serialization

import io.github.pdvrieze.formats.xmlschema.XmlSchemaConstants
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.UseSerializers
import nl.adaptivity.xmlutil.QNameSerializer
import nl.adaptivity.xmlutil.serialization.XmlSerialName

enum class XSAttrUse {
    @SerialName("optional")
    OPTIONAL,

    @SerialName("prohibited")
    PROHIBITED,

    @SerialName("required")
    REQUIRED
}

enum class XSScopeVariety {
    @SerialName("global")
    GLOBAL,

    @SerialName("local")
    LOCAL
}

@Serializable
@XmlSerialName("valueConstraint", XmlSchemaConstants.XS_NAMESPACE, XmlSchemaConstants.XS_PREFIX)
class XSValueConstraint private constructor(
    val variety: XSValueConstraintVariety,
    val value: String,
    val lexicalForm: String
)

enum class XSValueConstraintVariety {
    @SerialName("default")
    DEFAULT,

    @SerialName("fixed")
    FIXED
}
