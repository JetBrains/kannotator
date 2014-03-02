package org.jetbrains.kannotator.declarations

enum class Variance {
    COVARIANT
    CONTRAVARIANT
    INVARIANT
}

trait PositionWithinDeclaration {
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

trait AnnotationPosition {
    val member: ClassMember
    val relativePosition: PositionWithinDeclaration
}

trait MethodTypePosition : AnnotationPosition {
    val method: Method
    override val relativePosition: PositionWithinDeclaration
}

trait FieldTypePosition : AnnotationPosition {
    val field: Field
}

trait AnnotatedType {
    val position: AnnotationPosition
    val arguments: List<AnnotatedType>
}