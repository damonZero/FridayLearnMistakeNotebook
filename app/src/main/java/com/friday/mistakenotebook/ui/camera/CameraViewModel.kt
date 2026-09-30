package com.friday.mistakenotebook.ui.camera

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.friday.mistakenotebook.data.remote.AiChatService
import com.friday.mistakenotebook.data.remote.OcrResult
import com.friday.mistakenotebook.data.remote.OcrService
import com.friday.mistakenotebook.util.ImageUtil
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject

data class CameraUiState(
    val capturedImageUri: Uri? = null,
    // 本次图片压缩存盘后的绝对路径（filesDir/images），失败为 null，不影响 OCR
    val imagePath: String? = null,
    val ocrResult: OcrResult? = null,
    // AI 识题整理出的参考答案（多解法）与学生作答，失败/缺失为空串
    val answer: String = "",
    val userAnswer: String = "",
    val isProcessing: Boolean = false,
    val error: String? = null,
    val isOcrComplete: Boolean = false,
    val ocrHint: String? = null
)

@HiltViewModel
class CameraViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val ocrService: OcrService,
    private val aiChatService: AiChatService,
    private val imageUtil: ImageUtil
) : ViewModel() {

    private val _uiState = MutableStateFlow(CameraUiState())
    val uiState: StateFlow<CameraUiState> = _uiState.asStateFlow()

    private var captureJob: Job? = null
    private var captureSeq = 0

    fun onImageCaptured(uri: Uri) {
        startCapture(uri)
    }

    fun onImageSelected(uri: Uri) {
        startCapture(uri)
    }

    /**
     * 拍照/选图共用入口：取消上一次未完成任务（防新旧图文串档），
     * 单次解码同时供存盘与 Base64（省一次整图解码），重活全部在 IO 线程。
     */
    private fun startCapture(uri: Uri) {
        captureSeq++
        val seq = captureSeq
        captureJob?.cancel()
        _uiState.update {
            it.copy(
                capturedImageUri = uri,
                imagePath = null,
                isProcessing = true,
                error = null,
                ocrHint = null,
                ocrResult = null,
                isOcrComplete = false
            )
        }
        captureJob = viewModelScope.launch {
            try {
                Log.d("OCR_CAMERA", "开始处理图片 URI: $uri")
                val bitmap = withContext(Dispatchers.IO) {
                    imageUtil.decodeForSave(context, uri, SAVE_MAX_DIMENSION)
                }
                if (seq != captureSeq) return@launch
                if (bitmap == null) {
                    Log.e("OCR_CAMERA", "图片解码失败")
                    _uiState.update {
                        it.copy(
                            error = "无法读取图片",
                            isProcessing = false,
                            ocrHint = "图片读取失败，请重新拍照或从相册选择"
                        )
                    }
                    return@launch
                }

                // 原图留存：存盘失败不打断 OCR 主流程
                val savedPath = withContext(Dispatchers.IO) {
                    val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
                    imageUtil.saveImageToLocal(context, bitmap, "question_$timestamp.jpg")
                }
                if (savedPath == null) {
                    Log.w("OCR_CAMERA", "原图存盘失败，本次错题将不附带图片")
                }

                val base64 = withContext(Dispatchers.IO) { imageUtil.bitmapToBase64(bitmap) }
                bitmap.recycle()
                if (seq != captureSeq) return@launch
                Log.d("OCR_CAMERA", "图片处理完成 base64=${base64.length} imagePath=$savedPath")

                // AI 识题优先：一次多模态调用完成"题干提取 + 学生作答提取 + 多解法参考答案"；
                // 失败回退纯 OCR（只填题干，答案留空手动输入）
                val extraction = aiChatService.analyzeQuestionImage(base64)
                if (seq != captureSeq) return@launch
                val result: OcrResult
                var answer = ""
                var userAnswer = ""
                if (extraction.isSuccess) {
                    val ex = extraction.getOrThrow()
                    Log.d("OCR_CAMERA", "AI 识题成功: content=${ex.content.take(50)}...")
                    result = OcrResult(text = ex.content, confidence = 0.95f)
                    answer = ex.answer
                    userAnswer = ex.userAnswer
                } else {
                    Log.w("OCR_CAMERA", "AI 识题失败，回退纯 OCR: ${extraction.exceptionOrNull()?.message}")
                    result = ocrService.recognizeText(base64)
                    if (seq != captureSeq) return@launch
                }
                Log.d("OCR_CAMERA", "识别完成: text=${result.text.take(50)}..., confidence=${result.confidence}")

                // imagePath/answer 与识别结果在同一次 update 落位，避免错配
                _uiState.update {
                    it.copy(
                        ocrResult = result,
                        imagePath = savedPath,
                        answer = answer,
                        userAnswer = userAnswer,
                        isProcessing = false,
                        isOcrComplete = true,
                        ocrHint = when {
                            result.confidence <= 0f && result.text.contains("为空") ->
                                "这次没有识别到文字。请检查图片清晰度、光线或裁剪范围。"
                            result.confidence <= 0f -> "识别结果异常。你可以重拍，或者直接手动输入。"
                            answer.isNotBlank() ->
                                "已识别题目并整理参考答案${if (userAnswer.isNotBlank()) "与学生作答" else ""}，返回录入页可核对修改。"
                            else -> "识别完成。可直接使用结果，或先检查内容再保存。"
                        }
                    )
                }
            } catch (e: Exception) {
                if (seq != captureSeq) return@launch
                Log.e("OCR_CAMERA", "拍照处理失败: ${e.message}", e)
                _uiState.update {
                    it.copy(
                        error = "处理失败: ${e.message}",
                        isProcessing = false,
                        ocrHint = "识别失败，请重试或手动输入。"
                    )
                }
            }
        }
    }

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }

    fun reset() {
        _uiState.update { CameraUiState() }
    }

    companion object {
        // 原图留存的采样目标边长，由 ImageUtil.decodeForSave 采样+EXIF 矫正+精确缩放
        private const val SAVE_MAX_DIMENSION = 1920
    }
}
