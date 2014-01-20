package annotations.io

import java.io.File
import org.junit.Test
import org.junit.Assert
import kotlinlib.toUnixSeparators
import org.jetbrains.kannotator.index.ClassSource
import org.jetbrains.kannotator.index.DeclarationIndexImpl
import org.jetbrains.kannotator.annotations.io.writeAnnotationsToJaif

public class WriteJaifTest {

    fun doTest(specFile: File, vararg classes: Class<*>) {
        val classSource: ClassSource = util.Classes(classes.toList())

        val annotations = util.loadNullabilityAnnotations(classSource)
        val declarationIndex = DeclarationIndexImpl(classSource)

        val actualFile = File.createTempFile("writeJaif", specFile.name)
        println("Saved file: ${actualFile.getAbsolutePath()}")

        writeAnnotationsToJaif(
                declIndex = declarationIndex,
                destRoot = actualFile.getParentFile()!!,
                fileName = actualFile.name.replace(".jaif", ""),
                nullability = annotations,
                propagatedNullabilityPositions = setOf(),
                includeNullable = true
        )

        Assert.assertEquals(
                specFile.readText().trim().toUnixSeparators(),
                actualFile.readText().trim().toUnixSeparators()
        )
    }

    fun simpleTest(klass: Class<*>) {
        doTest(specFile(klass), klass)
    }

    fun specFile(klass: Class<*>): File {
        return File("testData/${klass.getName()!!.replace('.', '/')}.jaif")
    }

    Test fun testNotNullFields() {
        simpleTest(javaClass<annotations.io.spec.NotNullFields>())
    }

    Test fun testNullableFields() {
        simpleTest(javaClass<annotations.io.spec.NullableFields>())
    }

    Test fun testNotNullArgs() {
        simpleTest(javaClass<annotations.io.spec.NotNullArgs>())
    }

    Test fun testNullableArgs() {
        simpleTest(javaClass<annotations.io.spec.NullableArgs>())
    }

    Test fun testNotNullReturn() {
        simpleTest(javaClass<annotations.io.spec.NotNullReturn>())
    }

    Test fun testNullableReturn() {
        simpleTest(javaClass<annotations.io.spec.NullableReturn>())
    }

    Test fun testNestedClassesA(){
        simpleTest(javaClass<annotations.io.spec.Nested.A>())
    }

    Test fun testPackage() {
        doTest(
                File("testData/annotations/io/spec/package.jaif"),
                javaClass<annotations.io.spec.Nested.A>(),
                javaClass<annotations.io.spec.NotNullArgs>(),
                javaClass<annotations.io.spec.NotNullFields>(),
                javaClass<annotations.io.spec.NotNullReturn>(),
                javaClass<annotations.io.spec.NullableArgs>(),
                javaClass<annotations.io.spec.NullableFields>(),
                javaClass<annotations.io.spec.NullableReturn>())
    }

}
