package org.jetbrains.kannotator.annotationsInference.mutability

import org.objectweb.asm.tree.MethodInsnNode

val mutableInterfaces: Map<String, List<String>> = hashMap(
        "java/util/Collection" to arrayList("add", "remove", "addAll", "removeAll", "retainAll", "clear"),
        "java/util/List" to arrayList("add", "remove", "addAll", "removeAll", "retainAll", "clear", "set", "add", "remove"),
        "java/util/Set" to arrayList("add", "remove", "AddAll", "removeAll", "retainAll", "clear"),
        "java/util/Map" to arrayList("put", "remove", "putAll", "clear"),
        "java/util/Map\$Entry" to arrayList("setValue"),
        "java/util/Iterator" to arrayList("remove"),
        "java/util/ListIterator" to arrayList("remove", "set", "add")
)

val propagatingMutability: Map<String, List<String>> = hashMap(
        "java/util/Collection" to arrayList("iterator"),
        "java/util/List" to arrayList("iterator", "listIterator", "subList"),
        "java/util/Set" to arrayList("iterator"),
        "java/util/SortedSet" to arrayList("iterator", "subSet", "headSet", "tailSet"),
        "java/util/NavigableSet" to
            arrayList("iterator", "subSet", "headSet", "tailSet", "descendingSet", "descendingIterator", ""),
        "java/util/Map" to arrayList("keySet", "values", "entrySet"),
        "java/util/SortedMap" to arrayList("keySet", "values", "entrySet", "subMap", "headMap", "tailMap"),
        "java/util/NavigableMap" to
            arrayList("keySet", "values", "entrySet", "subMap", "headMap", "tailMap", "descendingMap", "navigableKeySet", "descendingKeySet")
)

fun MethodInsnNode.isMutatingInvocation() : Boolean =
        mutableInterfaces.containsInvocation(this)

fun MethodInsnNode.isMutabilityPropagatingInvocation() : Boolean =
        propagatingMutability.containsInvocation(this)

private fun Map<String, List<String>>.containsInvocation(instruction: MethodInsnNode) : Boolean {
    val className = instruction.owner!!
    val methodName = instruction.name!!
    return this[className]?.contains(methodName) ?: false
}
