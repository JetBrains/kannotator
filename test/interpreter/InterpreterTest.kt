package interpreter

import junit.framework.TestCase
import org.objectweb.asm.commons.Method as AsmMethod
import java.io.File
import org.objectweb.asm.Type
import org.objectweb.asm.ClassReader

class InterpreterTest : TestCase() {

    fun testSimpleIf() {
        doTest(javaClass<interpreter.simpleIf.Test>())
    }

    fun testOneParam() {
        doTest(javaClass<interpreter.oneParam.Test>())
    }

    fun testStatic() {
        doTest(javaClass<interpreter._static.Test>())
    }

    fun testLoops() {
        doTest(javaClass<interpreter.loops.Test>())
    }

    fun testLongSyntheticAccessor() {
        doTest(javaClass<interpreter._long.Test.Listener>())
    }

    fun testJSR_RET() {
        val name = "EDU/oswego/cs/dl/util/concurrent/ClockDaemon\$RunLoop"
        doTest(File("testData/interpreter"), ClassReader(name))
    }

    fun testPrimitiveParamsAndReturn() {
        doTest(javaClass<interpreter.primitiveParamsAndReturn.Test>())
    }
}