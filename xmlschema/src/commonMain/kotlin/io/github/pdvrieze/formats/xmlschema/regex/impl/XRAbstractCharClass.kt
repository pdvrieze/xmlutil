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

@file:Suppress("DEPRECATION") // Char.toInt()
package io.github.pdvrieze.formats.xmlschema.regex.impl

import io.github.pdvrieze.formats.xmlschema.regex.impl.sets.XBitSet
import io.github.pdvrieze.formats.xmlschema.regex.impl.sets.set
import io.github.pdvrieze.formats.xmlschema.resolved.ResolvedSchema
import io.github.pdvrieze.formats.xmlschema.resolved.ResolvedSchema.Version
import nl.adaptivity.xmlutil.XmlUtilInternal
import kotlin.collections.associate

/**
 * Unicode category (i.e. Ll, Lu).
 */
internal open class XRUnicodeCategory(protected val category: Int) : XRAbstractCharClass() {
    override fun contains(ch: Int): Boolean = alt xor (ch.toChar().category.ordinal == category)
}

/**
 * Unicode category scope (i.e IsL, IsM, ...)
 */
internal class XRUnicodeCategoryScope(category: Int) : XRUnicodeCategory(category) {
    override fun contains(ch: Int): Boolean {
        return alt xor (((category shr ch.toChar().category.ordinal) and 1) != 0)
    }
}

/**
 * This class represents character classes, i.e. sets of character either predefined or user defined.
 * Note: this class represent a token, not node, so being constructed by lexer.
 */
@OptIn(XmlUtilInternal::class)
internal abstract class XRAbstractCharClass : XRSpecialToken() {
    /**
     * Show if the class has alternative meaning:
     * if the class contains character 'a' and alt == true then the class will contains all characters except 'a'.
     */
    internal var alt: Boolean = false
    internal var altSurrogates: Boolean = false

    /**
     * For each unpaired surrogate char indicates whether it is contained in this char class.
     */
    internal val lowHighSurrogates = XBitSet(SURROGATE_CARDINALITY) // Bit set for surrogates?

    /**
     * Indicates if this class may contain supplementary Unicode codepoints.
     * If this flag is specified it doesn't mean that this class contains supplementary characters but may contain.
     */
    var mayContainSupplCodepoints = false
        protected set

    /** Returns true if this char class contains character specified. */
    abstract operator fun contains(ch: Int): Boolean
    open fun contains(ch: Char): Boolean = contains(ch.toInt())

    /**
     * Returns BitSet representing this character class or `null`
     * if this character class does not have character representation;
     */
    open internal val bits: XBitSet?
        get() = null

    fun hasLowHighSurrogates(): Boolean {
        return if (altSurrogates)
            lowHighSurrogates.nextClearBit(0) != -1
        else
            lowHighSurrogates.nextSetBit(0) != -1
    }

    override val type: Type = Type.CHARCLASS

    open val instance: XRAbstractCharClass
        get() = this


    private val surrogates_: XRAbstractCharClass by lazy {
        val surrogates = lowHighSurrogates
        val result = object : XRAbstractCharClass() {
            override fun contains(ch: Int): Boolean {
                val index = ch - Char.MIN_SURROGATE.toInt()

                return if (index >= 0 && index < XRAbstractCharClass.SURROGATE_CARDINALITY) {
                    this.altSurrogates xor surrogates[index]
                } else {
                    false
                }
            }
        }
        result.alt = this.alt
        result.altSurrogates = this.altSurrogates
        result.mayContainSupplCodepoints = this.mayContainSupplCodepoints
        result
    }
    /**
     * Returns a char class that contains only unpaired surrogate chars from this char class.
     *
     * Consider the following char class: `[a\uD801\uDC00\uD800]`.
     * This function returns a char class that contains only `\uD800`: `[\uD800]`.
     * [classWithoutSurrogates] returns a char class that does not contain `\uD800`: `[a\uD801\uDC00]`.
     *
     * The returned char class is used to create [XRSurrogateRangeSet] node
     * that matches any unpaired surrogate from this char class. [XRSurrogateRangeSet]
     * doesn't match a surrogate that is paired with the char before or after it.
     * The result of [classWithoutSurrogates] is used to create [XRSupplementaryRangeSet]
     * or [XRRangeSet] depending on [mayContainSupplCodepoints].
     * The two nodes are then combined in [XRCompositeRangeSet] node to fully represent this char class.
     */
    fun classWithSurrogates(): XRAbstractCharClass {
        return surrogates_
    }


