package com.togetherweserve.app.auth

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.togetherweserve.app.MainActivity
import com.togetherweserve.app.R
import com.togetherweserve.app.databinding.ActivityLoginBinding

/**
 * FR1 / FR2 - Registration & secure login, including Google Sign-In (SSO).
 * Firebase Authentication handles password hashing/storage server-side;
 * this app never stores or transmits a plain-text password itself.
 */
class LoginActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLoginBinding
    private lateinit var auth: FirebaseAuth
    private lateinit var googleSignInLauncher: androidx.activity.result.ActivityResultLauncher<Intent>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)
        auth = FirebaseAuth.getInstance()

        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.default_web_client_id))
            .requestEmail()
            .build()
        val googleSignInClient = GoogleSignIn.getClient(this, gso)

        googleSignInLauncher = registerForActivityResult(
            androidx.activity.result.contract.ActivityResultContracts.StartActivityForResult()
        ) { result ->
            val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
            try {
                val account = task.getResult(ApiException::class.java)
                firebaseAuthWithGoogle(account.idToken)
            } catch (e: ApiException) {
                Log.e(TAG, "Google sign-in failed, statusCode=${e.statusCode}", e)
                showError()
            }
        }

        binding.btnLogin.setOnClickListener {
            val email = binding.inputEmail.text?.toString()?.trim().orEmpty()
            val password = binding.inputPassword.text?.toString().orEmpty()
            if (email.isEmpty() || password.isEmpty()) {
                Log.w(TAG, "Login blocked: empty email or password")
                showError()
                return@setOnClickListener
            }
            setLoading(true)
            auth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener { task ->
                    setLoading(false)
                    if (task.isSuccessful) {
                        Log.i(TAG, "Email/password login succeeded")
                        goToMain()
                    } else {
                        Log.w(TAG, "Email/password login failed", task.exception)
                        showError()
                    }
                }
        }

        binding.btnGoogleSignIn.setOnClickListener {
            googleSignInLauncher.launch(googleSignInClient.signInIntent)
        }

        binding.tvGoToRegister.setOnClickListener {
            startActivity(Intent(this, RegisterActivity::class.java))
        }
    }

    private fun firebaseAuthWithGoogle(idToken: String?) {
        if (idToken == null) { Log.w(TAG, "Google account returned no ID token"); showError(); return }
        setLoading(true)
        val credential = GoogleAuthProvider.getCredential(idToken, null)
        auth.signInWithCredential(credential).addOnCompleteListener { task ->
            setLoading(false)
            if (task.isSuccessful) {
                Log.i(TAG, "Google SSO sign-in succeeded")
                goToMain()
            } else {
                Log.w(TAG, "Firebase rejected Google credential", task.exception)
                showError()
            }
        }
    }

    private fun goToMain() {
        startActivity(Intent(this, MainActivity::class.java))
        finish()
    }

    private fun setLoading(loading: Boolean) {
        binding.progressBar.visibility = if (loading) View.VISIBLE else View.GONE
        binding.btnLogin.isEnabled = !loading
    }

    private fun showError() {
        binding.progressBar.visibility = View.GONE
        com.google.android.material.snackbar.Snackbar.make(
            binding.root, getString(R.string.error_login_failed), com.google.android.material.snackbar.Snackbar.LENGTH_LONG
        ).show()
    }

    override fun onStart() {
        super.onStart()
        if (auth.currentUser != null) goToMain()
    }

    companion object {
        private const val TAG = "TWS-Login"
    }
}
