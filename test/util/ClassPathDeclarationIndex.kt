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
    private val FIELD_DEFAULT_DESCRIPTOR = ""

    data class SearchQuery(val owner : ClassName, val name: String, val desc: String)

    private val cacheMethods = HashMap<SearchQuery, Method>()
    private val cacheFields = HashMap<SearchQuery, Field>()

    override fun findMethod(owner: ClassName, name: String, desc: String) : Method? {
        val query = SearchQuery(owner, name, desc)
        if (!cacheMethods.containsKey(query)) {
            search(query)
        }

        return cacheMethods[query]
    }

    override fun findField(owner: ClassName, name: String): Field? {
        val query = SearchQuery(owner, name, FIELD_DEFAULT_DESCRIPTOR)
        if (!cacheFields.containsKey(query)) {
            search(query)
        }

        return cacheFields[query]
    }

    private fun search(query : SearchQuery) {
        val stream = ClassLoader.getSystemResourceAsStream(query.owner.internal + ".class")
        if (stream == null) {
            return
        }

        val reader = ClassReader(stream)
        val node = ClassNode()
        reader.accept(node, SKIP_CODE or SKIP_DEBUG or SKIP_FRAMES)

        val className = ClassName.fromInternalName(node.name)

        node.methods.forEach {
            val methodNode = it
            val methodQuery = SearchQuery(className, methodNode.name, methodNode.desc)
            val method = Method(className, methodNode.access, methodNode.name, methodNode.desc, methodNode.signature)

            cacheMethods[methodQuery] = method
        }

        node.fields.forEach {
            val fieldNode = it
            val fieldQuery = SearchQuery(className, fieldNode.name, FIELD_DEFAULT_DESCRIPTOR)
            val field = Field(className, fieldNode.access, fieldNode.name, fieldNode.desc, fieldNode.signature, fieldNode.value)

            cacheFields[fieldQuery] = field
        }
    }
}