package interpreter

import junit.framework.TestCase
import org.objectweb.asm.commons.Method as AsmMethod
import java.io.File
import org.objectweb.asm.Type
import org.objectweb.asm.ClassReader
import util.getClassReader

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

    fun testMixedVars() {
        doTest(javaClass<interpreter._long.MixedVars>())
    }

    fun testJSR_RET() {
        val name = "EDU/oswego/cs/dl/util/concurrent/ClockDaemon\$RunLoop"
        doTest(File("testData/interpreter"), getClassReader(name))
    }

    fun testPrimitiveParamsAndReturn() {
        doTest(javaClass<interpreter.primitiveParamsAndReturn.Test>())
    }

    fun testStaticInitializer() {
        doTest(javaClass<interpreter.staticInitializer.Test>())
    }
}