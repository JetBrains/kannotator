package org.jetbrains.kannotator.kotlinSignatures

fun substitute(genericType: GenericType, substitution: (name: String) -> GenericType?): GenericType {
    val classifier = genericType.classifier
    return when (classifier) {
        is BaseType -> genericType
        is NamedClass -> substituteArguments(classifier, genericType.arguments, substitution)
        Array -> ImmutableGenericType(Array, arrayList(NoWildcard(substitute(genericType.arrayElementType, substitution))))
        is TypeVariable -> substitution(classifier.name) ?: genericType
        else -> throw IllegalArgumentException(classifier.toString())
    }
}

private fun substituteArguments(
        classifier: NamedClass,
        arguments: List<TypeArgument>,
        substitution: (name: String) -> GenericType?
): GenericType {
    return ImmutableGenericType(
            classifier,
            arguments.map {
               arg ->
               when (arg) {
                   UnBoundedWildcard -> arg
                   is BoundedWildcard -> BoundedWildcard(arg.wildcard, substitute(arg.bound, substitution))
                   is NoWildcard -> NoWildcard(substitute(arg.genericType, substitution))
                   else -> throw IllegalArgumentException(arg.toString())
               }
            }
    )
}