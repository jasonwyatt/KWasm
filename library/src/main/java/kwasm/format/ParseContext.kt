package kwasm.format

data class ParseContext(val fileName: String, val lineNumber: Int, val column: Int) {
    override fun toString(): String = "${fileName}L$lineNumber:$column"
}
