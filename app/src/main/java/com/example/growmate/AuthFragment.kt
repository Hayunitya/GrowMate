package com.example.growmate

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import com.example.growmate.databinding.FragmentAuthBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.ktx.Firebase
import com.google.firebase.auth.ktx.auth

class AuthFragment : Fragment() {
    private var _binding: FragmentAuthBinding? = null
    private val binding get() = _binding!!

    private lateinit var auth: FirebaseAuth
    private var isLoginMode = true

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentAuthBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        auth = Firebase.auth
        updateUI()

        binding.btnAuth.setOnClickListener {
            val email = binding.etEmail.text.toString().trim()
            val password = binding.etPassword.text.toString().trim()
            val confirm = binding.etConfirmPassword.text.toString().trim()

            if (email.isEmpty() || password.isEmpty()) {
                Toast.makeText(context, "Isi semua field", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (isLoginMode) {
                login(email, password)
            } else {
                if (password != confirm) {
                    Toast.makeText(context, "Password tidak sama", Toast.LENGTH_SHORT).show()
                } else {
                    register(email, password)
                }
            }
        }

        binding.tvSwitchMode.setOnClickListener {
            isLoginMode = !isLoginMode
            updateUI()
        }
    }

    private fun updateUI() {
        if (isLoginMode) {
            binding.btnAuth.text = "Login"
            binding.tvSwitchMode.text = "Belum punya akun? Daftar"
            binding.etConfirmPassword.visibility = View.GONE
        } else {
            binding.btnAuth.text = "Register"
            binding.tvSwitchMode.text = "Sudah punya akun? Login"
            binding.etConfirmPassword.visibility = View.VISIBLE
        }
    }

    private fun login(email: String, password: String) {
        auth.signInWithEmailAndPassword(email, password)
            .addOnSuccessListener {
                Toast.makeText(context, "Login berhasil", Toast.LENGTH_SHORT).show()
//                findNavController().navigate(R.id.action_authFragment_to_homeFragment)
            }
            .addOnFailureListener {
                Toast.makeText(context, "Login gagal: ${it.message}", Toast.LENGTH_LONG).show()
            }
    }

    private fun register(email: String, password: String) {
        auth.createUserWithEmailAndPassword(email, password)
            .addOnSuccessListener {
                Toast.makeText(context, "Akun berhasil dibuat", Toast.LENGTH_SHORT).show()
                isLoginMode = true
                updateUI()
            }
            .addOnFailureListener {
                Toast.makeText(context, "Register gagal: ${it.message}", Toast.LENGTH_LONG).show()
            }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}