    /**
     * Returns a char class that contains all chars from this char class excluding the unpaired surrogate chars.
     *
     * See [classWithSurrogates] for details.
     */
    // We cannot cache this class as we've done with surrogates above because
    // here is a circular reference between it and AbstractCharClass.
    fun classWithoutSurrogates(): XRAbstractCharClass {
        val result = object : XRAbstractCharClass() {
            override fun contains(ch: Int): Boolean {
                val index = ch - Char.MIN_SURROGATE.toInt()

                val containslHS = if (index >= 0 && index < XRAbstractCharClass.SURROGATE_CARDINALITY)
                    this.altSurrogates xor this@XRAbstractCharClass.lowHighSurrogates.get(index)
                else
                    false

                return this@XRAbstractCharClass.contains(ch) && !containslHS
            }
        }
        result.alt = this.alt
        result.altSurrogates = this.altSurrogates
        result.mayContainSupplCodepoints = this.mayContainSupplCodepoints
        return result
    }

    /**
     * Sets this CharClass to negative form, i.e. if they will add some characters and after that set this
     * class to negative it will accept all the characters except previously set ones.
     *
     * Although this method will not alternate all the already set characters,
     * just overall meaning of the class.
     */
    fun setNegative(value: Boolean): XRAbstractCharClass {
        if (alt xor value) {
            alt = !alt
            altSurrogates = !altSurrogates

            if (!mayContainSupplCodepoints) {
                mayContainSupplCodepoints = true
            }
        }
        return this
    }

    fun isNegative(): Boolean {
        return alt
    }

    internal abstract class CachedCharClass {
        lateinit private var posValue: XRAbstractCharClass

        lateinit private var negValue: XRAbstractCharClass

        // Somewhat ugly init sequence, as computeValue() may depend on fields, initialized in subclass ctor.
        protected fun initValues() {
            posValue = computeValue()
            negValue = computeValue().setNegative(true)
        }

        fun getValue(negative: Boolean): XRAbstractCharClass = if (!negative) posValue else negValue
        protected abstract fun computeValue(): XRAbstractCharClass
    }

    internal class CachedDigit : CachedCharClass() {
        init {
            initValues()
        }
        override fun computeValue(): XRAbstractCharClass = XRCharClass().add('0', '9')
    }

    internal class CachedNonDigit : CachedCharClass() {
        init {
            initValues()
        }
        override fun computeValue(): XRAbstractCharClass =
                XRCharClass().add('0', '9').setNegative(true).apply { mayContainSupplCodepoints = true }
    }

    internal class CachedSpace : CachedCharClass() {
        init {
            initValues()
        }
        /* 9-13 - \t\n\x0B\f\r; 32 - ' ' */
        override fun computeValue(): XRAbstractCharClass = XRCharClass().add(9, 13).add(32)
    }

    internal class CachedNonSpace : CachedCharClass() {
        init {
            initValues()
        }
        override fun computeValue(): XRAbstractCharClass =
                CachedSpace().getValue(negative = true).apply { mayContainSupplCodepoints = true }
    }

    internal class CachedWord : CachedCharClass() {
        init {
            initValues()
        }
        override fun computeValue(): XRAbstractCharClass = XRCharClass().add('a', 'z').add('A', 'Z').add('0', '9').add('_')
    }

    internal class CachedNonWord : CachedCharClass() {
        init {
            initValues()
        }
        override fun computeValue(): XRAbstractCharClass =
                CachedWord().getValue(negative = true).apply { mayContainSupplCodepoints = true }
    }

    internal class CachedNameStartChar : CachedCharClass() {
        init {
            initValues()
        }
        public override fun computeValue(): XRAbstractCharClass = XRCharClass()
            .add(':')
            .add('A', 'Z')
            .add('_')
            .add('a', 'z')
            .add(0xC0, 0xD6)
            .add(0xD8, 0xF6)
            .add(0xF8, 0x2FF)
            .add(0x370, 0x37D)
            .add(0x37F, 0x1FFF)
            .add(0x200C, 0x200D)
            .add(0x2070, 0x218F)
            .add(0x2C00, 0x2FEF)
            .add(0x3001, 0xD7FF)
            .add(0xF900, 0xFDCF)
            .add(0xFDF0, 0xFFFD)
            .add(0x10000, 0xEFFFF)
    }

    internal class CachedNonNameStartChar : CachedCharClass() {
        init {
            initValues()
        }
        override fun computeValue(): XRAbstractCharClass =
            CachedNameStartChar().getValue(negative = true).apply { mayContainSupplCodepoints = true }
    }

    internal class CachedNameChar : CachedCharClass() {
        init {
            initValues()
        }
        override fun computeValue(): XRAbstractCharClass {
            return (CachedNameStartChar().computeValue() as XRCharClass)
                .add('-')
                .add('.')
                .add('0', '9')
                .add(0xB7)
                .add(0x300,0x36f)
                .add(0x203f,0x2040)
        }
    }

    internal class CachedNonNameChar : CachedCharClass() {
        init {
            initValues()
        }
        override fun computeValue(): XRAbstractCharClass =
                CachedWord().getValue(negative = true).apply { mayContainSupplCodepoints = true }
    }

