package com.example.growmate

import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.example.growmate.databinding.FragmentProfileBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.UserProfileChangeRequest
import com.google.firebase.firestore.FirebaseFirestore

class ProfileFragment : Fragment() {

    private var _binding: FragmentProfileBinding? = null
    private val binding get() = _binding!!

    private lateinit var auth: FirebaseAuth
    private lateinit var firestore: FirebaseFirestore

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentProfileBinding.inflate(inflater, container, false)
        return binding.root
    }
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        auth = FirebaseAuth.getInstance()
        firestore = FirebaseFirestore.getInstance()

        val user = FirebaseAuth.getInstance().currentUser

        user?.let {
            binding.etDisplayName.setText(it.displayName ?: "")
            binding.etEmail.setText(it.email ?: "")
        }

        var isInEditMode = false

        binding.btnEditProfile.setOnClickListener {
            // Sembunyikan TextView
            binding.tvDisplayName.visibility = View.GONE
            binding.tvUserEmail.visibility = View.GONE
            binding.tvPassword.visibility = View.GONE

            // Tampilkan EditText
            binding.tilName.visibility = View.VISIBLE
            binding.tilEmail.visibility = View.VISIBLE
            binding.tilPassword.visibility = View.VISIBLE

            // Aktifkan input
            binding.etDisplayName.apply {
                isFocusable = true
                isFocusableInTouchMode = true
                isClickable = true
            }
            binding.etEmail.apply {
                isFocusable = true
                isFocusableInTouchMode = true
                isClickable = true
            }
            binding.etPassword.apply {
                isFocusable = true
                isFocusableInTouchMode = true
                isClickable = true
            }

            // Isi nilai EditText dari TextView
            binding.etDisplayName.setText(binding.tvDisplayName.text.toString())
            binding.etEmail.setText(binding.tvUserEmail.text.toString())

            // Ganti tombol
            binding.btnEditProfile.visibility = View.GONE
            binding.btnSaveProfile.visibility = View.VISIBLE
        }

        binding.btnSaveProfile.setOnClickListener {
            val user = FirebaseAuth.getInstance().currentUser
            val newName = binding.etDisplayName.text.toString().trim()
            val newEmail = binding.etEmail.text.toString().trim()
            val newPassword = binding.etPassword.text.toString().trim()

            if (user == null) {
                Toast.makeText(context, "User tidak ditemukan", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // 1. Update Display Name
            val profileUpdates = UserProfileChangeRequest.Builder()
                .setDisplayName(newName)
                .build()

            user.updateProfile(profileUpdates)
                .addOnSuccessListener {
                    // 2. Update Email
                    user.updateEmail(newEmail)
                        .addOnSuccessListener {
                            // 3. Update Password jika tidak kosong
                            if (newPassword.isNotEmpty()) {
                                user.updatePassword(newPassword)
                                    .addOnSuccessListener {
                                        Toast.makeText(context, "Profil berhasil diperbarui", Toast.LENGTH_SHORT).show()
                                        binding.tvDisplayName.text = newName
                                        binding.tvUserEmail.text = newEmail
                                        exitEditMode()
                                    }
                                    .addOnFailureListener {
                                        Toast.makeText(context, "Gagal update password: ${it.message}", Toast.LENGTH_SHORT).show()
                                    }
                            } else {
                                Toast.makeText(context, "Profil berhasil diperbarui", Toast.LENGTH_SHORT).show()
                                binding.tvDisplayName.text = newName
                                binding.tvUserEmail.text = newEmail
                                exitEditMode()
                            }
                        }
                        .addOnFailureListener {
                            Toast.makeText(context, "Gagal update email: ${it.message}", Toast.LENGTH_SHORT).show()
                        }
                }
                .addOnFailureListener {
                    Toast.makeText(context, "Gagal update nama: ${it.message}", Toast.LENGTH_SHORT).show()
                }

            binding.btnEditProfile.visibility = View.VISIBLE
        }


        binding.btnLogout.setOnClickListener {
            auth.signOut()
            startActivity(Intent(requireContext(), LoginActivity::class.java))
            requireActivity().finish()
        }

        binding.btnDeleteAccount.setOnClickListener {
            AlertDialog.Builder(requireContext())
                .setTitle("Hapus Akun")
                .setMessage("Yakin ingin menghapus akun ini secara permanen?")
                .setPositiveButton("Ya") { _, _ ->
                    FirebaseAuth.getInstance().currentUser?.delete()
                        ?.addOnSuccessListener {
                            Toast.makeText(context, "Akun dihapus", Toast.LENGTH_SHORT).show()
                            startActivity(Intent(requireContext(), LoginActivity::class.java))
                            requireActivity().finish()
                        }
                        ?.addOnFailureListener {
                            Toast.makeText(context, "Gagal hapus akun", Toast.LENGTH_SHORT).show()
                        }
                }
                .setNegativeButton("Batal", null)
                .show()
        }


        loadUserProfile()
    }

    private fun loadUserProfile() {
        val user = auth.currentUser ?: return
        firestore.collection("users").document(user.uid).get()
            .addOnSuccessListener { doc ->
                if (doc != null && doc.exists()) {
                    binding.tvDisplayName.text = doc.getString("name") ?: "Tidak diketahui"
                    binding.tvUserEmail.text = doc.getString("email") ?: user.email ?: "-"
                } else {
                    binding.tvDisplayName.text = user.displayName ?: "Tidak diketahui"
                    binding.tvUserEmail.text = user.email ?: "-"
                }
            }
            .addOnFailureListener {
                Toast.makeText(requireContext(), "Gagal ambil data user", Toast.LENGTH_SHORT).show()
            }
    }

    private fun exitEditMode() {
        binding.tilName.visibility = View.GONE
        binding.tilEmail.visibility = View.GONE
        binding.tilPassword.visibility = View.GONE
        binding.btnSaveProfile.visibility = View.GONE

        binding.tvDisplayName.visibility = View.VISIBLE
        binding.tvUserEmail.visibility = View.VISIBLE
        binding.tvPassword.visibility = View.VISIBLE

        binding.etPassword.setText("") // kosongkan password baru
    }


}