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

import io.github.pdvrieze.formats.xpath.eval.data.XdmSequence
import io.github.pdvrieze.formats.xpath.eval.typeTest.XdmSchemaTypeTest
import io.github.pdvrieze.formats.xpath.functions.BuiltinFunctionImpl
import io.github.pdvrieze.formats.xpath.impl.SequenceType.OccurrenceType.OPTIONAL
import io.github.pdvrieze.formats.xpath.impl.XPathInternal
import io.github.pdvrieze.xml.schematypes.types.DurationType
import io.github.pdvrieze.xml.schematypes.values.XsdDuration

@OptIn(XPathInternal::class)
object DurationFunctions : AbstractFunctionObject(){

    //region 8.3 Component extraction
    val fnYearsFromDuration = BuiltinFunctionImpl.Fn(
        "years-from-duration",
        INTEGER.single,
        XdmSchemaTypeTest(DurationType.Instance, OPTIONAL)
    ) { args ->
        val duration = args.atomicOrEmpty<XsdDuration>(0) ?: return@Fn XdmSequence.EMPTY
        atomic(duration.months / 12)
    }

    val fnMonthsFromDuration = BuiltinFunctionImpl.Fn(
        "months-from-duration",
        INTEGER.single,
        XdmSchemaTypeTest(DurationType.Instance, OPTIONAL)
    ) { args ->
        val duration = args.atomicOrEmpty<XsdDuration>(0) ?: return@Fn XdmSequence.EMPTY
        atomic(duration.months % 12)
    }

    val fnDaysFromDuration = BuiltinFunctionImpl.Fn(
        "days-from-duration",
        INTEGER.single,
        XdmSchemaTypeTest(DurationType.Instance, OPTIONAL)
    ) { args ->
        val duration = args.atomicOrEmpty<XsdDuration>(0) ?: return@Fn XdmSequence.EMPTY
        atomic(duration.millis / 86_400_000L)
    }

    val fnHoursFromDuration = BuiltinFunctionImpl.Fn(
        "hours-from-duration",
        INTEGER.single,
        XdmSchemaTypeTest(DurationType.Instance, OPTIONAL)
    ) { args ->
        val duration = args.atomicOrEmpty<XsdDuration>(0) ?: return@Fn XdmSequence.EMPTY
        atomic((duration.millis % 86_400_000) / 3_600_000)
    }

    val fnMinutesFromDuration = BuiltinFunctionImpl.Fn(
        "minutes-from-duration",
        INTEGER.single,
        XdmSchemaTypeTest(DurationType.Instance, OPTIONAL)
    ) { args ->
        val duration = args.atomicOrEmpty<XsdDuration>(0) ?: return@Fn XdmSequence.EMPTY
        atomic((duration.millis % 3_600_000) / 60_000)
    }

    val fnSecondsFromDuration = BuiltinFunctionImpl.Fn(
        "seconds-from-duration",
        INTEGER.single,
        XdmSchemaTypeTest(DurationType.Instance, OPTIONAL)
    ) { args ->
        val duration = args.atomicOrEmpty<XsdDuration>(0) ?: return@Fn XdmSequence.EMPTY
        atomic((duration.millis % 60_000) / 1000)
    }

    //endregion
}
