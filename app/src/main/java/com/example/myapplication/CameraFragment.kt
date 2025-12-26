package com.example.myapplication

import android.content.ContentValues
import android.content.Intent // Penting
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
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

    private var isTorchOn = false
    private var isFlashSupported = false

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

        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { _, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            val closeParams = binding.closeButton.layoutParams as ViewGroup.MarginLayoutParams
            closeParams.topMargin = 25 + systemBars.top
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
        binding.closeButton.setOnClickListener {
            (activity as? MainActivity)?.navigateToHomeTab()
        }

        binding.flashButton.setOnClickListener {
            if (!isFlashSupported) {
                showToast("Flash tidak tersedia")
                return@setOnClickListener
            }
            isTorchOn = !isTorchOn
            val icon = if (isTorchOn) R.drawable.ic_flash_on else R.drawable.ic_flash_off
            binding.flashButton.setImageResource(icon)
            try {
                camera?.cameraControl?.enableTorch(isTorchOn)
            } catch (e: Exception) {
                Log.e(TAG, "Gagal mengubah mode torch: ${e.message}")
            }
        }

        binding.shutterButton.setOnClickListener { view ->
            animateButtonPress(view)
            takePhoto()
        }

        binding.galleryButtonInCamera.setOnClickListener { view ->
            animateButtonPress(view)
            (activity as? MainActivity)?.openGallery()
        }
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
            val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

            try {
                cameraProvider.unbindAll()
                camera = cameraProvider.bindToLifecycle(
                    viewLifecycleOwner, cameraSelector, preview, imageCapture
                )
                isFlashSupported = camera?.cameraInfo?.hasFlashUnit() == true
                binding.flashButton.alpha = if (isFlashSupported) 1.0f else 0.5f
                isTorchOn = false
                binding.flashButton.setImageResource(R.drawable.ic_flash_off)
                camera?.cameraControl?.enableTorch(false)
                binding.cameraPreview.visibility = View.VISIBLE
            } catch (exc: Exception) {
                Log.e(TAG, "Gagal bind use case", exc)
                showToast("Gagal memulai kamera")
            }
        }, ContextCompat.getMainExecutor(requireContext()))
    }

    private fun takePhoto() {
        val imageCapture = this.imageCapture ?: return

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
            .Builder(requireContext().contentResolver, MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)
            .build()

        imageCapture.takePicture(
            outputOptions,
            ContextCompat.getMainExecutor(requireContext()),
            object : ImageCapture.OnImageSavedCallback {
                override fun onError(exc: ImageCaptureException) {
                    Log.e(TAG, "Gagal mengambil foto: ${exc.message}", exc)
                    showToast("Gagal mengambil foto")
                }

                // --- BAGIAN INI YANG MEMBUAT PINDAH HALAMAN ---
                override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
                    val savedUri = outputFileResults.savedUri ?: return
                    Log.d(TAG, "Foto tersimpan di: $savedUri")

                    // Pindah ke ResultActivity
                    activity?.runOnUiThread {
                        val context = requireContext()
                        val intent = Intent(context, ResultActivity::class.java)
                        intent.putExtra(ResultActivity.EXTRA_IMAGE_URI, savedUri.toString())
                        startActivity(intent)
                    }
                }
            }
        )
    }

    private fun showToast(message: String) {
        activity?.runOnUiThread {
            Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        camera?.cameraControl?.enableTorch(false)
        cameraExecutor.shutdown()
        _binding = null
    }
}