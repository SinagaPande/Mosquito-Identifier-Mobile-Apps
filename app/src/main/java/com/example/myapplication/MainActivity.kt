package com.example.myapplication

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.os.Bundle
import android.provider.MediaStore
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import androidx.viewpager2.widget.ViewPager2

class MainActivity : AppCompatActivity() {

    companion object {
        private const val PERMISSION_GRANTED_MESSAGE = "Kamera siap digunakan"
        private const val PERMISSION_DENIED_MESSAGE = "Izin kamera diperlukan untuk menggunakan kamera"
        private const val CAMERA_PERMISSION_TITLE = "Izin Kamera Diperlukan"
        private const val CAMERA_PERMISSION_MESSAGE = "Aplikasi memerlukan akses kamera untuk mengambil foto."
        private const val OK_BUTTON = "OK"
        private const val CANCEL_BUTTON = "Batal"
    }

    lateinit var viewPager: ViewPager2

    private val cameraPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (!isGranted) {
            showToast(PERMISSION_DENIED_MESSAGE, Toast.LENGTH_LONG)
        }
    }

    private val galleryLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            val selectedUri = result.data?.data
            if (selectedUri != null) {
                val intent = Intent(this, ResultActivity::class.java)
                intent.putExtra(ResultActivity.EXTRA_IMAGE_URI, selectedUri.toString())
                startActivity(intent)
            } else {
                showToast("Gagal mengambil gambar")
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        WindowCompat.setDecorFitsSystemWindows(window, false)
        window.statusBarColor = Color.TRANSPARENT
        window.navigationBarColor = Color.TRANSPARENT

        setContentView(R.layout.activity_main_with_pager)

        viewPager = findViewById(R.id.viewPager)
        val adapter = ViewPagerAdapter(this)
        viewPager.adapter = adapter
        viewPager.currentItem = ViewPagerAdapter.HOME_TAB

        checkCameraPermission()
    }

    private fun checkCameraPermission() {
        when {
            hasCameraPermission() -> {}
            shouldShowRequestPermissionRationale(Manifest.permission.CAMERA) -> {
                showCameraPermissionRationale()
            }
            else -> {
                cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
            }
        }
    }

    private fun showCameraPermissionRationale() {
        AlertDialog.Builder(this)
            .setTitle(CAMERA_PERMISSION_TITLE)
            .setMessage(CAMERA_PERMISSION_MESSAGE)
            .setPositiveButton(OK_BUTTON) { _, _ ->
                cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
            }
            .setNegativeButton(CANCEL_BUTTON, null)
            .show()
    }

    fun hasCameraPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED
    }

    fun requestCameraPermission() {
        cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
    }

    fun openGallery() {
        val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI).apply {
            type = "image/*"
        }
        if (intent.resolveActivity(packageManager) != null) {
            galleryLauncher.launch(intent)
        } else {
            showToast("Tidak ada aplikasi galeri yang tersedia", Toast.LENGTH_LONG)
        }
    }

    private fun showToast(message: String, duration: Int = Toast.LENGTH_SHORT) {
        Toast.makeText(this, message, duration).show()
    }

    // Navigasi untuk kembali ke Home Tab
    fun navigateToHomeTab() {
        viewPager.currentItem = ViewPagerAdapter.HOME_TAB
    }

    // --- PERBAIKAN: Fungsi ini yang sebelumnya hilang ---
    // Navigasi untuk pindah ke Camera Tab (digunakan oleh HomeFragment)
    fun navigateToCameraTab() {
        viewPager.currentItem = ViewPagerAdapter.CAMERA_TAB
    }
}