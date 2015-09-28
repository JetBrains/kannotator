package kotlinlib

import java.util.regex.Matcher

operator fun Matcher.get(groupIndex: Int): String? = group(groupIndex)