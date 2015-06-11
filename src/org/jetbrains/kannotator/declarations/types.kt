package org.jetbrains.kannotator.declarations

enum class Variance {
    COVARIANT,
    CONTRAVARIANT,
    INVARIANT
}

interface PositionWithinDeclaration {
    val variance: Variance
}

object FIELD_TYPE : PositionWithinDeclaration {
    override val variance: Variance
        get() = Variance.INVARIANT

    override fun toString() = "FIELD_TYPE"
}

object RETURN_TYPE : PositionWithinDeclaration {
    override val variance: Variance
        get() = Variance.COVARIANT

    override fun toString() = "RETURN_TYPE"
}

data class ParameterPosition(val index: Int) : PositionWithinDeclaration {
    override val variance: Variance
        get() = Variance.CONTRAVARIANT    
}

interface AnnotationPosition {
    val member: ClassMember
    val relativePosition: PositionWithinDeclaration
}

interface MethodTypePosition : AnnotationPosition {
    val method: Method
    override val relativePosition: PositionWithinDeclaration
}

interface FieldTypePosition : AnnotationPosition {
    val field: Field
}

interface AnnotatedType {
    val position: AnnotationPosition
    val arguments: List<AnnotatedType>
}