    /** Supports unknown classes, in xsd 1.1 those are allowed, but match any character at all. */
    internal class CachedUnknownClass: CachedCharClass() {
        init {
            initValues()
        }

        override fun computeValue(): XRAbstractCharClass {
            return XRCharClass().apply { inverted=true }
        }
    }

    internal class CachedLower : CachedCharClass() {
        init {
            initValues()
        }
        override fun computeValue(): XRAbstractCharClass = XRCharClass().add('a', 'z')
    }

    internal class CachedUpper : CachedCharClass() {
        init {
            initValues()
        }
        override fun computeValue(): XRAbstractCharClass = XRCharClass().add('A', 'Z')
    }

    internal class CachedASCII : CachedCharClass() {
        init {
            initValues()
        }
        override fun computeValue(): XRAbstractCharClass = XRCharClass().add(0x00, 0x7F)
    }

    internal class CachedAlpha : CachedCharClass() {
        init {
            initValues()
        }
        override fun computeValue(): XRAbstractCharClass = XRCharClass().add('a', 'z').add('A', 'Z')
    }

    internal class CachedAlnum : CachedCharClass() {
        init {
            initValues()
        }
        override fun computeValue(): XRAbstractCharClass =
                (CachedAlpha().getValue(negative = false) as XRCharClass).add('0', '9')
    }

    internal class CachedPunct : CachedCharClass() {
        init {
            initValues()
        }
        /* Punctuation !"#$%&'()*+,-./:;<=>?@ [\]^_` {|}~ */
        override fun computeValue(): XRAbstractCharClass = XRCharClass().add(0x21, 0x40).add(0x5B, 0x60).add(0x7B, 0x7E)
    }

    internal class CachedGraph : CachedCharClass() {
        init {
            initValues()
        }
        /* plus punctuation */
        override fun computeValue(): XRAbstractCharClass =
                (CachedAlnum().getValue(negative = false) as XRCharClass)
                        .add(0x21, 0x40)
                        .add(0x5B, 0x60)
                        .add(0x7B, 0x7E)
    }

    internal class CachedPrint : CachedCharClass() {
        init {
            initValues()
        }
        override fun computeValue(): XRAbstractCharClass =
                (CachedGraph().getValue(negative = true) as XRCharClass).add(0x20)
    }

    internal class CachedBlank : CachedCharClass() {
        init {
            initValues()
        }
        override fun computeValue(): XRAbstractCharClass = XRCharClass().add(' ').add('\t')
    }

    internal class CachedCntrl : CachedCharClass() {
        init {
            initValues()
        }
        override fun computeValue(): XRAbstractCharClass = XRCharClass().add(0x00, 0x1F).add(0x7F)
    }

    internal class CachedXDigit : CachedCharClass() {
        init {
            initValues()
        }
        override fun computeValue(): XRAbstractCharClass = XRCharClass().add('0', '9').add('a', 'f').add('A', 'F')
    }

    // From Java 8+ `Pattern` doc: \v - A vertical whitespace character: [\n\x0B\f\r\x85\u2028\u2029]
    internal class CachedVerticalWhitespace : CachedCharClass() {
        init {
            initValues()
        }
        override fun computeValue(): XRAbstractCharClass =
            XRCharClass().addAll(listOf('\n', '\u000B', '\u000C' /* aka \f */, '\r', '\u0085', '\u2028', '\u2029'))
    }

    // From Java 8+ `Pattern` doc: \V - A non-vertical whitespace character: [^\v]
    internal class CachedNonVerticalWhitespace: CachedCharClass() {
        init {
            initValues()
        }
        override fun computeValue(): XRAbstractCharClass =
            CachedVerticalWhitespace().getValue(negative = true).apply { mayContainSupplCodepoints = true }
        // TODO: Does it match a pair of surrogates? Do we really need mayContainSupplCodepoints?
    }

    // From Java 8+ `Pattern` doc:
    // \h - A horizontal whitespace character: [ \t\xA0\u1680\u180e\u2000-\u200a\u202f\u205f\u3000]
    internal class CachedHorizontalWhitespace: CachedCharClass() {
        init {
            initValues()
        }
        override fun computeValue(): XRAbstractCharClass =
            XRCharClass().addAll(listOf(' ', '\t', '\u00A0', '\u1680', '\u180e', '\u202f', '\u205f', '\u3000'))
                .add('\u2000', '\u200a')
    }

    // From Java 8+ `Pattern` doc:
    // \H - A non-horizontal whitespace character: [^\h]
    internal class CachedNonHorizontalWhitespace: CachedCharClass() {
        init {
            initValues()
        }
        override fun computeValue(): XRAbstractCharClass =
            CachedHorizontalWhitespace().getValue(negative = true).apply { mayContainSupplCodepoints = true }
    }

