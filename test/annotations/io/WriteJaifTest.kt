package annotations.io

import java.io.File
import org.junit.Test
import org.junit.Assert
import kotlinlib.toUnixSeparators
import org.jetbrains.kannotator.index.ClassSource
import org.jetbrains.kannotator.index.DeclarationIndexImpl
import org.jetbrains.kannotator.annotations.io.writeAnnotationsToJaif

/** Reads annotations from classes in testData/annotations/io/spec/ from bytecode,
 *  puts them into JAIF and checks against expected JAIFs.
 */
public class WriteJaifTest {

    fun doTest(specFile: File, vararg classes: Class<*>) {
        val classSource: ClassSource = util.Classes(classes.toList())

        val annotations = util.loadNullabilityAnnotations(classSource)
        val declarationIndex = DeclarationIndexImpl(classSource)

        val actualFile = File.createTempFile("writeJaif", specFile.name)
        println("Saved file: ${actualFile.absolutePath}")

        writeAnnotationsToJaif(
                declIndex = declarationIndex,
                destRoot = actualFile.parentFile!!,
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
        return File("testData/${klass.name!!.replace('.', '/')}.jaif")
    }

    @Test fun testNotNullFields() {
        simpleTest(annotations.io.spec.NotNullFields::class.java)
    }

    @Test fun testNullableFields() {
        simpleTest(annotations.io.spec.NullableFields::class.java)
    }

    @Test fun testNotNullArgs() {
        simpleTest(annotations.io.spec.NotNullArgs::class.java)
    }

    @Test fun testNullableArgs() {
        simpleTest(annotations.io.spec.NullableArgs::class.java)
    }

    @Test fun testNotNullReturn() {
        simpleTest(annotations.io.spec.NotNullReturn::class.java)
    }

    @Test fun testNullableReturn() {
        simpleTest(annotations.io.spec.NullableReturn::class.java)
    }

    @Test fun testNestedClassesA(){
        simpleTest(annotations.io.spec.Nested.A::class.java)
    }

    @Test fun testPackage() {
        doTest(
                File("testData/annotations/io/spec/package.jaif"),
                annotations.io.spec.Nested.A::class.java,
                annotations.io.spec.NotNullArgs::class.java,
                annotations.io.spec.NotNullFields::class.java,
                annotations.io.spec.NotNullReturn::class.java,
                annotations.io.spec.NullableArgs::class.java,
                annotations.io.spec.NullableFields::class.java,
                annotations.io.spec.NullableReturn::class.java)
    }

}
