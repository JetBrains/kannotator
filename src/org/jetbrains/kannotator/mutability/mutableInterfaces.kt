package org.jetbrains.kannotator.mutability


val mutableInterfaces: Map<String, List<String>> = hashMap(
        "java.util.Collection" to arrayList("add", "remove", "addAll", "removeAll", "retainAll", "clear"),
        "java.util.List" to arrayList("add", "remove", "addAll", "removeAll", "retainAll", "clear", "set", "add", "remove"),
        "java.util.Iterator" to arrayList("remove")
)

val propagatingMutability: Map<String, List<String>> = hashMap(
        "java.util.Collection" to arrayList("iterator")
)
