package com.example.myapplication

import android.net.Uri
import android.os.Bundle
import android.widget.Button
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class ResultActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_IMAGE_URI = "extra_image_uri"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_result)

        val imageView = findViewById<ImageView>(R.id.resultImageView)
        val backButton = findViewById<Button>(R.id.backButton)

        // 1. Ambil Data URI dari Intent
        val imageUriString = intent.getStringExtra(EXTRA_IMAGE_URI)

        if (imageUriString != null) {
            val imageUri = Uri.parse(imageUriString)
            // 2. Tampilkan Gambar
            imageView.setImageURI(imageUri)
        } else {
            Toast.makeText(this, "Gagal memuat gambar", Toast.LENGTH_SHORT).show()
            finish() // Kembali jika tidak ada gambar
        }

        // 3. Tombol Kembali
        backButton.setOnClickListener {
            finish() // Menutup activity ini dan kembali ke layar sebelumnya (Kamera/Home)
        }
        
        // Di sini nanti logika AI akan ditempatkan untuk mengganti teks dummy
    }
}