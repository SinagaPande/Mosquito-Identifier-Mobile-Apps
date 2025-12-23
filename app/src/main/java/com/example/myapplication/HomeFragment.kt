package com.example.myapplication

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.fragment.app.Fragment
import com.example.myapplication.databinding.FragmentHomeBinding

class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // --- TAHAP 2: RESPONSIVE PADDING ---
        // Mengatur agar konten di dalam containerContent tidak tertutup System Bars
        // Tapi background (ivBackground) tetap full screen
        ViewCompat.setOnApplyWindowInsetsListener(binding.containerContent) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(
                v.paddingLeft,
                systemBars.top + 32, // Padding asli (32) + tinggi status bar
                v.paddingRight,
                systemBars.bottom + 32 // Padding asli (32) + tinggi nav bar
            )
            insets
        }

        setupButtonListeners()
    }

    private fun setupButtonListeners() {
        binding.cameraButton.setOnClickListener {
            navigateToCameraFragment()
        }

        binding.galleryButton.setOnClickListener {
            openGallery()
        }
    }

    private fun navigateToCameraFragment() {
        (activity as? MainActivity)?.navigateToCameraTab()
    }

    private fun openGallery() {
        (activity as? MainActivity)?.openGallery()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}