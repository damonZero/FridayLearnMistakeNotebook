package com.friday.mistakenotebook.ui.camera

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.friday.mistakenotebook.data.remote.OcrResult
import com.friday.mistakenotebook.data.remote.OcrService
import com.friday.mistakenotebook.util.ImageUtil
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject

data class CameraUiState(
    val capturedImageUri: Uri? = null,
    // 本次图片压缩存盘后的绝对路径（filesDir/images），失败为 null，不影响 OCR
    val imagePath: String? = null,
    val ocrResult: OcrResult? = null,
    val isProcessing: Boolean = false,
    val error: String? = null,
    val isOcrComplete: Boolean = false,
    val ocrHint: String? = null
)

@HiltViewModel
class CameraViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val ocrService: OcrService,
    private val imageUtil: ImageUtil
) : ViewModel() {

    private val _uiState = MutableStateFlow(CameraUiState())
    val uiState: StateFlow<CameraUiState> = _uiState.asStateFlow()

    fun onImageCaptured(uri: Uri) {
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

        viewModelScope.launch {
            try {
                Log.d("OCR_CAMERA", "开始处理图片 URI: $uri")
                val base64 = imageUtil.uriToBase64(context, uri)
                if (base64 == null) {
                    Log.e("OCR_CAMERA", "uriToBase64 返回 null，图片加载失败")
                    _uiState.update { it.copy(error = "无法加载图片", isProcessing = false, ocrHint = "图片读取失败，请重新拍照或从相册选择") }
                    return@launch
                }
                Log.d("OCR_CAMERA", "图片转 Base64 成功，长度: ${base64.length}")

                saveCapturedImageSafely(uri)

                Log.d("OCR_CAMERA", "开始调用 OCR 服务...")
                val result = ocrService.recognizeText(base64)
                Log.d("OCR_CAMERA", "OCR 识别完成: text=${result.text.take(50)}..., confidence=${result.confidence}")

                _uiState.update {
                    it.copy(
                        ocrResult = result,
                        isProcessing = false,
                        isOcrComplete = true,
                        ocrHint = when {
                            result.confidence <= 0f && result.text.contains("为空") -> "这次没有识别到文字。请检查图片清晰度、光线或裁剪范围。"
                            result.confidence <= 0f -> "识别结果异常。你可以重拍，或者直接手动输入。"
                            else -> "识别完成。可直接使用结果，或先检查内容再保存。"
                        }
                    )
                }
            } catch (e: Exception) {
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

    fun onImageSelected(uri: Uri) {
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

        viewModelScope.launch {
            try {
                Log.d("OCR_CAMERA", "开始处理相册图片 URI: $uri")
                val base64 = imageUtil.uriToBase64(context, uri)
                if (base64 == null) {
                    Log.e("OCR_CAMERA", "uriToBase64 返回 null，图片读取失败")
                    _uiState.update { it.copy(error = "无法读取图片", isProcessing = false, ocrHint = "图片读取失败，请重新选择或更换图片。") }
                    return@launch
                }
                Log.d("OCR_CAMERA", "图片转 Base64 成功，长度: ${base64.length}")

                saveCapturedImageSafely(uri)

                Log.d("OCR_CAMERA", "开始调用 OCR 服务...")
                val result = ocrService.recognizeText(base64)
                Log.d("OCR_CAMERA", "OCR 识别完成: text=${result.text.take(50)}..., confidence=${result.confidence}")
                _uiState.update {
                    it.copy(
                        ocrResult = result,
                        isProcessing = false,
                        isOcrComplete = true,
                        ocrHint = when {
                            result.confidence <= 0f && result.text.contains("为空") -> "这次没有识别到文字。请换一张更清晰的图片。"
                            result.confidence <= 0f -> "识别结果异常。请重试或手动输入。"
                            else -> "识别完成。可直接使用结果，或先检查内容再保存。"
                        }
                    )
                }
            } catch (e: Exception) {
                Log.e("OCR_CAMERA", "相册图片识别失败: ${e.message}", e)
                _uiState.update {
                    it.copy(
                        error = "识别失败: ${e.message}",
                        isProcessing = false,
                        ocrHint = "识别失败，请重试或手动输入。"
                    )
                }
            }
        }
    }

    /**
     * 原图留存：压缩后写入 filesDir/images，路径进 UiState 供保存错题时落库。
     * 任何一步失败都只告警，不打断 OCR 主流程（imagePath 保持 null）。
     */
    private suspend fun saveCapturedImageSafely(uri: Uri) {
        val savedPath = withContext(Dispatchers.IO) { saveImageToLocal(uri) }
        if (savedPath == null) {
            Log.w("OCR_CAMERA", "原图存盘失败，本次错题将不附带图片")
        } else {
            _uiState.update { it.copy(imagePath = savedPath) }
        }
    }

    private fun saveImageToLocal(uri: Uri): String? {
        return try {
            // decodeForSave 内含采样 + EXIF 方向矫正 + 缩放，竖拍照片不会横着存盘
            val bitmap = imageUtil.decodeForSave(context, uri, SAVE_MAX_DIMENSION) ?: return null
            val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
            imageUtil.saveImageToLocal(context, bitmap, "question_$timestamp.jpg")
        } catch (e: Exception) {
            Log.w("OCR_CAMERA", "原图压缩存盘失败: ${e.message}")
            null
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
