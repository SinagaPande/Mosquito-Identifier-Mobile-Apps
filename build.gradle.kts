// Lokasi: MosquitoIdentifier/build.gradle.kts (ROOT)

plugins {
    // apply false artinya: "Siapkan plugin ini, tapi jangan pakai di root, pakai di module anak saja"
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.android) apply false
}

// HAPUS BLOK android { ... }
// HAPUS BLOK dependencies { ... }