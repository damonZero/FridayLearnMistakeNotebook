package com.friday.mistakenotebook.print

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import android.net.Uri
import android.os.CancellationSignal
import android.os.ParcelFileDescriptor
import android.print.PageRange
import android.print.PrintAttributes
import android.print.PrintDocumentAdapter
import android.print.PrintDocumentInfo
import android.print.PrintManager
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import androidx.core.content.FileProvider
import com.friday.mistakenotebook.data.remote.GeneratedQuestion
import com.friday.mistakenotebook.domain.model.Question
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

/** 一道待打印的题：错题本体 + 可选的举一反三 */
data class SheetItem(
    val question: Question,
    val similar: List<GeneratedQuestion> = emptyList()
)

/** 生成结果：练习卷（孩子）+ 答案卷（家长） */
data class SheetFiles(val exerciseSheet: File, val answerSheet: File)

/** 练习卷生成流程状态（列表批量与详情单题共用） */
sealed interface SheetGenerateState {
    data object Idle : SheetGenerateState
    data class Generating(val progress: String) : SheetGenerateState
    data class Ready(val files: SheetFiles, val note: String? = null) : SheetGenerateState
    data class Failed(val message: String) : SheetGenerateState
}

/**
 * 纸质练习卷生成器：A4 PDF，练习卷与答案卷分离（防泄题）。
 * 使用系统 PdfDocument + StaticLayout 排版，零第三方依赖。
 */
