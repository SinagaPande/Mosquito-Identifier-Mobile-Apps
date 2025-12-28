package com.example.myapplication

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.View
import androidx.core.content.ContextCompat
import com.example.myapplication.R

class ScannerOverlayView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val eraserPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val scanRect = RectF()

    // --- KONFIGURASI TAMPILAN ---
    // Corner radius sedikit diperkecil agar cocok dengan kotak kecil
    private val cornerRadius = 40f
    private val strokeWidth = 12f // Garis sedikit lebih tipis agar elegan
    private val cornerLength = 50f
    // ---------------------------

    private val cornerPath = Path()

    init {
        paint.color = ContextCompat.getColor(context, R.color.overlay_dark)
        paint.style = Paint.Style.FILL

        borderPaint.color = ContextCompat.getColor(context, R.color.scan_border)
        borderPaint.style = Paint.Style.STROKE
        borderPaint.strokeWidth = strokeWidth
        borderPaint.strokeJoin = Paint.Join.ROUND
        borderPaint.strokeCap = Paint.Cap.ROUND

        eraserPaint.xfermode = PorterDuffXfermode(PorterDuff.Mode.CLEAR)
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        calculateRect(w, h)
    }

    private fun calculateRect(width: Int, height: Int) {
        // --- REVISI UI: KEMBALI KE KOTAK PERSEGI (SQUARE) ---
        // Alasan: Fokus untuk objek kecil (nyamuk), bukan dokumen kertas.

        // Ukuran: 65% dari lebar layar (Cukup fokus, tidak terlalu besar)
        val boxSideLength = width * 0.65f

        val centerX = width / 2f
        val centerY = height / 2f

        scanRect.set(
            centerX - (boxSideLength / 2),
            centerY - (boxSideLength / 2),
            centerX + (boxSideLength / 2),
            centerY + (boxSideLength / 2)
        )
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val sc = canvas.saveLayer(0f, 0f, width.toFloat(), height.toFloat(), null)
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), paint)
        canvas.drawRoundRect(scanRect, cornerRadius, cornerRadius, eraserPaint)
        canvas.restoreToCount(sc)
        drawCornerIndicators(canvas)
    }

    private fun drawCornerIndicators(canvas: Canvas) {
        cornerPath.reset()
        val r = scanRect
        val cl = cornerLength

        // Kiri Atas
        cornerPath.moveTo(r.left, r.top + cl)
        cornerPath.lineTo(r.left, r.top)
        cornerPath.lineTo(r.left + cl, r.top)

        // Kanan Atas
        cornerPath.moveTo(r.right - cl, r.top)
        cornerPath.lineTo(r.right, r.top)
        cornerPath.lineTo(r.right, r.top + cl)

        // Kanan Bawah
        cornerPath.moveTo(r.right, r.bottom - cl)
        cornerPath.lineTo(r.right, r.bottom)
        cornerPath.lineTo(r.right - cl, r.bottom)

        // Kiri Bawah
        cornerPath.moveTo(r.left + cl, r.bottom)
        cornerPath.lineTo(r.left, r.bottom)
        cornerPath.lineTo(r.left, r.bottom - cl)

        canvas.drawPath(cornerPath, borderPaint)
    }
}