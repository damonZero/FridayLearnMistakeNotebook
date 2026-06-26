package com.friday.mistakenotebook.ui.camera

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.friday.mistakenotebook.data.remote.OcrResult
import com.friday.mistakenotebook.data.remote.OcrService
import com.friday.mistakenotebook.util.ImageUtil
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject

data class CameraUiState(
    val capturedImageUri: Uri? = null,
    val capturedBitmap: Bitmap? = null,
    val ocrResult: OcrResult? = null,
    val isProcessing: Boolean = false,
    val error: String? = null,
    val isOcrComplete: Boolean = false
)

@HiltViewModel
class CameraViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val ocrService: OcrService,
    private val imageUtil: ImageUtil
) : ViewModel() {

    private val _uiState = MutableStateFlow(CameraUiState())
    val uiState: StateFlow<CameraUiState> = _uiState.asStateFlow()

    /**
     * 处理拍照结果
     */
    fun onImageCaptured(uri: Uri) {
        _uiState.update { it.copy(capturedImageUri = uri, isProcessing = true) }

        viewModelScope.launch {
            try {
                // 加载并压缩图片
                val bitmap = imageUtil.loadLocalImage(uri.path ?: return@launch)
                if (bitmap == null) {
                    _uiState.update { it.copy(error = "无法加载图片", isProcessing = false) }
                    return@launch
                }

                val compressedBitmap = imageUtil.compressBitmap(bitmap)
                _uiState.update { it.copy(capturedBitmap = compressedBitmap) }

                // 进行 OCR 识别
                val base64 = imageUtil.bitmapToBase64(compressedBitmap)
                val result = ocrService.recognizeText(base64)

                _uiState.update {
                    it.copy(
                        ocrResult = result,
                        isProcessing = false,
                        isOcrComplete = true
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        error = "处理失败: ${e.message}",
                        isProcessing = false
                    )
                }
            }
        }
    }

    /**
     * 处理从相册选择的图片
     */
    fun onImageSelected(uri: Uri) {
        _uiState.update { it.copy(capturedImageUri = uri, isProcessing = true) }

        viewModelScope.launch {
            try {
                val base64 = imageUtil.uriToBase64(context, uri)
                if (base64 == null) {
                    _uiState.update { it.copy(error = "无法读取图片", isProcessing = false) }
                    return@launch
                }

                val result = ocrService.recognizeText(base64)
                _uiState.update {
                    it.copy(
                        ocrResult = result,
                        isProcessing = false,
                        isOcrComplete = true
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        error = "识别失败: ${e.message}",
                        isProcessing = false
                    )
                }
            }
        }
    }

    /**
     * 保存图片到本地
     */
    fun saveImageToLocal(): String? {
        val bitmap = _uiState.value.capturedBitmap ?: return null
        val filename = "question_${System.currentTimeMillis()}.jpg"
        return imageUtil.saveImageToLocal(context, bitmap, filename)
    }

    /**
     * 清除错误信息
     */
    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }

    /**
     * 重置状态
     */
    fun reset() {
        _uiState.update { CameraUiState() }
    }
}
