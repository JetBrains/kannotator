package index

import java.io.File
import java.util.Comparator
import kotlinlib.*
import org.jetbrains.kannotator.index.FieldDependencyInfo
import org.jetbrains.kannotator.index.buildFieldsDependencyInfos
import org.junit.Test
import util.ClassPathDeclarationIndex
import util.ClassesFromClassPath
import util.assertEqualsOrCreate

private val PATH = "testData/fieldDependency/"

class FieldDependencyTest {
    Test fun funInDifferentClassesTest() {
        doTest("simple/simple.txt", "fieldDependency.simple.Simple")
    }

    private val fieldInfoComparator = object : Comparator<FieldDependencyInfo> {
        public override fun compare(o1: FieldDependencyInfo, o2: FieldDependencyInfo): Int {
            return o2.field?.name?.compareTo(o1.field?.name ?: "") ?: -1
        }

        public override fun equals(obj: Any?): Boolean {
            throw UnsupportedOperationException()
        }
    }

    fun doTest(expectedResultPath: String, canonicalName: String) {
        val classSource = ClassesFromClassPath(canonicalName)
        val infos = buildFieldsDependencyInfos(util.ClassPathDeclarationIndex, classSource)

        val actual = buildString { sb ->
            for (fieldInfo in infos.values().sort(fieldInfoComparator)) {
                printFieldInfo(sb, fieldInfo)
            }
        }.trim()

        val expectedFile = File(PATH + expectedResultPath)
        assertEqualsOrCreate(expectedFile, actual)
    }

    fun printFieldInfo(sb: StringBuilder, fieldInfo: FieldDependencyInfo) {
        sb.println("=========== ${fieldInfo.field} ============")
        sb.println("== readers ==")
        if (fieldInfo.readers.isEmpty()) sb.println("<no readers>") else fieldInfo.readers.forEach { sb.println(it) }
        sb.println("== writers ==")
        if (fieldInfo.writers.isEmpty()) sb.println("<no writers>") else fieldInfo.writers.forEach { sb.println(it) }
    }
}

