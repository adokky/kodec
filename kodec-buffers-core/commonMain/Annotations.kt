package io.kodec.buffers

import kotlin.annotation.AnnotationTarget.*

@RequiresOptIn(
    "This is internal kodec-buffers API. " +
    "There is no backward compatibility guarantees (to the point this API " +
    "can be completely removed in future releases). Use at your own risk"
)
@MustBeDocumented
@Target(
    CLASS,
    ANNOTATION_CLASS,
    PROPERTY,
    FIELD,
    LOCAL_VARIABLE,
    VALUE_PARAMETER,
    CONSTRUCTOR,
    FUNCTION,
    PROPERTY_GETTER,
    PROPERTY_SETTER,
    TYPEALIAS
)
@Retention(AnnotationRetention.BINARY)
annotation class InternalBuffersApi

@RequiresOptIn(
    "No backward compatibility guarantees. Use at your own risk",
    level = RequiresOptIn.Level.WARNING
)
@MustBeDocumented
@Target(
    CLASS,
    ANNOTATION_CLASS,
    PROPERTY,
    FIELD,
    LOCAL_VARIABLE,
    VALUE_PARAMETER,
    CONSTRUCTOR,
    FUNCTION,
    PROPERTY_GETTER,
    PROPERTY_SETTER,
    TYPEALIAS
)
@Retention(AnnotationRetention.BINARY)
annotation class ExperimentalBuffersApi