package util.junit

import junit.framework.TestCase

public fun TestCase.getTestName(lowercaseFirstLetter: Boolean) : String {
    return getTestName(getName(), lowercaseFirstLetter)
}

public fun TestCase.getTestName(name: String?, lowercaseFirstLetter: Boolean) : String {
    if (name == null) {
        return "";
    }

    val trimmedName = name.trimLeading("test")
    if (trimmedName.isEmpty()) {
        return ""
    }

    return trimmedName.lowercaseFirstLetter(lowercaseFirstLetter);
}

private fun String.lowercaseFirstLetter(lowercaseFirstLetter: Boolean) : String {
    if (lowercaseFirstLetter && !isAllUppercaseName()) {
        return Character.toLowerCase(charAt(0)).toString() + substring(1);
    }

    return this;
}

private fun String.isAllUppercaseName() : Boolean = toCharArray() all { !Character.isLowerCase(it) }