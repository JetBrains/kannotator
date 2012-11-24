package org.jetbrains.kannotator.annotations.io

import com.gs.collections.impl.multimap.set.UnifiedSetMultimap
import org.jetbrains.kannotator.declarations.*
import org.jetbrains.kannotator.index.ClassSource
import org.objectweb.asm.AnnotationVisitor
import org.objectweb.asm.ClassReader
import org.objectweb.asm.ClassVisitor
import org.objectweb.asm.MethodVisitor
import org.objectweb.asm.Opcodes
import org.objectweb.asm.Type
import java.util.HashSet
import org.objectweb.asm.FieldVisitor

public fun <A: Any> getAnnotationsFromClassFiles(
        classSource: ClassSource,
        // We use canonical names to avoid making users have annotations.jar on the class path
        annotationClassesToAnnotation: (canonicalAnnotationClassNames: Set<String>) -> A?
): Annotations<A> {
    val annotations = AnnotationsImpl<A>()

    classSource forEach {
        reader ->

        reader.accept(object : ClassVisitor(Opcodes.ASM4) {
                override fun visitMethod(access: Int, name: String, desc: String, signature: String?, exceptions: Array<out String>?): MethodVisitor? {
                    val method = Method(ClassName.fromInternalName(reader.getClassName()), access, name, desc, signature)
                    return object : MethodVisitor(Opcodes.ASM4) {
                        private val positions = PositionsForMethod(method)
                        private val canonicalAnnotationClassNames = UnifiedSetMultimap<AnnotationPosition, String>()

                        private fun setAnnotation(annotatedType: AnnotatedType, desc: String) {
                            val annotationClass = ClassName.fromType(Type.getType(desc)).canonicalName
                            canonicalAnnotationClassNames.put(annotatedType.position, annotationClass)
                        }

                        override fun visitAnnotation(desc: String, visible: Boolean): AnnotationVisitor? {
                            setAnnotation(positions.forReturnType(), desc)
                            return null
                        }

                        override fun visitParameterAnnotation(parameter: Int, desc: String, visible: Boolean): AnnotationVisitor? {
                            val thisOffset = if (method.isStatic()) 0 else 1
                            setAnnotation(positions.forParameter(parameter + thisOffset), desc)
                            return null
                        }

                        override fun visitEnd() {
                            positions.forEachValidPosition {
                                pos ->
                                val classNames = canonicalAnnotationClassNames[pos]!!
                                val annotation = annotationClassesToAnnotation(classNames)
                                annotations.setIfNotNull(pos, annotation)
                            }
                        }
                    }
                }

                public override fun visitField(access: Int, name: String, desc: String, signature: String?, value: Any?): FieldVisitor? {
                    val field = Field(ClassName.fromInternalName(reader.getClassName()), access, name, desc, signature, value)

                    return object : FieldVisitor(Opcodes.ASM4) {
                        private val canonicalAnnotationClassNames : MutableSet<String> = HashSet<String>()

                        public override fun visitAnnotation(desc: String, visible: Boolean): AnnotationVisitor? {
                            canonicalAnnotationClassNames.add(ClassName.fromType(Type.getType(desc)).canonicalName)
                            return null
                        }

                        public override fun visitEnd() {
                            val typePosition = getFieldTypePosition(field)
                            annotations.setIfNotNull(typePosition, annotationClassesToAnnotation(canonicalAnnotationClassNames))
                        }
                    }
                }
        }, ClassReader.SKIP_CODE)
    }

    return annotations
}
