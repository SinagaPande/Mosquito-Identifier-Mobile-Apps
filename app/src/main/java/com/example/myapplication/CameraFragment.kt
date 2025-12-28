package com.example.myapplication

import android.content.ContentValues
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.Toast
import androidx.camera.core.Camera
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

    private var imageCapture: ImageCapture? = null
    private var camera: Camera? = null
    private lateinit var cameraExecutor: ExecutorService
    private lateinit var scaleGestureDetector: ScaleGestureDetector

    // --- VARIABEL UNTUK FLIP CAMERA ---
    private var cameraSelector: CameraSelector = CameraSelector.DEFAULT_BACK_CAMERA
    // ----------------------------------

    private var isTorchOn = false

    private companion object {
        private const val TAG = "CameraFragment"
        private const val FILENAME_FORMAT = "yyyy-MM-dd-HH-mm-ss-SSS"
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

        // Hapus atau ubah bagian ViewCompat.setOnApplyWindowInsetsListener
        // karena closeButton sudah dihapus dan flipCameraButton sudah dipindah
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { _, insets ->
            // Hanya atur margin untuk komponen lain jika diperlukan
            // (tidak ada komponen di atas yang perlu margin top sekarang)
            insets
        }

        setupButtonListeners()
        setupZoomGesture()
    }

    private fun setupZoomGesture() {
        scaleGestureDetector = ScaleGestureDetector(requireContext(), object : ScaleGestureDetector.SimpleOnScaleGestureListener() {
            override fun onScale(detector: ScaleGestureDetector): Boolean {
                val camera = camera ?: return false
                val zoomState = camera.cameraInfo.zoomState.value ?: return false
                val currentZoomRatio = zoomState.zoomRatio
                val delta = detector.scaleFactor
                val newZoomRatio = currentZoomRatio * delta
                val clampedRatio = newZoomRatio.coerceIn(zoomState.minZoomRatio, zoomState.maxZoomRatio)
                camera.cameraControl.setZoomRatio(clampedRatio)
                return true
            }
        })

        binding.cameraPreview.setOnTouchListener { view, event ->
            scaleGestureDetector.onTouchEvent(event)

            if (event.pointerCount > 1) {
                view.parent.requestDisallowInterceptTouchEvent(true)
            } else if (event.action == MotionEvent.ACTION_UP) {
                view.parent.requestDisallowInterceptTouchEvent(false)
                view.performClick()
            } else if (event.action == MotionEvent.ACTION_CANCEL) {
                view.parent.requestDisallowInterceptTouchEvent(false)
            }

            return@setOnTouchListener true
        }
    }

    override fun onResume() {
        super.onResume()
        checkPermissionAndStartCamera()
    }

    override fun onPause() {
        super.onPause()
        binding.cameraPreview.visibility = View.GONE
        toggleScreenTorch(false)
    }

    private fun setupButtonListeners() {
        // HAPUS kode berikut (karena tombol close dihapus):
        // binding.closeButton.setOnClickListener {
        //     (activity as? MainActivity)?.navigateToHistoryTab()
        // }

        // Tombol flipCameraButton dan flashButton tetap sama, hanya posisinya yang berubah di XML
        // Tidak ada perubahan logika fungsionalitas

        binding.flipCameraButton.setOnClickListener { view ->
            animateButtonPress(view)
            flipCamera()
        }

        binding.flashButton.setOnClickListener {
            isTorchOn = !isTorchOn
            val icon = if (isTorchOn) R.drawable.ic_flash_on else R.drawable.ic_flash_off
            binding.flashButton.setImageResource(icon)

            try {
                camera?.cameraControl?.enableTorch(isTorchOn)
            } catch (e: Exception) {
                Log.e(TAG, "Gagal mengubah mode torch hardware: ${e.message}")
            }
        }

        binding.shutterButton.setOnClickListener { view ->
            animateButtonPress(view)

            if (isTorchOn && cameraSelector == CameraSelector.DEFAULT_FRONT_CAMERA) {
                takePhotoWithScreenFlash()
            } else {
                takePhoto()
            }
        }

        binding.galleryButtonInCamera.setOnClickListener { view ->
            animateButtonPress(view)
            (activity as? MainActivity)?.openGallery()
        }
    }

    // --- FUNGSI BARU: Screen Flash saat Shutter ditekan ---
    private fun takePhotoWithScreenFlash() {
        // 1. Nyalakan Layar Putih & Brightness Max
        toggleScreenTorch(true)

        // 2. Beri jeda sedikit (misal 150ms) agar layar sempat terang maksimal sebelum capture
        binding.root.postDelayed({
            val imageCapture = this.imageCapture ?: return@postDelayed

            // Persiapan file output
            val outputOptions = createOutputOptions()

            imageCapture.takePicture(
                outputOptions,
                ContextCompat.getMainExecutor(requireContext()),
                object : ImageCapture.OnImageSavedCallback {
                    override fun onError(exc: ImageCaptureException) {
                        Log.e(TAG, "Gagal mengambil foto: ${exc.message}", exc)
                        showToast("Gagal mengambil foto")
                        // Kembalikan layar ke normal jika error
                        toggleScreenTorch(false)
                    }

                    override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
                        // Kembalikan layar ke normal SEGERA setelah foto tersimpan
                        toggleScreenTorch(false)

                        val savedUri = outputFileResults.savedUri ?: return
                        navigateToResult(savedUri.toString())
                    }
                }
            )
        }, 150) // Delay 150ms
    }
    // ------------------------------------------------------

    private fun takePhoto() {
        val imageCapture = this.imageCapture ?: return
        val outputOptions = createOutputOptions()

        imageCapture.takePicture(
            outputOptions,
            ContextCompat.getMainExecutor(requireContext()),
            object : ImageCapture.OnImageSavedCallback {
                override fun onError(exc: ImageCaptureException) {
                    Log.e(TAG, "Gagal mengambil foto: ${exc.message}", exc)
                    showToast("Gagal mengambil foto")
                }

                override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
                    val savedUri = outputFileResults.savedUri ?: return
                    navigateToResult(savedUri.toString())
                }
            }
        )
    }

    // Helper untuk membuat opsi file output (agar kode tidak duplikat)
    private fun createOutputOptions(): ImageCapture.OutputFileOptions {
        val name = SimpleDateFormat(FILENAME_FORMAT, Locale.US)
            .format(System.currentTimeMillis())
        val contentValues = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, name)
            put(MediaStore.MediaColumns.MIME_TYPE, IMAGE_JPEG)
            if (Build.VERSION.SDK_INT > Build.VERSION_CODES.P) {
                put(MediaStore.Images.Media.RELATIVE_PATH, PICTURES_PATH)
            }
        }
        return ImageCapture.OutputFileOptions
            .Builder(requireContext().contentResolver, MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)
            .build()
    }

    private fun navigateToResult(uriString: String) {
        activity?.runOnUiThread {
            val context = requireContext()
            val intent = Intent(context, ResultActivity::class.java)
            intent.putExtra(ResultActivity.EXTRA_IMAGE_URI, uriString)
            startActivity(intent)
        }
    }

    private fun toggleScreenTorch(isOn: Boolean) {
        activity?.runOnUiThread {
            binding.screenFlashOverlay.visibility = if (isOn) View.VISIBLE else View.GONE

            val window = requireActivity().window
            val layoutParams = window.attributes
            if (isOn) {
                layoutParams.screenBrightness = 1.0f // Max brightness
            } else {
                layoutParams.screenBrightness = WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_NONE // Reset
            }
            window.attributes = layoutParams
        }
    }

    private fun flipCamera() {
        // Matikan senter fisik sebelum switch (agar tidak error state)
        // State tombol di UI (isTorchOn) biarkan tetap, atau reset sesuai preferensi.
        // Di sini saya reset agar aman.
        isTorchOn = false
        binding.flashButton.setImageResource(R.drawable.ic_flash_off)
        try { camera?.cameraControl?.enableTorch(false) } catch (e: Exception) {}
        toggleScreenTorch(false)

        cameraSelector = if (cameraSelector == CameraSelector.DEFAULT_BACK_CAMERA) {
            CameraSelector.DEFAULT_FRONT_CAMERA
        } else {
            CameraSelector.DEFAULT_BACK_CAMERA
        }
        startCamera()
    }

    private fun animateButtonPress(view: View) {
        view.animate().scaleX(0.9f).scaleY(0.9f).setDuration(100).withEndAction {
            view.animate().scaleX(1.0f).scaleY(1.0f).setDuration(100).start()
        }.start()
    }

    private fun checkPermissionAndStartCamera() {
        val mainActivity = activity as? MainActivity
        if (mainActivity?.hasCameraPermission() == true) {
            startCamera()
        } else {
            showToast("Meminta izin kamera...")
            mainActivity?.requestCameraPermission()
            binding.cameraPreview.visibility = View.GONE
        }
    }

    private fun startCamera() {
        val context = context ?: return
        val cameraProviderFuture = ProcessCameraProvider.getInstance(context)

        cameraProviderFuture.addListener({
            val cameraProvider: ProcessCameraProvider = cameraProviderFuture.get()

            val preview = Preview.Builder().build().also {
                it.setSurfaceProvider(binding.cameraPreview.surfaceProvider)
            }

            imageCapture = ImageCapture.Builder()
                .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                .build()

            try {
                cameraProvider.unbindAll()
                camera = cameraProvider.bindToLifecycle(
                    viewLifecycleOwner, cameraSelector, preview, imageCapture
                )

                binding.flashButton.alpha = 1.0f
                camera?.cameraControl?.setZoomRatio(1.0f)

                // Reset torch state saat inisialisasi awal
                isTorchOn = false
                binding.flashButton.setImageResource(R.drawable.ic_flash_off)
                camera?.cameraControl?.enableTorch(false)
                toggleScreenTorch(false)

                binding.cameraPreview.visibility = View.VISIBLE

            } catch (exc: Exception) {
                Log.e(TAG, "Gagal bind use case", exc)
            }
        }, ContextCompat.getMainExecutor(requireContext()))
    }

    private fun showToast(message: String) {
        activity?.runOnUiThread {
            Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        try { camera?.cameraControl?.enableTorch(false) } catch (e: Exception) {}
        toggleScreenTorch(false)
        cameraExecutor.shutdown()
        _binding = null
    }
}