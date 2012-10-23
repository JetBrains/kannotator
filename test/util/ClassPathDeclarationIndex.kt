package util

import java.util.HashMap
import org.jetbrains.kannotator.declarations.ClassName
import org.jetbrains.kannotator.declarations.Method
import org.objectweb.asm.ClassReader
import org.objectweb.asm.ClassReader.*
import org.objectweb.asm.tree.ClassNode
import org.jetbrains.kannotator.index.DeclarationIndex
import org.jetbrains.kannotator.declarations.Field
import java.util.HashSet

object ClassPathDeclarationIndex : DeclarationIndex {
    data class SearchQuery(val owner : ClassName, val name: String, val desc: String)

    private val cache = HashMap<SearchQuery, Method>()

    override fun findMethod(owner: ClassName, name: String, desc: String) : Method? {
        val query = SearchQuery(owner, name, desc)
        if (!cache.containsKey(query)) {
            search(query)
        }

        return cache[query]
    }

    override fun findField(owner: ClassName, name: String): Field? {
        throw UnsupportedOperationException()
    }

    private fun search(query : SearchQuery) {
        val stream = ClassLoader.getSystemResourceAsStream(query.owner.internal + ".class")
        if (stream == null) {
            return
        }

        val reader = ClassReader(stream)
        val node = ClassNode()
        reader.accept(node, SKIP_CODE or SKIP_DEBUG or SKIP_FRAMES)

        val foundPairs = node.methods.map {
            val methodNode = it
            val className = ClassName.fromInternalName(node.name)
            val methodQuery = SearchQuery(className, methodNode.name, methodNode.desc)
            val method = Method(className, methodNode.access, methodNode.name, methodNode.desc, methodNode.signature)

            Pair(methodQuery, method)
        }

        cache.putAll(*foundPairs.toArray(array()))
    }
}