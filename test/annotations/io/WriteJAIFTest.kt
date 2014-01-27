package annotations.io

import java.io.File
import org.junit.Test
import org.junit.Assert
import kotlinlib.toUnixSeparators
import org.jetbrains.kannotator.index.ClassSource
import org.jetbrains.kannotator.index.DeclarationIndexImpl
import org.jetbrains.kannotator.annotations.io.writeAnnotationsToJaif

public class WriteJAIFTest {

    fun doTest(klass: Class<*>) {
        val classSource: ClassSource = util.Classes(klass)

        val annotations = util.loadNullabilityAnnotations(classSource)
        val declarationIndex = DeclarationIndexImpl(classSource)

        val specFile = File("testData/${klass.getCanonicalName()!!.replace('.', '/')}.jaif")
        val actualFile = File.createTempFile("writeJaif", "${klass.getSimpleName()}.jaif")
        println("Saved file: ${actualFile.getAbsolutePath()}")

        writeAnnotationsToJaif(
                declIndex = declarationIndex,
                destRoot = actualFile.getParentFile()!!,
                fileName = actualFile.name.replace(".jaif", ""),
                nullability = annotations,
                propagatedNullabilityPositions = setOf()
        )

        Assert.assertEquals(
                specFile.readText().trim().toUnixSeparators(),
                actualFile.readText().trim().toUnixSeparators()
        )
    }

    Test fun testNotNullFields() {
        doTest(javaClass<annotations.io.spec.NotNullFields>())
    }

    Test fun testNullableFields() {
        doTest(javaClass<annotations.io.spec.NullableFields>())
    }

    Test fun testNotNullArgs() {
        doTest(javaClass<annotations.io.spec.NotNullArgs>())
    }

    Test fun testNullableArgs() {
        doTest(javaClass<annotations.io.spec.NullableArgs>())
    }

    Test fun testNotNullReturn() {
        doTest(javaClass<annotations.io.spec.NotNullReturn>())
    }

    Test fun testNullableReturn() {
        doTest(javaClass<annotations.io.spec.NullableReturn>())
    }

}
