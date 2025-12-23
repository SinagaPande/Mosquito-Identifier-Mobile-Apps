package com.example.myapplication

import android.content.ContentValues
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.camera.core.Camera // ✅ Import Wajib untuk akses Torch
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.fragment.app.Fragment
import com.example.myapplication.databinding.FragmentCameraBinding
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class CameraFragment : Fragment() {

    private var _binding: FragmentCameraBinding? = null
    private val binding get() = _binding!!

    // CameraX components
    private var imageCapture: ImageCapture? = null
    private var camera: Camera? = null // ✅ Variable untuk akses kontrol Torch (Senter)
    private lateinit var cameraExecutor: ExecutorService

    // State untuk Torch
    private var isTorchOn = false
    private var isFlashSupported = false

    private companion object {
        private const val TAG = "CameraFragment"
        private const val FILENAME_FORMAT = "yyyy-MM-dd-HH-mm-ss-SSS"
        private const val CAMERA_NOT_READY_MESSAGE = "Kamera belum siap"
        private const val REQUESTING_CAMERA_PERMISSION = "Meminta izin kamera..."
        private const val CAMERA_START_FAILED = "Gagal memulai kamera"
        private const val PHOTO_CAPTURE_FAILED = "Gagal mengambil foto"
        private const val PHOTO_SAVED_SUCCESS = "Foto berhasil disimpan"
        private const val IMAGE_JPEG = "image/jpeg"
        private const val PICTURES_PATH = "Pictures/CameraX-App"
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentCameraBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        cameraExecutor = Executors.newSingleThreadExecutor()

        // Pengaturan Insets (Safe Zones)
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { _, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())

            // Sesuaikan margin agar tidak tertutup poni/status bar
            val closeParams = binding.closeButton.layoutParams as ViewGroup.MarginLayoutParams
            closeParams.topMargin = 25 + systemBars.top // Mengikuti margin XML asli (25dp)
            binding.closeButton.layoutParams = closeParams

            insets
        }

        setupButtonListeners()
    }

    override fun onResume() {
        super.onResume()
        checkPermissionAndStartCamera()
    }

    override fun onPause() {
        super.onPause()
        binding.cameraPreview.visibility = View.GONE
    }

    private fun setupButtonListeners() {
        // Close Button
        binding.closeButton.setOnClickListener {
            (activity as? MainActivity)?.navigateToHomeTab()
        }

        // --- LOGIC TORCH (SENTER) ---
        binding.flashButton.setOnClickListener {
            // 1. Cek dukungan hardware
            if (!isFlashSupported) {
                showToast("Flash tidak tersedia di perangkat ini")
                return@setOnClickListener
            }

            // 2. Toggle State
            isTorchOn = !isTorchOn

            // 3. Update Icon UI (Langsung berubah)
            val icon = if (isTorchOn) R.drawable.ic_flash_on else R.drawable.ic_flash_off
            binding.flashButton.setImageResource(icon)

            // 4. Perintah ke Hardware Kamera (Enable/Disable Torch)
            try {
                camera?.cameraControl?.enableTorch(isTorchOn)
            } catch (e: Exception) {
                Log.e(TAG, "Gagal mengubah mode torch: ${e.message}")
            }
        }

        // Shutter Button
        binding.shutterButton.setOnClickListener { view ->
            animateButtonPress(view)
            takePhoto()
        }

        // Gallery Button
        binding.galleryButtonInCamera.setOnClickListener { view ->
            animateButtonPress(view)
            (activity as? MainActivity)?.openGallery()
        }
    }

    private fun animateButtonPress(view: View) {
        view.animate()
            .scaleX(0.9f)
            .scaleY(0.9f)
            .setDuration(100)
            .withEndAction {
                view.animate()
                    .scaleX(1.0f)
                    .scaleY(1.0f)
                    .setDuration(100)
                    .start()
            }
            .start()
    }

    private fun checkPermissionAndStartCamera() {
        val mainActivity = activity as? MainActivity
        if (mainActivity?.hasCameraPermission() == true) {
            startCamera()
        } else {
            showToast(REQUESTING_CAMERA_PERMISSION)
            mainActivity?.requestCameraPermission()
            binding.cameraPreview.visibility = View.GONE
        }
    }

    private fun startCamera() {
        val context = context ?: return
        val cameraProviderFuture = ProcessCameraProvider.getInstance(context)

        cameraProviderFuture.addListener({
            val cameraProvider: ProcessCameraProvider = cameraProviderFuture.get()

            val preview = Preview.Builder()
                .build()
                .also {
                    it.setSurfaceProvider(binding.cameraPreview.surfaceProvider)
                }

            // ImageCapture setup (Flash mode tidak di-set di sini karena kita pakai Torch manual)
            imageCapture = ImageCapture.Builder()
                .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                .build()

            val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

            try {
                cameraProvider.unbindAll()

                // ✅ BINDING KAMERA & SIMPAN REFERENSI
                // Kita menyimpan objek 'camera' agar bisa mengakses cameraControl nanti
                camera = cameraProvider.bindToLifecycle(
                    viewLifecycleOwner, cameraSelector, preview, imageCapture
                )

                // Cek apakah HP punya flash
                isFlashSupported = camera?.cameraInfo?.hasFlashUnit() == true

                // Visual cue jika flash tidak support (redupkan tombol)
                binding.flashButton.alpha = if (isFlashSupported) 1.0f else 0.5f

                // Reset state Torch ke OFF saat kamera mulai
                isTorchOn = false
                binding.flashButton.setImageResource(R.drawable.ic_flash_off)
                camera?.cameraControl?.enableTorch(false)

                binding.cameraPreview.visibility = View.VISIBLE

            } catch (exc: Exception) {
                Log.e(TAG, "Gagal bind use case", exc)
                showToast(CAMERA_START_FAILED)
            }

        }, ContextCompat.getMainExecutor(requireContext()))
    }

    private fun takePhoto() {
        val context = context ?: return
        val imageCapture = this.imageCapture ?: run {
            showToast(CAMERA_NOT_READY_MESSAGE)
            return
        }

        val name = SimpleDateFormat(FILENAME_FORMAT, Locale.US)
            .format(System.currentTimeMillis())
        val contentValues = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, name)
            put(MediaStore.MediaColumns.MIME_TYPE, IMAGE_JPEG)
            if (Build.VERSION.SDK_INT > Build.VERSION_CODES.P) {
                put(MediaStore.Images.Media.RELATIVE_PATH, PICTURES_PATH)
            }
        }

        val outputOptions = ImageCapture.OutputFileOptions
            .Builder(
                requireContext().contentResolver,
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                contentValues
            )
            .build()

        imageCapture.takePicture(
            outputOptions,
            ContextCompat.getMainExecutor(context),
            object : ImageCapture.OnImageSavedCallback {
                override fun onError(exc: ImageCaptureException) {
                    Log.e(TAG, "Gagal mengambil foto: ${exc.message}", exc)
                    showToast(PHOTO_CAPTURE_FAILED)
                }

                override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
                    val msg = "$PHOTO_SAVED_SUCCESS: ${outputFileResults.savedUri}"
                    showToast(msg, Toast.LENGTH_LONG)
                    Log.d(TAG, msg)
                }
            }
        )
    }

    private fun showToast(message: String, duration: Int = Toast.LENGTH_SHORT) {
        activity?.runOnUiThread {
            Toast.makeText(requireContext(), message, duration).show()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        // ✅ SAFETY: Matikan senter saat view dihancurkan (misal pindah tab)
        try {
            camera?.cameraControl?.enableTorch(false)
        } catch (e: Exception) {
            Log.e(TAG, "Gagal mematikan torch saat destroy: ${e.message}")
        }
        cameraExecutor.shutdown()
        _binding = null
    }
}