@Singleton
class PracticeSheetPdfGenerator @Inject constructor(
    @ApplicationContext private val context: Context
) {

    companion object {
        private const val PAGE_W = 595   // A4 595×842 pt
        private const val PAGE_H = 842
        private const val MARGIN = 42f
        private const val CONTENT_W = PAGE_W - 2 * MARGIN
        private val BLACK = Color.rgb(33, 33, 33)
        private val GRAY = Color.rgb(150, 150, 150)
        private val LINE = Color.rgb(200, 200, 200)
        private val ACCENT = Color.rgb(230, 90, 90)

        private fun bodyPaint(): TextPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            textSize = 11f; color = BLACK; typeface = normalTypeface()
        }

        private fun titlePaint(size: Float): TextPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            textSize = size; color = BLACK; typeface = boldTypeface()
        }

        private fun grayPaint(size: Float): TextPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            textSize = size; color = GRAY; typeface = normalTypeface()
        }

        private fun normalTypeface() = android.graphics.Typeface.create("sans-serif", android.graphics.Typeface.NORMAL)
        private fun boldTypeface() = android.graphics.Typeface.create("sans-serif", android.graphics.Typeface.BOLD)
    }

    /** 分页排版写手：管理当前页与光标位置，自动换页（页脚每页绘制） */
    private inner class Writer(private val doc: PdfDocument, private val footer: (Int) -> String) {
        private var page: PdfDocument.Page? = null
        private var canvas: Canvas? = null
        var pageNum = 0
            private set
        var y = MARGIN
            private set

        fun startPage() {
            page?.let { doc.finishPage(it) }
            pageNum++
            page = doc.startPage(PdfDocument.PageInfo.Builder(PAGE_W, PAGE_H, pageNum).create())
            canvas = page!!.canvas
            y = MARGIN
            // 页脚每页都画：内容区最多到 PAGE_H-MARGIN，不会与页脚重叠
            val c = canvas!!
            c.drawText(footer(pageNum), MARGIN, PAGE_H - MARGIN / 2, grayPaint(9f))
            c.drawText("第 $pageNum 页", PAGE_W - MARGIN - 60f, PAGE_H - MARGIN / 2, grayPaint(9f))
        }

        fun endPage() {
            page?.let { doc.finishPage(it) }
            page = null
        }

        fun ensure(needed: Float) {
            if (y + needed > PAGE_H - MARGIN) startPage()
        }

        /** 分页绘制 StaticLayout：断点按行界对齐，跨页不截断行、不重复绘制 */
        fun drawLayout(layout: StaticLayout) {
            var line = 0
            val lineCount = layout.lineCount
            while (line < lineCount) {
                val lineHeight = (layout.getLineBottom(line) - layout.getLineTop(line) + 2f)
                ensure(lineHeight)
                val remaining = PAGE_H - MARGIN - y
                var last = line
                while (last < lineCount &&
                    layout.getLineBottom(last) - layout.getLineTop(line) <= remaining
                ) {
                    last++
                }
                if (last == line) last = line + 1 // 单行超高也强制画出
                val top = layout.getLineTop(line)
                val bottom = layout.getLineBottom(last - 1)
                val c = canvas!!
                c.save()
                c.translate(MARGIN, y)
                c.clipRect(0f, 0f, CONTENT_W, (bottom - top).toFloat())
                c.translate(0f, -top.toFloat())
                layout.draw(c)
                c.restore()
                y += bottom - top
                line = last
                if (line < lineCount) startPage()
            }
        }

        fun drawText(text: String, paint: TextPaint, spacingAfter: Float = 6f) {
            if (text.isBlank()) return
            val layout = StaticLayout.Builder
                .obtain(text, 0, text.length, paint, CONTENT_W.toInt())
                .setAlignment(Layout.Alignment.ALIGN_NORMAL)
                .setLineSpacing(3f, 1.15f)
                .build()
            drawLayout(layout)
            y += spacingAfter
        }

        /** 作答留白：试卷风格，纯空白不画线 */
        fun drawBlankArea(height: Float = 110f) {
            ensure(height)
            y += height + 10f
        }

        /** 嵌入题目原图（等比缩放，超高则缩到一页内） */
        fun drawImage(file: File, maxContentHeight: Float = 420f) {
            val bitmap = decodeSampled(file, CONTENT_W.toInt()) ?: return
            try {
                val ratio = CONTENT_W / bitmap.width
                var drawH = bitmap.height * ratio
                var drawW = CONTENT_W.toFloat()
                if (drawH > maxContentHeight) {
                    val shrink = maxContentHeight / drawH
                    drawH = maxContentHeight
                    drawW = bitmap.width * shrink
                }
                ensure(drawH + 8f)
                val left = MARGIN + (CONTENT_W - drawW) / 2f
                canvas!!.drawBitmap(bitmap, null, android.graphics.RectF(left, y, left + drawW, y + drawH), null)
                y += drawH + 10f
            } finally {
                bitmap.recycle()
            }
        }

        private fun decodeSampled(file: File, targetWidth: Int): Bitmap? {
            return try {
                val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                BitmapFactory.decodeFile(file.absolutePath, bounds)
                if (bounds.outWidth <= 0) return null
                var sample = 1
                while (bounds.outWidth / (sample * 2) >= targetWidth) sample *= 2
                BitmapFactory.decodeFile(file.absolutePath, BitmapFactory.Options().apply { inSampleSize = sample })
            } catch (e: Exception) {
                null
            }
        }

        fun drawDivider() {
            ensure(10f)
            val p = Paint().apply { strokeWidth = 1.2f; color = ACCENT }
            canvas!!.drawLine(MARGIN, y, PAGE_W - MARGIN, y, p)
            y += 10f
        }

        fun finish() = endPage()
    }

    private val mutex = kotlinx.coroutines.sync.Mutex()

    /**
     * 生成练习卷与答案卷。
     * @param includeSimilar 是否把每题的举一反三印上去（批量默认关）
     */
    suspend fun generate(
        items: List<SheetItem>,
        includeSimilar: Boolean,
        title: String
    ): SheetFiles = withContext(Dispatchers.IO) {
        mutex.withLock {
            val dir = File(context.filesDir, "exports").apply { mkdirs() }
            val stamp = SimpleDateFormat("yyyyMMdd_HHmm", Locale.getDefault()).format(Date())
            val exercise = uniqueFile(dir, "周周练习卷_$stamp")
            val answer = uniqueFile(dir, "周周答案卷_$stamp")

            generateExercise(exercise, items, includeSimilar, title)
            generateAnswer(answer, items, includeSimilar, title)
            SheetFiles(exercise, answer)
        }
    }

    /** 同一分钟内多次生成时追加序号，避免覆写仍在分享中的文件 */
    private fun uniqueFile(dir: File, base: String): File {
        var f = File(dir, "$base.pdf")
        var i = 1
        while (f.exists()) {
            f = File(dir, "${base}_$i.pdf")
            i++
        }
        return f
    }

    // ---------- 练习卷 ----------

    private fun generateExercise(out: File, items: List<SheetItem>, includeSimilar: Boolean, title: String) {
        val doc = PdfDocument()
        val writer = Writer(doc) { page ->
            "完成时间：__________    家长签名：__________"
        }
        writer.startPage()
        writer.drawText(title, titlePaint(18f), 2f)
        writer.drawText(
            "姓名：__________  日期：${SimpleDateFormat("yyyy年M月d日", Locale.getDefault()).format(Date())}",
            grayPaint(10f), 8f
        )
        writer.drawDivider()

        items.forEachIndexed { index, item ->
            val q = item.question
            writer.drawText("第 ${index + 1} 题（原错题）", titlePaint(13f), 4f)
            writer.drawText(q.content, bodyPaint(), 8f)
            q.imagePath?.let { path ->
                val img = File(path)
                if (img.exists()) writer.drawImage(img)
            }
            writer.drawBlankArea(120f)

            if (includeSimilar) {
                item.similar.forEachIndexed { sIdx, s ->
                    writer.drawText(
                        "第 ${index + 1}.${sIdx + 1} 题（举一反三）",
                        titlePaint(13f), 4f
                    )
                    writer.drawText(s.content, bodyPaint(), 8f)
                    writer.drawBlankArea(120f)
                }
            }
            writer.drawDivider()
        }

        writer.finish()
        try {
            FileOutputStream(out).use { doc.writeTo(it) }
        } finally {
            doc.close()
        }
    }

    // ---------- 答案卷 ----------

    private fun generateAnswer(out: File, items: List<SheetItem>, includeSimilar: Boolean, title: String) {
        val doc = PdfDocument()
        val writer = Writer(doc) { "答案卷 · 家长留存" }
        writer.startPage()
        writer.drawText("答案卷（家长留存，勿发给孩子）", titlePaint(16f), 2f)
        writer.drawText(title, grayPaint(11f), 8f)
        writer.drawDivider()

        items.forEachIndexed { index, item ->
            val q = item.question
            writer.drawText("第 ${index + 1} 题（原错题）", titlePaint(13f), 4f)
            writer.drawText(q.content, bodyPaint(), 6f)
            writer.drawText("正确答案：${q.answer.ifBlank { "（未填写）" }}", bodyPaint(), 6f)

            if (includeSimilar) {
                item.similar.forEachIndexed { sIdx, s ->
                    writer.drawText(
                        "第 ${index + 1}.${sIdx + 1} 题（举一反三）答案：${s.answer.ifBlank { "（未填写）" }}",
                        bodyPaint(), 8f
                    )
                }
            }

            // AI 分析跟在答案区一起，方便家长对照讲解
            q.aiAnalysis?.takeIf { it.isNotBlank() }?.let {
                writer.drawText("AI 分析：$it", bodyPaint(), 6f)
            }
            writer.drawText("孩子的记录：上一次作答 —— ${q.userAnswer.ifBlank { "（无记录）" }}", grayPaint(10f), 8f)

            writer.drawDivider()
        }

        // 家长记录栏
        writer.drawText("本次练习记录（勾选后在 App 里更新对应题目的状态）", titlePaint(13f), 6f)
        writer.drawText(
            "☐ 全部做对    ☐ 错 1~2 题    ☐ 多数不会    ☐ 未完成",
            bodyPaint(), 8f
        )
        writer.drawText("错因备注：________________________________________", bodyPaint(), 6f)

        writer.finish()
        try {
            FileOutputStream(out).use { doc.writeTo(it) }
        } finally {
            doc.close()
        }
    }
}

