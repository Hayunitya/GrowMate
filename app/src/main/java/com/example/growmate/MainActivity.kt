package com.example.growmate

import android.content.Intent
import android.content.pm.PackageManager
import android.media.MediaPlayer
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.fragment.findNavController
import androidx.navigation.ui.setupWithNavController
import com.example.growmate.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private var mediaPlayer: MediaPlayer? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Inisialisasi layout binding
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Tombol kontrol musik
        val btnPlayPause = binding.btnPlayPause
        val btnMute = binding.btnMute

        var isMuted = false

        btnPlayPause.setOnClickListener {
            if (mediaPlayer != null) {
                if (mediaPlayer!!.isPlaying) {
                    mediaPlayer?.pause()
                    btnPlayPause.setImageResource(R.drawable.ic_play) // Ganti jadi play
                } else {
                    mediaPlayer?.start()
                    btnPlayPause.setImageResource(R.drawable.ic_pause) // Ganti jadi pause
                }
            }
        }

        btnMute.setOnClickListener {
            isMuted = !isMuted
            if (isMuted) {
                mediaPlayer?.setVolume(0f, 0f)
                btnMute.setImageResource(R.drawable.ic_muted) // pakai icon buatan sendiri
            } else {
                mediaPlayer?.setVolume(0.5f, 0.5f)
                btnMute.setImageResource(R.drawable.ic_unmuted)
            }
        }

        // Setup MediaPlayer untuk backsound
        mediaPlayer = MediaPlayer.create(this, R.raw.music)
        mediaPlayer?.isLooping = true
        mediaPlayer?.setVolume(0.5f, 0.5f)
        mediaPlayer?.start()
        btnPlayPause.setImageResource(R.drawable.ic_pause)

        // Navigasi bottom nav
        val navHostFragment = supportFragmentManager.findFragmentById(R.id.nav_host_fragment)
        val navController = navHostFragment!!.findNavController()
        binding.bottomNav.setupWithNavController(navController)

        // Cek permission notifikasi
        checkNotificationPermission()

        // Jika dibuka dari notifikasi
        if (intent.hasExtra("PLANT_NAME") || intent.hasExtra("TYPE")) {
            navController.navigate(R.id.homeFragment)
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)

        val navHostFragment = supportFragmentManager.findFragmentById(R.id.nav_host_fragment)
        val navController = navHostFragment!!.findNavController()

        if (intent.hasExtra("PLANT_NAME") || intent.hasExtra("TYPE")) {
            navController.navigate(R.id.homeFragment)
        }
    }

    private fun checkNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(arrayOf(android.Manifest.permission.POST_NOTIFICATIONS), 100)
            }
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 100) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "Notification permission granted!", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "Notification permission denied!", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onStart() {
        super.onStart()
        mediaPlayer?.start()
    }

    override fun onPause() {
        super.onPause()
        mediaPlayer?.pause()
    }

    override fun onDestroy() {
        super.onDestroy()
        mediaPlayer?.release()
        mediaPlayer = null
    }
}
