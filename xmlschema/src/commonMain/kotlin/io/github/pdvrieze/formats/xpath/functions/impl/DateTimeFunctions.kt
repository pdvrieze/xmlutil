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

package io.github.pdvrieze.formats.xpath.functions.impl

import io.github.pdvrieze.formats.xpath.eval.ErrorCodes
import io.github.pdvrieze.formats.xpath.eval.EvaluationException
import io.github.pdvrieze.formats.xpath.eval.data.XdmAtomic
import io.github.pdvrieze.formats.xpath.eval.data.XdmSequence
import io.github.pdvrieze.formats.xpath.eval.data.XdmString
import io.github.pdvrieze.formats.xpath.functions.BuiltinFunctionImpl
import io.github.pdvrieze.formats.xpath.functions.argN
import io.github.pdvrieze.formats.xpath.functions.atomicArgOrEmpty
import io.github.pdvrieze.formats.xpath.functions.checkArgCount
import io.github.pdvrieze.formats.xpath.impl.XPathInternal
import io.github.pdvrieze.xml.schematypes.types.DayTimeDurationType
import io.github.pdvrieze.xml.schematypes.types.TimeType
import io.github.pdvrieze.xml.schematypes.values.XsdDayTimeDuration
import io.github.pdvrieze.xml.schematypes.values.XsdString
import io.github.pdvrieze.xml.schematypes.values.XsdTime

@XPathInternal
object DateTimeFunctions : AbstractFunctionObject() {

    fun toMaybeTimeArg(): Unit = TODO()

    val fnTimezoneFromTime = BuiltinFunctionImpl(
        "timezone-from-time",
        functionType(DayTimeDurationType.Instance.opt, TimeType.Instance.opt)
    ) { args ->
        args.checkArgCount(1)
        val time = args.atomicArgOrEmpty<XsdTime>(0) ?: return@BuiltinFunctionImpl XdmSequence.EMPTY
        val offset = time.timezoneOffset ?: return@BuiltinFunctionImpl XdmSequence.EMPTY
        XdmAtomic(XsdDayTimeDuration.ofMinutes(offset))
    }

    val fnStringJoin: BuiltinFunctionImpl<XdmString> = BuiltinFunctionImpl("string-join", contextFunctionTypes(STRING, STRING, ATOMIC.any)) { args ->
        if (args.size > 2) throw EvaluationException(ErrorCodes.FOAP0001_WRONG_ARG_CNT, "String-join takes 1 or 2 arguments")
        val seq = args.argN<XdmSequence<XdmAtomic<*>>>(0)
        val separator = if (args.size == 2) args.argN<XdmString>(1).value else ""
        val join = seq.asSequence().map { Accessors.fnString(it).value.xmlString }.joinToString(separator)
        XdmAtomic(XsdString(join))
    }

}
