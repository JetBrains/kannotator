package index

import java.io.File
import java.util.ArrayList
import junit.framework.TestCase
import kotlinlib.recurseFiltered
import org.jetbrains.kannotator.declarations.*
import org.jetbrains.kannotator.index.DeclarationIndexImpl
import org.jetbrains.kannotator.index.FileBasedClassSource
import org.jetbrains.kannotator.util.processJar
import org.objectweb.asm.ClassVisitor
import org.objectweb.asm.MethodVisitor
import org.objectweb.asm.Opcodes
import kotlin.test.fail

class MethodIndexTest : TestCase() {

    fun doTest(jars: Collection<File>) {
        val methods = ArrayList<Method>()

        measureTime("Loading methods: ") {
            for (jar in jars) {
                processJar(jar) {
                    file, owner, reader ->
                    val className = ClassName.fromInternalName(reader.getClassName())
                    reader.accept(object : ClassVisitor(Opcodes.ASM4) {
                        override fun visitMethod(access: Int, name: String, desc: String, signature: String?, exceptions: Array<out String>?): MethodVisitor? {
                            val method = Method(className, access, name, desc, signature)
                            methods.add(method)
                            return null
                        }
                    }, 0)
                }
            }
        }


        println("${methods.size} methods found")


        measureTime("Building index: ") {
            watch ->
            val source = FileBasedClassSource(jars)
            val index = DeclarationIndexImpl(source)
            watch.dump()
            for (method in methods) {
                val found = index.findMethod(method.declaringClass, method.id.methodName, method.id.methodDesc)
                if (found == null) {
                    fail("Method not found: ${method.toFullString()}")
                }
                else if (method != found) {
                    fail("Wrong method found. Expected\n" + method.toFullString() + "\nbut was\n" + found.toFullString())
                }
            }
        }
    }

    fun test() {
        val dirs = arrayList(
                java.io.File("/System/Library/Java/JavaVirtualMachines/1.6.0.jdk/Contents/Classes"),
                java.io.File("/System/Library/Java/JavaVirtualMachines/1.6.0.jdk/Contents/Home/lib"),
                java.io.File("lib")
        )

        val jars = ArrayList<File>()
        for (dir in dirs) {
            dir.recurseFiltered({it.extension == "jar"}) {
                jars.add(it)
            }
        }

        for (jar in jars) {
            println(jar)
            doTest(arrayList(jar))
        }
    }

}

class StopWatch(val title: String? = null) {
    var time = 0.toLong()

    fun start() {
        time = System.nanoTime()
    }

    fun dump() {
        println((title ?: "Time") + ": " + (System.nanoTime() - time) / 1e+9 + "s")
    }

    fun dumpAndStart() {
        dump()
        start()
    }
}

fun measureTime(title: String? = null, body: (watch: StopWatch) -> Unit) {
    val watch = StopWatch(title)
    watch.start()
    body(watch)
    watch.dump()
}