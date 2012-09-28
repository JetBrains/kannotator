package kotlinlib

import java.util.regex.Matcher

fun Matcher.get(groupIndex: Int): String? = group(groupIndex)