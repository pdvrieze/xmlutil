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

package io.github.pdvrieze.xml.schematypes.values.test

import io.github.pdvrieze.xml.schematypes.values.XsdDateTime
import io.github.pdvrieze.xml.schematypes.values.XsdDateTimeStamp
import kotlinx.datetime.UtcOffset
import kotlinx.datetime.asTimeZone
import kotlinx.datetime.toLocalDateTime
import kotlin.test.Test
import kotlin.test.assertEquals

class TestDateTime {
    @Test
    fun testDateTimeToInstant() {
        val time1 = XsdDateTime("2021-01-01T12:13:42.123456789+01:00")
        val instant = time1.instant()
        val localTime = instant.toLocalDateTime(UtcOffset(1).asTimeZone())
        assertEquals(12, localTime.hour)
        assertEquals(13, localTime.minute)
        assertEquals(42, localTime.second)
        assertEquals(123456789, localTime.nanosecond)
    }
    @Test
    fun testDateTime() {
        val time1 = XsdDateTime("2021-01-01T12:00:00.123456789+01:00")
        val time2 = XsdDateTimeStamp("2021-01-01T12:00:00.123456789+01:00")

        val inst1 = time1.instant()
        val inst2 = time2.instant()

        assertEquals(inst1, inst2)

        assertEquals(time1, time2)
    }

}