    internal class CachedRange(var start: Int, var end: Int) : CachedCharClass() {
        init {
            initValues()
        }

        override fun computeValue(): XRAbstractCharClass =
                object: XRAbstractCharClass() {
                    override fun contains(ch: Int): Boolean = alt xor (ch in start..end)
                }.apply {
                    if (end >= MIN_SUPPLEMENTARY_CODE_POINT) {
                        mayContainSupplCodepoints = true
                    }
                    val minSurrogate = Char.MIN_SURROGATE.toInt()
                    val maxSurrogate = Char.MAX_SURROGATE.toInt()
                    // There is an intersection with surrogate characters.
                    if (end >= minSurrogate && start <= maxSurrogate && start <= end) {
                        val surrogatesStart = maxOf(start, minSurrogate) - minSurrogate
                        val surrogatesEnd = minOf(end, maxSurrogate) - minSurrogate
                        lowHighSurrogates.set(surrogatesStart..surrogatesEnd)
                    }
                }
    }

    internal class CachedSpecialsBlock : CachedCharClass() {
        init {
            initValues()
        }
        public override fun computeValue(): XRAbstractCharClass = XRCharClass().add(0xFEFF, 0xFEFF).add(0xFFF0, 0xFFFD)
    }

    internal class CachedCategoryScope(
            val category: Int,
            val mayContainSupplCodepoints: Boolean,
            val containsAllSurrogates: Boolean = false) : CachedCharClass() {
        init {
            initValues()
        }
        override fun computeValue(): XRAbstractCharClass {
            val result = XRUnicodeCategoryScope(category)
            if (containsAllSurrogates) {
                result.lowHighSurrogates.set(0, SURROGATE_CARDINALITY, true)
            }

            result.mayContainSupplCodepoints = mayContainSupplCodepoints
            return result
        }
    }

    internal class CachedCategory(
            val category: Int,
            val mayContainSupplCodepoints: Boolean,
            val containsAllSurrogates: Boolean = false) : CachedCharClass() {
        init {
            initValues()
        }
        override fun computeValue(): XRAbstractCharClass {
            val result = XRUnicodeCategory(category)
            if (containsAllSurrogates) {
                result.lowHighSurrogates.set(0, SURROGATE_CARDINALITY, true)
            }
            result.mayContainSupplCodepoints = mayContainSupplCodepoints
            return result
        }
    }

