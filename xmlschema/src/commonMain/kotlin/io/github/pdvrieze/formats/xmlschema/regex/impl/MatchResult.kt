/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package io.github.pdvrieze.formats.xmlschema.regex.impl

import io.github.pdvrieze.formats.xmlschema.regex.XMatchGroup

/**
 * Represents a collection of captured groups in a single match of a regular expression.
 *
 * This collection has size of `groupCount + 1` where `groupCount` is the count of groups in the regular expression.
 * Groups are indexed from 1 to `groupCount` and group with the index 0 corresponds to the entire match.
 *
 * An element of the collection at the particular index can be `null`,
 * if the corresponding group in the regular expression is optional and
 * there was no match captured by that group.
 */
public interface XMatchGroupCollection : Collection<XMatchGroup?> {

    /** Returns a group with the specified [index].
     *
     * @return An instance of [MatchGroup] if the group with the specified [index] was matched or `null` otherwise.
     *
     * Groups are indexed from 1 to the count of groups in the regular expression. A group with the index 0
     * corresponds to the entire match.
     */
    public operator fun get(index: Int): XMatchGroup?
}

/**
 * Extends [MatchGroupCollection] by introducing a way to get matched groups by name, when regex supports it.
 */
@SinceKotlin("1.1")
public interface XMatchNamedGroupCollection : XMatchGroupCollection {
    /**
     * Returns a named group with the specified [name].
     * @return An instance of [MatchGroup] if the group with the specified [name] was matched or `null` otherwise.
     * @throws IllegalArgumentException if there is no group with the specified [name] defined in the regex pattern.
     * @throws UnsupportedOperationException if this match group collection doesn't support getting match groups by name,
     * for example, when it's not supported by the current platform.
     */
    public operator fun get(name: String): XMatchGroup?
}

/**
 * Represents the results from a single regular expression match.
 */
public interface XMatchResult {
    /** The range of indices in the original string where match was captured. */
    public val range: IntRange
    /** The substring from the input string captured by this match. */
    public val value: String
    /**
     * A collection of groups matched by the regular expression.
     *
     * This collection has size of `groupCount + 1` where `groupCount` is the count of groups in the regular expression.
     * Groups are indexed from 1 to `groupCount` and group with the index 0 corresponds to the entire match.
     */
    public val groups: XMatchGroupCollection
    /**
     * A list of matched indexed group values.
     *
     * This list has size of `groupCount + 1` where `groupCount` is the count of groups in the regular expression.
     * Groups are indexed from 1 to `groupCount` and group with the index 0 corresponds to the entire match.
     *
     * If the group in the regular expression is optional and there were no match captured by that group,
     * corresponding item in [groupValues] is an empty string.
     *
     * @sample samples.text.Regexps.matchDestructuringToGroupValues
     */
    public val groupValues: List<String>

    /**
     * An instance of [MatchResult.Destructured] wrapper providing components for destructuring assignment of group values.
     *
     * component1 corresponds to the value of the first group, component2 — of the second, and so on.
     *
     * @sample samples.text.Regexps.matchDestructuringToGroupValues
     */
    public val destructured: Destructured get() = Destructured(this)

    /** Returns a new [MatchResult] with the results for the next match, starting at the position
     *  at which the last match ended (at the character after the last matched character).
     */
    public fun next(): XMatchResult?

    /**
     * Provides components for destructuring assignment of group values.
     *
     * [component1] corresponds to the value of the first group, [component2] — of the second, and so on.
     *
     * If the group in the regular expression is optional and there were no match captured by that group,
     * corresponding component value is an empty string.
     *
     * @sample samples.text.Regexps.matchDestructuringToGroupValues
     */
    public class Destructured internal constructor(public val match: XMatchResult) {
        public operator inline fun component1():  String = match.groupValues[1]

        public operator inline fun component2():  String = match.groupValues[2]

        public operator inline fun component3():  String = match.groupValues[3]

        public operator inline fun component4():  String = match.groupValues[4]

        public operator inline fun component5():  String = match.groupValues[5]

        public operator inline fun component6():  String = match.groupValues[6]

        public operator inline fun component7():  String = match.groupValues[7]

        public operator inline fun component8():  String = match.groupValues[8]

        public operator inline fun component9():  String = match.groupValues[9]

        public operator inline fun component10(): String = match.groupValues[10]

        /**
         *  Returns destructured group values as a list of strings.
         *  First value in the returned list corresponds to the value of the first group, and so on.
         *
         * @sample samples.text.Regexps.matchDestructuringToGroupValues
         */
        public fun toList(): List<String> = match.groupValues.subList(1, match.groupValues.size)
    }
}
