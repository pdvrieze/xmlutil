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

package io.github.pdvrieze.xml.schematypes.values.instances

import io.github.pdvrieze.xml.schematypes.types.BooleanType
import io.github.pdvrieze.xml.schematypes.values.XsdBoolean
import nl.adaptivity.xmlutil.XmlUtilInternal
import kotlin.jvm.JvmInline

@JvmInline
@XmlUtilInternal
value class XsdBooleanImpl(override val value: Boolean): XsdBoolean {
    operator fun not(): XsdBooleanImpl = XsdBooleanImpl(!value)

    override val xmlString: String get() = value.toString()
    override val schemaType: BooleanType<XsdBoolean> get() = BooleanType.Instance

    override fun toString(): String = xmlString
}
