package index

import org.junit.Assert
import org.junit.Test
import java.io.File
import java.util.HashSet
import org.jetbrains.kannotator.index.FileBasedClassSource
import org.jetbrains.kannotator.index.DeclarationIndexImpl
import util.findJarsInLibFolder
import util.collectAllAnnotationKeysTo

/**
 * Checks that each annotation key (from external annotations in lib folder)
 * can be found in declaration index (for lib folder).
 */
class AnnotationIndexTest {
    @Test fun indexesInLibFolder() {
        val keys = HashSet<String>()
        File("lib").collectAllAnnotationKeysTo(keys)

        val source = FileBasedClassSource(findJarsInLibFolder())
        val index = DeclarationIndexImpl(source, failOnDuplicates = false)

        for (key in keys) {
            Assert.assertNotNull(
                    "Position not found for $key",
                    index.findPositionByAnnotationKeyString(key)
            )
        }

    }
}
