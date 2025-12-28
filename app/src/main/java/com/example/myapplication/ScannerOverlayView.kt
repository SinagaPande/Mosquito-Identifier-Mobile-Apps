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

    private val backgroundPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val eraserPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val boxLinePaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val cornerPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val glowPaint = Paint(Paint.ANTI_ALIAS_FLAG)

    private val scanRect = RectF()
    private val cornerPath = Path()

    // --- KONFIGURASI TAMPILAN TERBARU ---
    private val cornerRadius = 40f

    // REVISI: Siku diperpanjang agar framing lebih jelas
    private val cornerLength = 120f
    // ---------------------------

    init {
        backgroundPaint.color = ContextCompat.getColor(context, R.color.overlay_dark)
        backgroundPaint.style = Paint.Style.FILL

        eraserPaint.xfermode = PorterDuffXfermode(PorterDuff.Mode.CLEAR)

        // 1. Garis Penghubung (Tipis, Hijau Neon)
        boxLinePaint.style = Paint.Style.STROKE
        boxLinePaint.color = ContextCompat.getColor(context, R.color.neon_green)
        boxLinePaint.strokeWidth = 3f

        // 2. Sudut Siku (Tebal, Hijau Neon Terang)
        cornerPaint.style = Paint.Style.STROKE
        cornerPaint.color = ContextCompat.getColor(context, R.color.neon_green)
        cornerPaint.strokeWidth = 12f
        cornerPaint.strokeCap = Paint.Cap.ROUND
        cornerPaint.strokeJoin = Paint.Join.ROUND

        // 3. Glow di balik siku
        glowPaint.style = Paint.Style.STROKE
        glowPaint.color = ContextCompat.getColor(context, R.color.neon_glow_strong)
        glowPaint.strokeWidth = 22f
        glowPaint.strokeCap = Paint.Cap.ROUND
        glowPaint.strokeJoin = Paint.Join.ROUND
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        calculateRect(w, h)
    }

    private fun calculateRect(width: Int, height: Int) {
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
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), backgroundPaint)
        canvas.drawRoundRect(scanRect, cornerRadius, cornerRadius, eraserPaint)
        canvas.restoreToCount(sc)
        canvas.drawRoundRect(scanRect, cornerRadius, cornerRadius, boxLinePaint)
        drawNeonCorners(canvas)
    }

    private fun drawNeonCorners(canvas: Canvas) {
        cornerPath.reset()
        val r = scanRect
        val cl = cornerLength

        cornerPath.moveTo(r.left, r.top + cl)
        cornerPath.lineTo(r.left, r.top)
        cornerPath.lineTo(r.left + cl, r.top)

        cornerPath.moveTo(r.right - cl, r.top)
        cornerPath.lineTo(r.right, r.top)
        cornerPath.lineTo(r.right, r.top + cl)

        cornerPath.moveTo(r.right, r.bottom - cl)
        cornerPath.lineTo(r.right, r.bottom)
        cornerPath.lineTo(r.right - cl, r.bottom)

        cornerPath.moveTo(r.left + cl, r.bottom)
        cornerPath.lineTo(r.left, r.bottom)
        cornerPath.lineTo(r.left, r.bottom - cl)

        canvas.drawPath(cornerPath, glowPaint)
        canvas.drawPath(cornerPath, cornerPaint)
    }
}