package org.jetbrains.kannotator.funDependecy

import java.util.HashMap
import org.jetbrains.kannotator.declarations.ClassName
import org.jetbrains.kannotator.declarations.Method
import org.jetbrains.kannotator.funDependecy.GlobalMethodSearcher.SearchQuery
import org.objectweb.asm.ClassReader
import org.objectweb.asm.ClassReader.*
import org.objectweb.asm.tree.ClassNode

class GlobalMethodSearcher {
    data class SearchQuery(val owner : String, val name: String, val desc: String)

    private val cache = HashMap<SearchQuery, Method>()

    fun find(owner: String, name: String, desc: String) : Method? {
        val query = SearchQuery(owner, name, desc)
        if (!cache.containsKey(query)) {
            search(query)
        }

        return cache[query]
    }

    private fun search(query : SearchQuery) {
        val stream = ClassLoader.getSystemResourceAsStream(query.owner + ".class")
        if (stream == null) {
            return
        }

        val reader = ClassReader(stream)
        val node = ClassNode()
        reader.accept(node, SKIP_CODE or SKIP_DEBUG or SKIP_FRAMES)

        val foundPairs = node.methods.map {
            val methodNode = it!!
            val methodQuery = SearchQuery(node.name, methodNode.name, methodNode.desc)
            val method = Method(ClassName.fromInternalName(node.name), methodNode.access, methodNode.name, methodNode.desc, methodNode.signature)

            Pair(methodQuery, method)
        }

        cache.putAll(*foundPairs.toArray(array()))
    }
}