    companion object {
        //Char.MAX_SURROGATE - Char.MIN_SURROGATE + 1
        const val SURROGATE_CARDINALITY = 2048

        /**
         * Character classes.
         * See http://www.unicode.org/reports/tr18/, http://www.unicode.org/Public/4.1.0/ucd/Blocks.txt
         */
        enum class CharClasses(val regexName : String, val factory: () -> CachedCharClass) {
            LOWER("Lower", ::CachedLower),
            UPPER("Upper", ::CachedUpper),
            ASCII("ASCII", ::CachedASCII),
            ALPHA("Alpha", ::CachedAlpha),
            DIGIT("Digit", ::CachedDigit),
            ALNUM("Alnum", :: CachedAlnum),
            PUNCT("Punct", ::CachedPunct),
            GRAPH("Graph", ::CachedGraph),
            PRINT("Print", ::CachedPrint),
            BLANK("Blank", ::CachedBlank),
            CNTRL("Cntrl", ::CachedCntrl),
            XDIGIT("XDigit", ::CachedXDigit),
            SPACE("Space", ::CachedSpace),
            NAMECHAR("c", ::CachedNameChar),
            NON_NAMECHAR("C", ::CachedNonNameChar),
            NAMESTARTCHAR("i", ::CachedNameStartChar),
            NON_NAMESTARTCHAR("I", ::CachedNonNameStartChar),
            WORD("w", ::CachedWord),
            NON_WORD("W", ::CachedNonWord),
            SPACE_SHORT("s", ::CachedSpace),
            NON_SPACE("S", ::CachedNonSpace),
            DIGIT_SHORT("d", ::CachedDigit),
            NON_DIGIT("D", ::CachedNonDigit),
            VERTICAL_WHITESPACE("v", ::CachedVerticalWhitespace),
            NON_VERTICAL_WHITESPACE("V", ::CachedNonVerticalWhitespace),
            HORIZONTAL_WHITESPACE("h", ::CachedHorizontalWhitespace),
            NON_HORIZONTAL_WHITESPACE("H", ::CachedNonHorizontalWhitespace),
            BASIC_LATIN("BasicLatin", { CachedRange(0x0000, 0x007F) }),
            LATIN1_SUPPLEMENT("Latin-1Supplement", { CachedRange(0x0080, 0x00FF) }),
            LATIN_EXTENDED_A("LatinExtended-A", { CachedRange(0x0100, 0x017F) }),
            LATIN_EXTENDED_B("LatinExtended-B", { CachedRange(0x0180, 0x024F) }),
            IPA_EXTENSIONS("IPAExtensions", { CachedRange(0x0250, 0x02AF) }),
            SPACING_MODIFIER_LETTERS("SpacingModifierLetters", { CachedRange(0x02B0, 0x02FF) }),
            COMBINING_DIACRITICAL_MARKS("CombiningDiacriticalMarks", { CachedRange(0x0300, 0x036F) }),
            GREEK("Greek", { CachedRange(0x0370, 0x03FF) }),
            CYRILLIC("Cyrillic", { CachedRange(0x0400, 0x04FF) }),
            CYRILLIC_SUPPLEMENT("CyrillicSupplement", { CachedRange(0x0500, 0x052F) }),
            ARMENIAN("Armenian", { CachedRange(0x0530, 0x058F) }),
            HEBREW("Hebrew", { CachedRange(0x0590, 0x05FF) }),
            ARABIC("Arabic", { CachedRange(0x0600, 0x06FF) }),
            SYRIAC("Syriac", { CachedRange(0x0700, 0x074F) }),
            ARABICSUPPLEMENT("ArabicSupplement", { CachedRange(0x0750, 0x077F) }),
            THAANA("Thaana", { CachedRange(0x0780, 0x07BF) }),
            DEVANAGARI("Devanagari", { CachedRange(0x0900, 0x097F) }),
            BENGALI("Bengali", { CachedRange(0x0980, 0x09FF) }),
            GURMUKHI("Gurmukhi", { CachedRange(0x0A00, 0x0A7F) }),
            GUJARATI("Gujarati", { CachedRange(0x0A80, 0x0AFF) }),
            ORIYA("Oriya", { CachedRange(0x0B00, 0x0B7F) }),
            TAMIL("Tamil", { CachedRange(0x0B80, 0x0BFF) }),
            TELUGU("Telugu", { CachedRange(0x0C00, 0x0C7F) }),
            KANNADA("Kannada", { CachedRange(0x0C80, 0x0CFF) }),
            MALAYALAM("Malayalam", { CachedRange(0x0D00, 0x0D7F) }),
            SINHALA("Sinhala", { CachedRange(0x0D80, 0x0DFF) }),
            THAI("Thai", { CachedRange(0x0E00, 0x0E7F) }),
            LAO("Lao", { CachedRange(0x0E80, 0x0EFF) }),
            TIBETAN("Tibetan", { CachedRange(0x0F00, 0x0FFF) }),
            MYANMAR("Myanmar", { CachedRange(0x1000, 0x109F) }),
            GEORGIAN("Georgian", { CachedRange(0x10A0, 0x10FF) }),
            HANGULJAMO("HangulJamo", { CachedRange(0x1100, 0x11FF) }),
            ETHIOPIC("Ethiopic", { CachedRange(0x1200, 0x137F) }),
            ETHIOPICSUPPLEMENT("EthiopicSupplement", { CachedRange(0x1380, 0x139F) }),
            CHEROKEE("Cherokee", { CachedRange(0x13A0, 0x13FF) }),
            UNIFIEDCANADIANABORIGINALSYLLABICS("UnifiedCanadianAboriginalSyllabics", { CachedRange(0x1400, 0x167F) }),
            OGHAM("Ogham", { CachedRange(0x1680, 0x169F) }),
            RUNIC("Runic", { CachedRange(0x16A0, 0x16FF) }),
            TAGALOG("Tagalog", { CachedRange(0x1700, 0x171F) }),
            HANUNOO("Hanunoo", { CachedRange(0x1720, 0x173F) }),
            BUHID("Buhid", { CachedRange(0x1740, 0x175F) }),
            TAGBANWA("Tagbanwa", { CachedRange(0x1760, 0x177F) }),
            KHMER("Khmer", { CachedRange(0x1780, 0x17FF) }),
            MONGOLIAN("Mongolian", { CachedRange(0x1800, 0x18AF) }),
            LIMBU("Limbu", { CachedRange(0x1900, 0x194F) }),
            TAILE("TaiLe", { CachedRange(0x1950, 0x197F) }),
            NEWTAILUE("NewTaiLue", { CachedRange(0x1980, 0x19DF) }),
            KHMERSYMBOLS("KhmerSymbols", { CachedRange(0x19E0, 0x19FF) }),
            BUGINESE("Buginese", { CachedRange(0x1A00, 0x1A1F) }),
            PHONETICEXTENSIONS("PhoneticExtensions", { CachedRange(0x1D00, 0x1D7F) }),
            PHONETICEXTENSIONSSUPPLEMENT("PhoneticExtensionsSupplement", { CachedRange(0x1D80, 0x1DBF) }),
            COMBININGDIACRITICALMARKSSUPPLEMENT("CombiningDiacriticalMarksSupplement", { CachedRange(0x1DC0, 0x1DFF) }),
            LATINEXTENDEDADDITIONAL("LatinExtendedAdditional", { CachedRange(0x1E00, 0x1EFF) }),
            GREEKEXTENDED("GreekExtended", { CachedRange(0x1F00, 0x1FFF) }),
            GENERALPUNCTUATION("GeneralPunctuation", { CachedRange(0x2000, 0x206F) }),
            SUPERSCRIPTSANDSUBSCRIPTS("SuperscriptsandSubscripts", { CachedRange(0x2070, 0x209F) }),
            CURRENCYSYMBOLS("CurrencySymbols", { CachedRange(0x20A0, 0x20CF) }),
            COMBININGMARKSFORSYMBOLS("CombiningMarksforSymbols", { CachedRange(0x20D0, 0x20FF) }),
            LETTERLIKESYMBOLS("LetterlikeSymbols", { CachedRange(0x2100, 0x214F) }),
            NUMBERFORMS("NumberForms", { CachedRange(0x2150, 0x218F) }),
            ARROWS("Arrows", { CachedRange(0x2190, 0x21FF) }),
            MATHEMATICALOPERATORS("MathematicalOperators", { CachedRange(0x2200, 0x22FF) }),
            MISCELLANEOUSTECHNICAL("MiscellaneousTechnical", { CachedRange(0x2300, 0x23FF) }),
            CONTROLPICTURES("ControlPictures", { CachedRange(0x2400, 0x243F) }),
            OPTICALCHARACTERRECOGNITION("OpticalCharacterRecognition", { CachedRange(0x2440, 0x245F) }),
            ENCLOSEDALPHANUMERICS("EnclosedAlphanumerics", { CachedRange(0x2460, 0x24FF) }),
            BOXDRAWING("BoxDrawing", { CachedRange(0x2500, 0x257F) }),
            BLOCKELEMENTS("BlockElements", { CachedRange(0x2580, 0x259F) }),
            GEOMETRICSHAPES("GeometricShapes", { CachedRange(0x25A0, 0x25FF) }),
            MISCELLANEOUSSYMBOLS("MiscellaneousSymbols", { CachedRange(0x2600, 0x26FF) }),
            DINGBATS("Dingbats", { CachedRange(0x2700, 0x27BF) }),
            MISCELLANEOUSMATHEMATICALSYMBOLS_A("MiscellaneousMathematicalSymbols-A", { CachedRange(0x27C0, 0x27EF) }),
            SUPPLEMENTALARROWS_A("SupplementalArrows-A", { CachedRange(0x27F0, 0x27FF) }),
            BRAILLEPATTERNS("BraillePatterns", { CachedRange(0x2800, 0x28FF) }),
            SUPPLEMENTALARROWS_B("SupplementalArrows-B", { CachedRange(0x2900, 0x297F) }),
            MISCELLANEOUSMATHEMATICALSYMBOLS_B("MiscellaneousMathematicalSymbols-B", { CachedRange(0x2980, 0x29FF) }),
            SUPPLEMENTALMATHEMATICALOPERATORS("SupplementalMathematicalOperators", { CachedRange(0x2A00, 0x2AFF) }),
            MISCELLANEOUSSYMBOLSANDARROWS("MiscellaneousSymbolsandArrows", { CachedRange(0x2B00, 0x2BFF) }),
            GLAGOLITIC("Glagolitic", { CachedRange(0x2C00, 0x2C5F) }),
            COPTIC("Coptic", { CachedRange(0x2C80, 0x2CFF) }),
            GEORGIANSUPPLEMENT("GeorgianSupplement", { CachedRange(0x2D00, 0x2D2F) }),
            TIFINAGH("Tifinagh", { CachedRange(0x2D30, 0x2D7F) }),
            ETHIOPICEXTENDED("EthiopicExtended", { CachedRange(0x2D80, 0x2DDF) }),
            SUPPLEMENTALPUNCTUATION("SupplementalPunctuation", { CachedRange(0x2E00, 0x2E7F) }),
            CJKRADICALSSUPPLEMENT("CJKRadicalsSupplement", { CachedRange(0x2E80, 0x2EFF) }),
            KANGXIRADICALS("KangxiRadicals", { CachedRange(0x2F00, 0x2FDF) }),
            IDEOGRAPHICDESCRIPTIONCHARACTERS("IdeographicDescriptionCharacters", { CachedRange(0x2FF0, 0x2FFF) }),
            CJKSYMBOLSANDPUNCTUATION("CJKSymbolsandPunctuation", { CachedRange(0x3000, 0x303F) }),
            HIRAGANA("Hiragana", { CachedRange(0x3040, 0x309F) }),
            KATAKANA("Katakana", { CachedRange(0x30A0, 0x30FF) }),
            BOPOMOFO("Bopomofo", { CachedRange(0x3100, 0x312F) }),
            HANGULCOMPATIBILITYJAMO("HangulCompatibilityJamo", { CachedRange(0x3130, 0x318F) }),
            KANBUN("Kanbun", { CachedRange(0x3190, 0x319F) }),
            BOPOMOFOEXTENDED("BopomofoExtended", { CachedRange(0x31A0, 0x31BF) }),
            CJKSTROKES("CJKStrokes", { CachedRange(0x31C0, 0x31EF) }),
            KATAKANAPHONETICEXTENSIONS("KatakanaPhoneticExtensions", { CachedRange(0x31F0, 0x31FF) }),
            ENCLOSEDCJKLETTERSANDMONTHS("EnclosedCJKLettersandMonths", { CachedRange(0x3200, 0x32FF) }),
            CJKCOMPATIBILITY("CJKCompatibility", { CachedRange(0x3300, 0x33FF) }),
            CJKUNIFIEDIDEOGRAPHSEXTENSIONA("CJKUnifiedIdeographsExtensionA", { CachedRange(0x3400, 0x4DB5) }),
            YIJINGHEXAGRAMSYMBOLS("YijingHexagramSymbols", { CachedRange(0x4DC0, 0x4DFF) }),
            CJKUNIFIEDIDEOGRAPHS("CJKUnifiedIdeographs", { CachedRange(0x4E00, 0x9FFF) }),
            YISYLLABLES("YiSyllables", { CachedRange(0xA000, 0xA48F) }),
            YIRADICALS("YiRadicals", { CachedRange(0xA490, 0xA4CF) }),
            MODIFIERTONELETTERS("ModifierToneLetters", { CachedRange(0xA700, 0xA71F) }),
            SYLOTINAGRI("SylotiNagri", { CachedRange(0xA800, 0xA82F) }),
            HANGULSYLLABLES("HangulSyllables", { CachedRange(0xAC00, 0xD7A3) }),
            HIGHSURROGATES("HighSurrogates", { CachedRange(0xD800, 0xDB7F) }),
            HIGHPRIVATEUSESURROGATES("HighPrivateUseSurrogates", { CachedRange(0xDB80, 0xDBFF) }),
            LOWSURROGATES("LowSurrogates", { CachedRange(0xDC00, 0xDFFF) }),
            PRIVATEUSEAREA("PrivateUseArea", { CachedRange(0xE000, 0xF8FF) }),
            CJKCOMPATIBILITYIDEOGRAPHS("CJKCompatibilityIdeographs", { CachedRange(0xF900, 0xFAFF) }),
            ALPHABETICPRESENTATIONFORMS("AlphabeticPresentationForms", { CachedRange(0xFB00, 0xFB4F) }),
            ARABICPRESENTATIONFORMS_A("ArabicPresentationForms-A", { CachedRange(0xFB50, 0xFDFF) }),
            VARIATIONSELECTORS("VariationSelectors", { CachedRange(0xFE00, 0xFE0F) }),
            VERTICALFORMS("VerticalForms", { CachedRange(0xFE10, 0xFE1F) }),
            COMBININGHALFMARKS("CombiningHalfMarks", { CachedRange(0xFE20, 0xFE2F) }),
            CJKCOMPATIBILITYFORMS("CJKCompatibilityForms", { CachedRange(0xFE30, 0xFE4F) }),
            SMALLFORMVARIANTS("SmallFormVariants", { CachedRange(0xFE50, 0xFE6F) }),
            ARABICPRESENTATIONFORMS_B("ArabicPresentationForms-B", { CachedRange(0xFE70, 0xFEFF) }),
            HALFWIDTHANDFULLWIDTHFORMS("HalfwidthandFullwidthForms", { CachedRange(0xFF00, 0xFFEF) }),
            ALL("all", { CachedRange(0x00, 0x10FFFF) }),
            SPECIALS("Specials", ::CachedSpecialsBlock),
            CN("Cn", { CachedCategory(CharCategory.UNASSIGNED.ordinal, true) }),
            ISL("IsL", { CachedCategoryScope(0x3E, true) }),
            LU("Lu", { CachedCategory(CharCategory.UPPERCASE_LETTER.ordinal, true) }),
            LL("Ll", { CachedCategory(CharCategory.LOWERCASE_LETTER.ordinal, true) }),
            LT("Lt", { CachedCategory(CharCategory.TITLECASE_LETTER.ordinal, false) }),
            LM("Lm", { CachedCategory(CharCategory.MODIFIER_LETTER.ordinal, false) }),
            LO("Lo", { CachedCategory(CharCategory.OTHER_LETTER.ordinal, true) }),
            ISM("IsM", { CachedCategoryScope(0x1C0, true) }),
            MN("Mn", { CachedCategory(CharCategory.NON_SPACING_MARK.ordinal, true) }),
            ME("Me", { CachedCategory(CharCategory.ENCLOSING_MARK.ordinal, false) }),
            MC("Mc", { CachedCategory(CharCategory.COMBINING_SPACING_MARK.ordinal, true) }),
            N("IsN", { CachedCategoryScope(0xE00, true) }),
            ND("Nd", { CachedCategory(CharCategory.DECIMAL_DIGIT_NUMBER.ordinal, true) }),
            NL("Nl", { CachedCategory(CharCategory.LETTER_NUMBER.ordinal, true) }),
            NO("No", { CachedCategory(CharCategory.OTHER_NUMBER.ordinal, true) }),
            ISZ("IsZ", { CachedCategoryScope(0x7000, false) }),
            ZS("Zs", { CachedCategory(CharCategory.SPACE_SEPARATOR.ordinal, false) }),
            ZL("Zl", { CachedCategory(CharCategory.LINE_SEPARATOR.ordinal, false) }),
            ZP("Zp", { CachedCategory(CharCategory.PARAGRAPH_SEPARATOR.ordinal, false) }),
            ISC("IsC", { CachedCategoryScope(0xF0000, true, true) }),
            CC("Cc", { CachedCategory(CharCategory.CONTROL.ordinal, false) }),
            CF("Cf", { CachedCategory(CharCategory.FORMAT.ordinal, true) }),
            CO("Co", { CachedCategory(CharCategory.PRIVATE_USE.ordinal, true) }),
            CS("Cs", { CachedCategory(CharCategory.SURROGATE.ordinal, false, true) }),
            ISP("IsP", { CachedCategoryScope((1 shl CharCategory.DASH_PUNCTUATION.ordinal)
                    or (1 shl CharCategory.START_PUNCTUATION.ordinal)
                    or (1 shl CharCategory.END_PUNCTUATION.ordinal)
                    or (1 shl CharCategory.CONNECTOR_PUNCTUATION.ordinal)
                    or (1 shl CharCategory.OTHER_PUNCTUATION.ordinal)
                    or (1 shl CharCategory.INITIAL_QUOTE_PUNCTUATION.ordinal)
                    or (1 shl CharCategory.FINAL_QUOTE_PUNCTUATION.ordinal), true) }),
            PD("Pd", { CachedCategory(CharCategory.DASH_PUNCTUATION.ordinal, false) }),
            PS("Ps", { CachedCategory(CharCategory.START_PUNCTUATION.ordinal, false) }),
            PE("Pe", { CachedCategory(CharCategory.END_PUNCTUATION.ordinal, false) }),
            PC("Pc", { CachedCategory(CharCategory.CONNECTOR_PUNCTUATION.ordinal, false) }),
            PO("Po", { CachedCategory(CharCategory.OTHER_PUNCTUATION.ordinal, true) }),
            ISS("IsS", { CachedCategoryScope(0x7E000000, true) }),
            SM("Sm", { CachedCategory(CharCategory.MATH_SYMBOL.ordinal, true) }),
            SC("Sc", { CachedCategory(CharCategory.CURRENCY_SYMBOL.ordinal, false) }),
            SK("Sk", { CachedCategory(CharCategory.MODIFIER_SYMBOL.ordinal, false) }),
            SO("So", { CachedCategory(CharCategory.OTHER_SYMBOL.ordinal, true) }),
            PI("Pi", { CachedCategory(CharCategory.INITIAL_QUOTE_PUNCTUATION.ordinal, false) }),
            PF("Pf", { CachedCategory(CharCategory.FINAL_QUOTE_PUNCTUATION.ordinal, false)  })
        }

        private val classCache = Array<Lazy<CachedCharClass>>(CharClasses.entries.size) { idx ->
            lazy {
                CharClasses.entries[idx].factory()
            }
        }
        private val classCacheMap = CharClasses.entries.associate { it -> it.regexName to it }

        fun intersects(ch1: Int, ch2: Int): Boolean = ch1 == ch2
        fun intersects(cc: XRAbstractCharClass, ch: Int): Boolean = cc.contains(ch)

        fun intersects(cc1: XRAbstractCharClass, cc2: XRAbstractCharClass): Boolean {
            if (cc1.bits == null || cc2.bits == null) {
                return true
            }
            return cc1.bits!!.intersects(cc2.bits!!)
        }

        fun getPredefinedClass(name: String, negative: Boolean, version: Version): XRAbstractCharClass {
            val charClass = classCacheMap[name] ?: when(version) {
                Version.V1_0 -> throw XRPatternSyntaxException("No such character class ($name)")
                else -> classCacheMap["all"]!! // xsd 1.1 allows unknown classes }
            }
            val cachedClass = classCache[charClass.ordinal].value
            return cachedClass.getValue(negative)
        }
    }
}
