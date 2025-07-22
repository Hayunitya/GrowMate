package com.example.growmate

import android.app.Activity
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
import com.bumptech.glide.Glide


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

        loadUserProfile()
        loadUserPoints()

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

        binding.ivProfilePhoto.setOnClickListener {
            val intent = Intent(Intent.ACTION_PICK)
            intent.type = "image/*"
            startActivityForResult(intent, 1001)
        }

    }

    private fun loadUserProfile() {
        val user = auth.currentUser ?: return
        firestore.collection("users").document(user.uid).get()
            .addOnSuccessListener { doc ->
                val displayNameFromAuth = user.displayName ?: "Tidak diketahui"
                val emailFromAuth = user.email ?: "-"

                // Tampilkan foto profil
                user.photoUrl?.let {
                    Glide.with(requireContext())
                        .load(it)
                        .placeholder(R.drawable.ic_profile) // opsional: gambar default
                        .into(binding.ivProfilePhoto)

                }

                binding.tvDisplayName.text = displayNameFromAuth
                binding.tvUserEmail.text = emailFromAuth
            }
            .addOnFailureListener {
                binding.tvDisplayName.text = auth.currentUser?.displayName ?: "Tidak diketahui"
                binding.tvUserEmail.text = auth.currentUser?.email ?: "-"
                Toast.makeText(requireContext(), "Gagal ambil data user", Toast.LENGTH_SHORT).show()
            }
    }

    private fun loadUserPoints() {
        val user = auth.currentUser ?: return
        firestore.collection("users").document(user.uid).get()
            .addOnSuccessListener { snapshot ->
                val points = snapshot.getLong("points") ?: 0
                binding.tvPoints.text = "Poin: $points"
            }
            .addOnFailureListener {
                binding.tvPoints.text = "Poin: -"
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

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == 1001 && resultCode == Activity.RESULT_OK && data != null) {
            val imageUri = data.data ?: return

            // Upload ke Firebase Storage
            val uid = auth.currentUser?.uid ?: return
            val storageRef = com.google.firebase.storage.FirebaseStorage.getInstance()
                .reference.child("profile_photos/$uid.jpg")

            storageRef.putFile(imageUri)
                .continueWithTask { task ->
                    if (!task.isSuccessful) throw task.exception!!
                    storageRef.downloadUrl
                }.addOnSuccessListener { uri ->
                    // Update ke Firebase Auth
                    val updates = UserProfileChangeRequest.Builder()
                        .setPhotoUri(uri)
                        .build()

                    auth.currentUser?.updateProfile(updates)
                        ?.addOnSuccessListener {
                            Toast.makeText(requireContext(), "Foto profil diperbarui!", Toast.LENGTH_SHORT).show()
                            Glide.with(requireContext())
                                .load(uri.toString())
                                .placeholder(R.drawable.ic_profile)
                                .into(binding.ivProfilePhoto)
                        }
                }.addOnFailureListener {
                    Toast.makeText(requireContext(), "Upload gagal: ${it.message}", Toast.LENGTH_SHORT).show()
                }
        }
    }

}