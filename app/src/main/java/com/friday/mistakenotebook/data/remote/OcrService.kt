package com.friday.mistakenotebook.data.remote

/**
 * OCR 识别结果
 */
data class OcrResult(
    val text: String,
    val confidence: Float,
    val blocks: List<TextBlock> = emptyList()
)

data class TextBlock(
    val text: String,
    val confidence: Float,
    val x: Int = 0,
    val y: Int = 0,
    val width: Int = 0,
    val height: Int = 0
)

/**
 * OCR 服务接口
 */
interface OcrService {
    suspend fun recognizeText(imageBase64: String): OcrResult
}
