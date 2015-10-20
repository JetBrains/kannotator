package util.junit

import junit.framework.TestCase
import util.NamedTest

public fun NamedTest.getTestName(lowercaseFirstLetter: Boolean) : String {
    return getTestName(getName(), lowercaseFirstLetter)
}

public fun NamedTest.getTestName(name: String?, lowercaseFirstLetter: Boolean) : String {
    if (name == null) {
        return "";
    }

    val trimmedName = name.removePrefix("test")
    if (trimmedName.isEmpty()) {
        return ""
    }

    return trimmedName.lowercaseFirstLetter(lowercaseFirstLetter);
}

private fun String.lowercaseFirstLetter(lowercaseFirstLetter: Boolean) : String {
    if (lowercaseFirstLetter && !isAllUppercaseName()) {
        return Character.toLowerCase(this[0]).toString() + substring(1);
    }

    return this;
}

private fun String.isAllUppercaseName() : Boolean = toCharArray().all { !Character.isLowerCase(it) }