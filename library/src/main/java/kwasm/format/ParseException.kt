package kwasm.format

data class ParseException(
    val errorMsg: String,
    val parseContext: ParseContext? = null
) : Exception(
    "(${parseContext ?: "Unknown Context"}) $errorMsg"
)
