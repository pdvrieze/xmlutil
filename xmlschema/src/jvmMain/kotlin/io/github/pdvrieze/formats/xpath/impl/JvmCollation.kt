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

package io.github.pdvrieze.formats.xpath.impl

import io.github.pdvrieze.xml.schematypes.Collation
import io.github.pdvrieze.xml.schematypes.values.XsdBase64Binary
import java.net.URI
import java.text.Collator
import java.util.*

class JvmCollation(override val uri: String, private val collator: Collator): Collation {

    override fun key(key: String): XsdBase64Binary {
        return XsdBase64Binary(collator.getCollationKey(key).toByteArray())
    }

    override fun compare(o1: String?, o2: String?): Int {
        return collator.compare(o1, o2)
    }

    companion object {
        fun fromUri(uri: URI): Collation? {
            val query = uri.query.splitToSequence(';', '&')
                .mapNotNull {
                    val i = it.indexOf('=')
                    if (i < 0) null else Pair(it.substring(0, i), it.substring(i + 1))
                }.associate { (k, v) -> k to v }

            val lang = query.getOrElse("lang") { Locale.getDefault().language }
            var locale = Locale.forLanguageTag(lang)

            query["co"]?.let { co ->
                locale = Locale.Builder()
                    .setLocale(locale)
                    .setUnicodeLocaleKeyword("co", co)
                    .build()
            }

            val collator = Collator.getInstance(locale)

            when (query["strength"]?.lowercase()) {
                "primary" -> collator.strength = Collator.PRIMARY
                "secondary" -> collator.strength = Collator.SECONDARY
                "tertiary" -> collator.strength = Collator.TERTIARY
                "identical" -> collator.strength = Collator.IDENTICAL
            }

            /** TODO: caseFirst Not supported for  now
            query["caseFirst"]?.let { caseFirst ->

            }
            */

            return JvmCollation(uri.toString(),collator)
        }

    }
}
