package com.example.myapplication

import android.net.Uri
import android.os.Bundle
import android.widget.Button
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
// Hapus import androidx.core.net.toUri jika ada, agar lebih aman

class ResultActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_IMAGE_URI = "extra_image_uri"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_result)

        // Inisialisasi View
        val imageView = findViewById<ImageView>(R.id.resultImageView)

        // Cek versi Android untuk kliping outline (API 21+)
        imageView.clipToOutline = true

        // Ganti <Button> menjadi <View> atau <LinearLayout> agar cocok dengan XML baru
        val btnRetake = findViewById<android.view.View>(R.id.btnRetake)
        val tvSpecies = findViewById<TextView>(R.id.speciesNameText)
        val tvAccuracy = findViewById<TextView>(R.id.accuracyText)
        val progressBar = findViewById<ProgressBar>(R.id.accuracyBar)
        val tvDescription = findViewById<TextView>(R.id.descriptionText)

        // 1. Ambil Data URI dari Intent
        val imageUriString = intent.getStringExtra(EXTRA_IMAGE_URI)

        if (!imageUriString.isNullOrEmpty()) {
            try {
                // KEMBALI KE CARA STANDAR (Lebih Aman dari Crash KTX)
                val imageUri = Uri.parse(imageUriString)
                imageView.setImageURI(imageUri)
            } catch (e: Exception) {
                e.printStackTrace()
                Toast.makeText(this, getString(R.string.error_load_image), Toast.LENGTH_SHORT).show()
            }
        } else {
            Toast.makeText(this, getString(R.string.error_load_image), Toast.LENGTH_SHORT).show()
            // Jangan finish() di sini agar user masih bisa melihat UI meski gambar gagal,
            // atau finish() jika Anda ingin langsung menutup.
            finish()
        }

        // --- DUMMY DATA ---
        val dummyAccuracy = 70

        // Menggunakan String Resource agar tidak warning
        tvSpecies.text = getString(R.string.species_aedes_aegypti)

        // Format string akurasi
        tvAccuracy.text = getString(R.string.accuracy_format, dummyAccuracy)

        progressBar.progress = dummyAccuracy

        // Deskripsi dari resource
        tvDescription.text = getString(R.string.desc_aedes_aegypti)

        // 3. Tombol Ambil Foto Lain
        btnRetake.setOnClickListener {
            // Menutup activity ini dan kembali ke CameraFragment
            finish()
        }
    }
}