package com.acme.packageprivate

/**
 * Marks a declaration as package-private.
 * Elements annotated with this annotation can only be accessed from within the same package.
 *
 * @property scope Optional override for package scope (useful for generated code or moved packages)
 */
@Target(
    AnnotationTarget.CLASS,
    AnnotationTarget.FUNCTION,
    AnnotationTarget.PROPERTY,
    AnnotationTarget.CONSTRUCTOR
)
@Retention(AnnotationRetention.BINARY)
annotation class PackagePrivate(
    val scope: String = ""
)
