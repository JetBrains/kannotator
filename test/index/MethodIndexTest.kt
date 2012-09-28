package index

import junit.framework.TestCase
import junit.framework.Assert.*
import java.io.File
import java.util.ArrayList
import java.util.HashSet
import kotlinlib.recurseFiltered
import org.jetbrains.kannotator.declarations.ClassName
import org.jetbrains.kannotator.declarations.Method
import org.jetbrains.kannotator.util.processJar
import org.objectweb.asm.ClassVisitor
import org.objectweb.asm.MethodVisitor
import org.objectweb.asm.Opcodes
import org.jetbrains.kannotator.index.DeclarationIndexImpl
import org.jetbrains.kannotator.index.FileBasedClassSource

class MethodIndexTest : TestCase() {

    fun test() {
        val methods = HashSet<Method>()
        val dirs = arrayList(
//                java.io.File("/System/Library/Java/JavaVirtualMachines/1.6.0.jdk/Contents/Classes"),
//                java.io.File("/System/Library/Java/JavaVirtualMachines/1.6.0.jdk/Contents/Home/lib"),
                java.io.File("lib")
        )

        val jars = ArrayList<File>()
        for (dir in dirs) {
            dir.recurseFiltered({it.extension == "jar"}) {
                jars.add(it)
            }
        }

        for (jar in jars) {
            processJar(jar) {
                file, owner, reader ->
                reader.accept(object : ClassVisitor(Opcodes.ASM4) {
                    override fun visitMethod(access: Int, name: String, desc: String, signature: String?, exceptions: Array<out String>?): MethodVisitor? {
                        val method = Method(ClassName.fromInternalName(reader.getClassName()), access, name, desc, signature)
                        methods.add(method)
                        return null
                    }
                }, 0)
            }
        }
        println("${methods.size} methods found")

        val source = FileBasedClassSource(jars)
        val index = DeclarationIndexImpl {n -> source[n]}
        for (method in methods) {
            val found = index.findMethod(method.declaringClass, method.id.methodName, method.id.methodDesc)
            if (found == null) {
                fail("Method not found: $method")
            }
            if (found != method) {
                assertEquals("Wrong method found", method, found)
            }
        }
    }


}
