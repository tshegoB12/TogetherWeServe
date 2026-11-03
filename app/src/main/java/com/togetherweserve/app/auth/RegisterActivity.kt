package com.togetherweserve.app.auth

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.UserProfileChangeRequest
import com.togetherweserve.app.MainActivity
import com.togetherweserve.app.R
import com.togetherweserve.app.databinding.ActivityRegisterBinding

/** FR1 - Email/password registration. Password hashing and storage is
 *  handled entirely by Firebase Authentication (industry-standard hashing);
 *  the plain-text password is only ever sent over TLS to sign the user up
 *  and is never persisted by this app. */
class RegisterActivity : AppCompatActivity() {

    private lateinit var binding: ActivityRegisterBinding
    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRegisterBinding.inflate(layoutInflater)
        setContentView(binding.root)
        auth = FirebaseAuth.getInstance()

        binding.btnRegister.setOnClickListener {
            val name = binding.inputName.text?.toString()?.trim().orEmpty()
            val email = binding.inputEmail.text?.toString()?.trim().orEmpty()
            val password = binding.inputPassword.text?.toString().orEmpty()

            if (name.isEmpty() || email.isEmpty() || password.length < 6) {
                Log.w(TAG, "Registration blocked: invalid input")
                Snackbar.make(binding.root, getString(R.string.error_required_field), Snackbar.LENGTH_LONG).show()
                return@setOnClickListener
            }

            setLoading(true)
            auth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        Log.i(TAG, "Account created (password is hashed server-side by Firebase Auth)")
                        val profileUpdate = UserProfileChangeRequest.Builder()
                            .setDisplayName(name).build()
                        auth.currentUser?.updateProfile(profileUpdate)
                        setLoading(false)
                        startActivity(Intent(this, MainActivity::class.java))
                        finish()
                    } else {
                        Log.w(TAG, "Registration failed", task.exception)
                        setLoading(false)
                        Snackbar.make(binding.root, task.exception?.localizedMessage
                            ?: getString(R.string.error_login_failed), Snackbar.LENGTH_LONG).show()
                    }
                }
        }

        binding.tvGoToLogin.setOnClickListener { finish() }
    }

    private fun setLoading(loading: Boolean) {
        binding.progressBar.visibility = if (loading) View.VISIBLE else View.GONE
        binding.btnRegister.isEnabled = !loading
    }

    companion object {
        private const val TAG = "TWS-Register"
    }
}
