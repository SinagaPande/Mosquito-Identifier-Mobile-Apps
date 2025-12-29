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
import androidx.fragment.app.Fragment
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import androidx.camera.core.ImageProxy
import com.example.myapplication.databinding.FragmentCameraBinding
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import kotlin.math.min

class CameraFragment : Fragment() {

    private var _binding: FragmentCameraBinding? = null
    private val binding get() = _binding!!

    private var imageCapture: ImageCapture? = null
    private var camera: Camera? = null
    private lateinit var cameraExecutor: ExecutorService
    private lateinit var scaleGestureDetector: ScaleGestureDetector

    private var cameraSelector: CameraSelector = CameraSelector.DEFAULT_BACK_CAMERA
    private var isTorchOn = false

    private companion object {
        private const val TAG = "CameraFragment"
        private const val FILENAME_FORMAT = "yyyy-MM-dd-HH-mm-ss-SSS"
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
        binding.flipCameraButton.setOnClickListener { view ->
            animateButtonPress(view)
            flipCamera()
        }

        binding.flashButton.setOnClickListener {
            isTorchOn = !isTorchOn
            val icon = if (isTorchOn) R.drawable.ic_flash_on else R.drawable.ic_flash_off
            binding.flashButton.setImageResource(icon)
            try { camera?.cameraControl?.enableTorch(isTorchOn) } catch (e: Exception) {}
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

    private fun takePhotoWithScreenFlash() {
        toggleScreenTorch(true)
        binding.root.postDelayed({
            val imageCapture = this.imageCapture ?: return@postDelayed
            imageCapture.takePicture(
                ContextCompat.getMainExecutor(requireContext()),
                object : ImageCapture.OnImageCapturedCallback() {
                    override fun onCaptureSuccess(image: ImageProxy) {
                        toggleScreenTorch(false)
                        processCapturedImage(image)
                    }

                    override fun onError(exc: ImageCaptureException) {
                        Log.e(TAG, "Gagal mengambil foto: ${exc.message}", exc)
                        showToast("Gagal mengambil foto")
                        toggleScreenTorch(false)
                    }
                }
            )
        }, 150)
    }

    private fun takePhoto() {
        val imageCapture = this.imageCapture ?: return
        imageCapture.takePicture(
            ContextCompat.getMainExecutor(requireContext()),
            object : ImageCapture.OnImageCapturedCallback() {
                override fun onCaptureSuccess(image: ImageProxy) {
                    processCapturedImage(image)
                }

                override fun onError(exc: ImageCaptureException) {
                    Log.e(TAG, "Gagal mengambil foto: ${exc.message}", exc)
                    showToast("Gagal mengambil foto")
                }
            }
        )
    }

    private fun processCapturedImage(image: ImageProxy) {
        // Proses crop di background thread agar tidak nge-lag UI
        cameraExecutor.execute {
            val croppedBitmap = cropImageToScanArea(image)
            image.close() // Jangan lupa close ImageProxy

            // Simpan bitmap dan pindah activity di UI Thread
            val uriString = saveBitmapToUri(croppedBitmap)
            croppedBitmap.recycle()

            activity?.runOnUiThread {
                navigateToResult(uriString)
            }
        }
    }

    private fun navigateToResult(uriString: String) {
        val context = requireContext()
        val intent = Intent(context, ResultActivity::class.java)
        intent.putExtra(ResultActivity.EXTRA_IMAGE_URI, uriString)
        startActivity(intent)
    }

    private fun toggleScreenTorch(isOn: Boolean) {
        activity?.runOnUiThread {
            binding.screenFlashOverlay.visibility = if (isOn) View.VISIBLE else View.GONE
            val window = requireActivity().window
            val layoutParams = window.attributes
            layoutParams.screenBrightness = if (isOn) 1.0f else WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_NONE
            window.attributes = layoutParams
        }
    }

    private fun flipCamera() {
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

    // --- LOGIKA CROP YANG DIPERBAIKI ---
    private fun cropImageToScanArea(image: ImageProxy): Bitmap {
        val buffer = image.planes[0].buffer
        val bytes = ByteArray(buffer.remaining())
        buffer.get(bytes)
        val originalBitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)

        // 1. Putar gambar jika perlu (misal: mode portrait biasanya perlu rotasi 90 derajat)
        val rotationDegrees = image.imageInfo.rotationDegrees
        val rotatedBitmap = if (rotationDegrees != 0) {
            rotateBitmap(originalBitmap, rotationDegrees.toFloat())
        } else {
            originalBitmap
        }
        if (rotatedBitmap != originalBitmap) originalBitmap.recycle()

        // 2. Hitung Ukuran Crop (Persegi)
        // Kita ingin mengambil 65% dari lebar (sesuai ScannerOverlayView)
        // Karena preview mengisi layar, kita asumsikan gambar penuh, lalu crop tengahnya.
        val bitmapWidth = rotatedBitmap.width
        val bitmapHeight = rotatedBitmap.height

        // Ambil dimensi terkecil untuk referensi (agar aman di portrait/landscape)
        val minDimension = min(bitmapWidth, bitmapHeight)

        // Ukuran kotak crop = 65% dari lebar gambar (asumsi portrait) atau 65% dari minDimension
        // Overlay di view = width * 0.65f.
        // Kita terapkan rasio yang sama ke bitmap yang ditangkap.
        val cropSize = (minDimension * 0.65f).toInt()

        // 3. Tentukan titik tengah
        val centerX = bitmapWidth / 2
        val centerY = bitmapHeight / 2

        // 4. Hitung koordinat kiri-atas (left, top)
        val cropLeft = (centerX - (cropSize / 2)).coerceAtLeast(0)
        val cropTop = (centerY - (cropSize / 2)).coerceAtLeast(0)

        // 5. Pastikan tidak keluar batas
        val finalWidth = if (cropLeft + cropSize > bitmapWidth) bitmapWidth - cropLeft else cropSize
        val finalHeight = if (cropTop + cropSize > bitmapHeight) bitmapHeight - cropTop else cropSize

        // 6. Lakukan Crop
        return Bitmap.createBitmap(
            rotatedBitmap,
            cropLeft,
            cropTop,
            finalWidth,
            finalHeight
        )
    }

    private fun rotateBitmap(source: Bitmap, angle: Float): Bitmap {
        val matrix = Matrix()
        matrix.postRotate(angle)
        return Bitmap.createBitmap(source, 0, 0, source.width, source.height, matrix, true)
    }

    private fun saveBitmapToUri(bitmap: Bitmap): String {
        val name = SimpleDateFormat(FILENAME_FORMAT, Locale.US)
            .format(System.currentTimeMillis())
        val contentValues = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, name)
            put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg")
            if (Build.VERSION.SDK_INT > Build.VERSION_CODES.P) {
                put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/CameraX-App")
            }
        }
        val resolver = requireContext().contentResolver
        val uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)
        uri?.let {
            resolver.openOutputStream(it)?.use { outputStream ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, 90, outputStream)
            }
        }
        return uri?.toString() ?: ""
    }
}