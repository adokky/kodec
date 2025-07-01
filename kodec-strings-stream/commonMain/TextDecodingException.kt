package io.kodec.text

import karamel.utils.NoStackTraceIllegalArgumentException

open class TextDecodingException(
    message: String,
    var position: Int = -1,
): NoStackTraceIllegalArgumentException(
    message.let { if (position >= 0) "$it at position $position" else it }
)