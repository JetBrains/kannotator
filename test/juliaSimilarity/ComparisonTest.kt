package juliaSimilarity

import junit.framework.TestCase
import org.junit.Assert
import annotations.io.IndexFileWriter
import annotations.io.IndexFileParser
import annotations.el.AScene
import org.junit.Test
import java.util.HashMap
import annotations.el.AClass

//fixme: @SupressWarnings should be included here
private val juliaToKannotatorReplacementMap = hashMapOf(
        Pair("@checkers.nullness.quals.Nullable", "@org.jetbrains.annotations.Nullable"),
        Pair("@checkers.nullness.quals.NotNull", "@org.jetbrains.annotations.NotNull"),
        Pair("@checkers.nullness.quals.PolyNull", ""),
        Pair("@checkers.nullness.quals.Raw", "")
        )

private val kannotatorToJuliaReplacementMap = juliaToKannotatorReplacementMap.invert()

private fun <K, V, R> Map<K, V>.fold(initial: R, operation: (R, Map.Entry<K, V>) -> R): R {
    var answer = initial
    for (c in this) answer = operation(answer, c)
    return answer
}
private fun   Map<String,String>.invert() :Map<String,String> {
    val result = HashMap<String,String>(this.size)
    for ((key,value) in this)
       if (!value.isEmpty()) result.put(value,key)
    return result
}

private fun sceneFromFile(filename: String): AScene
{
    val scene = AScene()
    IndexFileParser.parseFile(filename, scene)
    return scene
}

private fun String.applyTransform(transform: Map<String, String>?) =
        if (transform != null)
            transform.fold (this, {(s, kvp)-> s.replace(kvp.key, kvp.value) })
        else this

private  fun Map<String, String>.toInterspersedString() = values().fold("", {(h, t)-> h + "\n//\$\n" + t })
/**
 * Now both scenes should have the same sets of packages and classes. The annotations, however, will differ
 */
fun homogenizeScenes(fst: AScene, snd: AScene)
{
    for ((packName, _) in fst.packages)
        snd.packages.vivify(packName)
    for ((packName, _) in snd.packages)
        fst.packages.vivify(packName)

    for ((className, classRecord) in fst.classes)
        toCommonStructure(classRecord, snd.classes.vivify(className))

    for ((className, classRecord) in snd.classes)
        toCommonStructure(classRecord, fst.classes.vivify(className))
}

private fun toCommonStructure(fst: AClass, snd: AClass)
{
    for ((fieldName, _) in fst.fields) snd.fields.vivify(fieldName)
    for ((fieldName, _) in snd.fields) fst.fields.vivify(fieldName)
    for ((methodName, _) in fst.methods) snd.methods.vivify(methodName)
    for ((methodName, _) in snd.methods) fst.methods.vivify(methodName)
}




private  fun compareTwoFiles(fst: String, snd: String, fstAnnotationTransform: Map<String, String>? = null, sndAnnotationTransform: Map<String, String>? = null)
{
    val fstScene = sceneFromFile(fst)
    val sndScene = sceneFromFile(snd)
    homogenizeScenes(fstScene, sndScene)
    val fstClasses = IndexFileWriter(fstScene).getClassRepresentations()
    val sndClasses = IndexFileWriter(sndScene).getClassRepresentations()
    val fstString = fstClasses.toInterspersedString().applyTransform(fstAnnotationTransform)
    val sndString = sndClasses.toInterspersedString().applyTransform(sndAnnotationTransform)

    Assert.assertEquals(fstString, sndString)
}

class ComparisonTest : TestCase()
{
     Test fun testComparison() {
        compareTwoFiles("jaifUtil/_good.jaif", "jaifUtil/_reduced.jaif", juliaToKannotatorReplacementMap, juliaToKannotatorReplacementMap)
    }
}