/**
 * 单文件分享到系统分享面板。
 * 注意：微信对 SEND_MULTIPLE 只接收图片/视频，PDF 多文件分享面板里不会出现微信；
 * 单文件 ACTION_SEND 走微信的文件通道，可以正常分享
 */
fun shareSheetFile(context: Context, file: File) {
    val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "application/pdf"
        putExtra(Intent.EXTRA_STREAM, uri)
        clipData = android.content.ClipData.newRawUri("sheet", uri)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    context.startActivity(Intent.createChooser(intent, "分享「${file.name}」"))
}

/** 系统打印服务直印练习卷（需已配置打印机） */
fun printExerciseSheet(context: Context, file: File) {
    val printManager = context.getSystemService(Context.PRINT_SERVICE) as PrintManager
    val jobName = file.nameWithoutExtension
    val adapter = object : PrintDocumentAdapter() {
        override fun onLayout(
            oldAttributes: PrintAttributes?,
            newAttributes: PrintAttributes?,
            cancellationSignal: CancellationSignal?,
            callback: LayoutResultCallback?,
            extras: android.os.Bundle?
        ) {
            if (cancellationSignal?.isCanceled == true) {
                callback?.onLayoutCancelled()
                return
            }
            val info = PrintDocumentInfo.Builder(file.name)
                .setContentType(PrintDocumentInfo.CONTENT_TYPE_DOCUMENT)
                .build()
            callback?.onLayoutFinished(info, newAttributes != oldAttributes)
        }

        override fun onWrite(
            pages: Array<out PageRange>?,
            destination: ParcelFileDescriptor?,
            cancellationSignal: CancellationSignal?,
            callback: WriteResultCallback?
        ) {
            try {
                FileInputStream(file).use { input ->
                    ParcelFileDescriptor.AutoCloseOutputStream(destination).use { output ->
                        input.copyTo(output)
                    }
                }
                if (cancellationSignal?.isCanceled == true) {
                    callback?.onWriteCancelled()
                } else {
                    callback?.onWriteFinished(arrayOf(PageRange.ALL_PAGES))
                }
            } catch (e: Exception) {
                callback?.onWriteFailed(e.message)
            }
        }
    }
    printManager.print(
        jobName,
        adapter,
        PrintAttributes.Builder().setMediaSize(PrintAttributes.MediaSize.ISO_A4).build()
    )
}
