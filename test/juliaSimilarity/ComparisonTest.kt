package juliaSimilarity

import junit.framework.TestCase
import org.junit.Assert
import annotations.io.IndexFileWriter
import annotations.io.IndexFileParser
import annotations.el.AScene
import org.junit.Test
import java.util.HashMap
import annotations.el.AClass
import annotations.el.AnnotationDef
import annotations.el.AElement
import annotations.SceneAnnotation
import kotlinlib.mapKeysAndValues
import annotations.el.BoundLocation
import annotations.el.ATypeElement
import annotations.el.TypeIndexLocation
import annotations.el.AMethod
import annotations.el.LocalLocation
import kotlinlib.mapValues
import org.jetbrains.kannotator.annotations.io.transformAnnotations
import java.util.Collections
import java.io.File
import annotations.util.coll.VivifyingMap
import org.jetbrains.kannotator.annotations.io.InferringParameters

//Other annotations are ignored!
private val juliaToKannotatorReplacementMap = hashMapOf(
        Pair("checkers.nullness.quals.Nullable", "org.jetbrains.annotations.Nullable"),
        Pair("checkers.nullness.quals.NotNull", "org.jetbrains.annotations.NotNull"),
        Pair("checkers.nullness.quals.PolyNull", ""),
        Pair("checkers.nullness.quals.Raw", "")
)

private fun <K, V, R> Map<K, V>.fold(initial: R, operation: (R, Map.Entry<K, V>) -> R): R {
    var answer = initial
    for (c in this) answer = operation(answer, c)
    return answer
}
//
//private fun   Map<String, String>.invert(): Map<String, String> {
//    val result = HashMap<String, String>(this.size)
//    for ((key, value) in this)
//        if (!value.isEmpty()) result.put(value, key)
//    return result
//}

private fun sceneFromFile(filename: String): AScene
{
    val scene = AScene()
    IndexFileParser.parseFile(filename, scene)
    return scene
}

private  fun Map<String, String>.toInterspersedString() = values().fold("", {(h, t)-> h + "\n//\$\n" + t })

/**
 * Now both scenes should have the same sets of packages and classes with the same methods/fields. The annotations, however, will differ
 */
fun homogenizeScenes(fst: AScene, snd: AScene)
{
    homogenize(fst.packages, snd.packages)
    homogenize(fst.classes, snd.classes)

    for ((className, classRecord) in fst.classes)
        toCommonStructure(classRecord, snd.classes.vivify(className))

    for ((className, classRecord) in snd.classes)
        toCommonStructure(classRecord, fst.classes.vivify(className))
}

private fun toCommonStructure(fst: AClass, snd: AClass)
{
    homogenize(fst.fields, snd.fields)
    homogenize(fst.methods, snd.methods)

    for((fieldName, fieldRecord) in fst.fields) {
        homogenize(fieldRecord.thisType?.innerTypes, snd.fields[fieldName]?.thisType?.innerTypes)
    }
}


fun <K, V> homogenize (fst: VivifyingMap<K, V>?, snd: VivifyingMap<K, V>?) {
    if (fst != null && snd != null){
        for ((k, _) in fst) snd.vivify(k)
        for ((k, _) in snd) fst.vivify(k)
    }
}


private fun  jaifComparableStrings(fstFile: String, sndFile: String): Pair<Map<String, String>, Map<String, String>> {
    val fstScene = sceneFromFile(fstFile)
    val sndScene = sceneFromFile(sndFile)
    homogenizeScenes(fstScene, sndScene)
    val fstClasses = IndexFileWriter(fstScene).getClassRepresentations()
    val sndClasses = IndexFileWriter(sndScene).getClassRepresentations()
    return Pair(fstClasses, sndClasses)
}


private fun AScene.comparableString() = IndexFileWriter(this).getClassRepresentations().toInterspersedString()
private fun AScene.writeClasses(filename: String) = File(filename).writeText(comparableString())


class ComparisonTest
{
     class object {
         val TEST_DATA_PATH = "testData/juliaSimilarity/"
     }


    Test fun extensions() = doTest("extensions")

    fun doTest(libraryName:String) {
        //launch kannotator for this file
//        val params = InferringParameters(
//                true,
//                false,
//                TEST_DATA_PATH + libraryName,
//                true,
//                "lib/"
//                optionsValues.format!!,
//                optionsValues.verbose
//        )
//        ConsoleInferringTask(params, System.err).perform()


        hasKannotatorChanged(libraryName)
        compareFiles(libraryName)
    }

    fun hasKannotatorChanged(name:String){
        val oldscene = sceneFromFile("$TEST_DATA_PATH$name/kannotator-template.jaif")
        val newscene = sceneFromFile("$TEST_DATA_PATH$name/kannotator.jaif")
        Assert.assertEquals(
                "Kannotator annotates file $TEST_DATA_PATH$name differently: check the changes and replace the template file if needed",
                oldscene.comparableString(), newscene.comparableString() )
    }

    fun compareFiles(name:String) {
        val juliaScene = sceneFromFile("$TEST_DATA_PATH$name/julia.jaif")
        val kannotatorScene = sceneFromFile("$TEST_DATA_PATH$name/kannotator.jaif")

        juliaScene.transformAnnotations { a->
            if (juliaToKannotatorReplacementMap.containsKey(a.def.name))
            {
                val newname = juliaToKannotatorReplacementMap[a.def.name]
                if (newname.notEmpty()){
                    val newAnnotationDef = AnnotationDef(newname)
                    newAnnotationDef.setFieldTypes(Collections.emptyMap())
                    SceneAnnotation(newAnnotationDef, Collections.emptyMap())
                }
                else null
            }
            else null
        }
        homogenizeScenes(juliaScene, kannotatorScene)

        juliaScene.writeClasses("$TEST_DATA_PATH$name/comparable-julia.jaif")
        kannotatorScene.writeClasses("$TEST_DATA_PATH$name/comparable-ckannotator.jaif")

        Assert.assertEquals("Julia has output a result different to kannotator's one for file $TEST_DATA_PATH$name.",
                juliaScene.comparableString(), kannotatorScene.comparableString())
    }
}
