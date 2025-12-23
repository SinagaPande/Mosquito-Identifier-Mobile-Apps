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

    // Ukuran area scan (dihitung dinamis)
    private val scanRect = RectF()

    // --- KONFIGURASI TAMPILAN ---
    // Persentase ukuran kotak relatif terhadap lebar layar (misal 0.7 = 70%)
    private val scanSizePercent = 0.7f
    // Kelengkungan sudut area transparan (agar tidak terlalu tajam)
    private val cornerRadius = 50f
    // Ketebalan garis sudut "L"
    private val strokeWidth = 15f
    // Panjang lengan garis sudut "L"
    private val cornerLength = 60f
    // ---------------------------

    // Path object untuk menggambar sudut (di-reuse agar performa lebih baik)
    private val cornerPath = Path()

    init {
        // Setup Paint untuk area gelap (Overlay latar belakang)
        // Menggunakan warna semi-transparan yang sudah didefinisikan di colors.xml
        paint.color = ContextCompat.getColor(context, R.color.overlay_dark)
        paint.style = Paint.Style.FILL

        // Setup Paint untuk indikator sudut "L" (Border)
        borderPaint.color = ContextCompat.getColor(context, R.color.scan_border)
        borderPaint.style = Paint.Style.STROKE
        borderPaint.strokeWidth = strokeWidth
        // Membuat sudut dan ujung garis menjadi melengkung (rounded) agar terlihat modern
        borderPaint.strokeJoin = Paint.Join.ROUND
        borderPaint.strokeCap = Paint.Cap.ROUND

        // Setup Eraser (Untuk membuat lubang transparan di tengah)
        eraserPaint.xfermode = PorterDuffXfermode(PorterDuff.Mode.CLEAR)
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        // Hitung ulang posisi dan ukuran kotak saat ukuran layar berubah
        calculateRect(w, h)
    }

    private fun calculateRect(width: Int, height: Int) {
        // Hitung ukuran sisi kotak persegi berdasarkan persentase lebar layar
        // Menggunakan min(width, height) memastikan kotak tetap masuk akal saat orientasi landscape
        val overlaySize = kotlin.math.min(width, height) * scanSizePercent

        // Cari titik tengah layar
        val centerX = width / 2f
        val centerY = height / 2f

        // Set koordinat batas kotak scan (Kiri, Atas, Kanan, Bawah) berada tepat di tengah
        scanRect.set(
            centerX - (overlaySize / 2),
            centerY - (overlaySize / 2),
            centerX + (overlaySize / 2),
            centerY + (overlaySize / 2)
        )
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        // 1. Buat Layer baru untuk menampung efek "penghapus"
        val sc = canvas.saveLayer(0f, 0f, width.toFloat(), height.toFloat(), null)

        // 2. Gambar background gelap transparan memenuhi layar
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), paint)

        // 3. "Hapus" bagian tengah area scan agar menjadi transparan (bolong)
        // Menggunakan drawRoundRect agar lubangnya memiliki sudut yang sedikit melengkung
        canvas.drawRoundRect(scanRect, cornerRadius, cornerRadius, eraserPaint)

        // 4. Terapkan perubahan layer (background gelap + lubang tengah)
        canvas.restoreToCount(sc)

        // 5. Gambar indikator sudut "L" di keempat pojok area scan
        drawCornerIndicators(canvas)
    }

    private fun drawCornerIndicators(canvas: Canvas) {
        // Reset path sebelum menggambar ulang
        cornerPath.reset()

        val r = scanRect
        val cl = cornerLength

        // Kiri Atas (Top Left)
        cornerPath.moveTo(r.left, r.top + cl)     // Mulai dari bawah sedikit di sisi kiri
        cornerPath.lineTo(r.left, r.top)          // Tarik garis ke atas (pojok)
        cornerPath.lineTo(r.left + cl, r.top)     // Tarik garis ke kanan

        // Kanan Atas (Top Right)
        cornerPath.moveTo(r.right - cl, r.top)    // Mulai dari kiri sedikit di sisi atas
        cornerPath.lineTo(r.right, r.top)         // Tarik garis ke kanan (pojok)
        cornerPath.lineTo(r.right, r.top + cl)    // Tarik garis ke bawah

        // Kanan Bawah (Bottom Right)
        cornerPath.moveTo(r.right, r.bottom - cl) // Mulai dari atas sedikit di sisi kanan
        cornerPath.lineTo(r.right, r.bottom)      // Tarik garis ke bawah (pojok)
        cornerPath.lineTo(r.right - cl, r.bottom) // Tarik garis ke kiri

        // Kiri Bawah (Bottom Left)
        cornerPath.moveTo(r.left + cl, r.bottom)  // Mulai dari kanan sedikit di sisi bawah
        cornerPath.lineTo(r.left, r.bottom)       // Tarik garis ke kiri (pojok)
        cornerPath.lineTo(r.left, r.bottom - cl)  // Tarik garis ke atas

        // Gambar jalur (path) yang sudah dibuat menggunakan borderPaint
        canvas.drawPath(cornerPath, borderPaint)
    }
}