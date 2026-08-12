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

package io.github.pdvrieze.formats.xpath.functions.impl.crossplatform

import io.github.pdvrieze.formats.xmlschema.regex.impl.RegexContext
import io.github.pdvrieze.formats.xmlschema.regex.impl.RegexVariant
import io.github.pdvrieze.formats.xmlschema.regex.impl.XRAbstractCharClass
import java.text.Normalizer

actual fun String.normalize(form: NormalizationForm): String {
    if (this.isEmpty()) return this
    val jvmForm = when (form) {
        NormalizationForm.NFC -> Normalizer.Form.NFC
        NormalizationForm.NFD -> Normalizer.Form.NFD
        NormalizationForm.NFKC -> Normalizer.Form.NFKC
        NormalizationForm.NFKD -> Normalizer.Form.NFKD
        NormalizationForm.FULLY_NORMALIZED -> {
            val cp = codePointAt(0)
            when {
                COMPOSING_CLASSES.any { it.contains(cp) } ->
                    return " $this".normalize(NormalizationForm.NFC)
                else -> Normalizer.Form.NFC
            }
        }
    }
    return Normalizer.normalize(this, jvmForm)
}

private val COMPOSING_CLASSES = context(RegexContext(RegexVariant.XPath_2_0)) {
    arrayOf(
        XRAbstractCharClass.getPredefinedClass("Mn", true),
        XRAbstractCharClass.getPredefinedClass("Mc", true),
        XRAbstractCharClass.getPredefinedClass("Me", true),
    )
}

