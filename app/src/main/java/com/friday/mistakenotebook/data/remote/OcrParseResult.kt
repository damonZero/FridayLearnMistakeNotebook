package com.friday.mistakenotebook.data.remote

sealed class OcrParseResult {
    data class Success(val result: OcrResult) : OcrParseResult()
    data object EmptyContent : OcrParseResult()
    data object MissingChoices : OcrParseResult()
    data class Failure(val message: String) : OcrParseResult()
}
