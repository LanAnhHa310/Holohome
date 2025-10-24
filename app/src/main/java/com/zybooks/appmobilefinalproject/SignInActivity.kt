package com.zybooks.appmobilefinalproject

import android.content.Intent
import android.os.Bundle
import android.util.Patterns
import android.view.inputmethod.EditorInfo
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.widget.addTextChangedListener
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.button.MaterialButton
import com.google.android.material.checkbox.MaterialCheckBox
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout

class SignInActivity : AppCompatActivity(R.layout.sign_in) {

    private lateinit var tilEmail: TextInputLayout
    private lateinit var tilPassword: TextInputLayout
    private lateinit var edtEmail: TextInputEditText
    private lateinit var edtPassword: TextInputEditText
    private lateinit var cbRemember: MaterialCheckBox
    private lateinit var btnSignIn: MaterialButton
    private lateinit var tvForgot: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Toolbar back/up
        findViewById<MaterialToolbar>(R.id.toolbar)?.setNavigationOnClickListener { finish() }

        // Bind views
        tilEmail    = findViewById(R.id.tilEmail)
        tilPassword = findViewById(R.id.tilPassword)
        edtEmail    = findViewById(R.id.edtEmail)
        edtPassword = findViewById(R.id.edtPassword)
        cbRemember  = findViewById(R.id.cbRemember)
        btnSignIn   = findViewById(R.id.btnSignIn)
        tvForgot    = findViewById(R.id.tvForgot)

        // Prefill email if previously remembered
        val prefs = getSharedPreferences("auth", MODE_PRIVATE)
        edtEmail.setText(prefs.getString("email_remembered", ""))

        // Live validation
        btnSignIn.isEnabled = false
        edtEmail.addTextChangedListener    { tilEmail.error = null;    updateButtonState() }
        edtPassword.addTextChangedListener { tilPassword.error = null; updateButtonState() }

        // IME action Done submits
        edtPassword.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_DONE && btnSignIn.isEnabled) {
                attemptSignIn()
                true
            } else false
        }

        btnSignIn.setOnClickListener { attemptSignIn() }

        tvForgot.setOnClickListener {
            // TODO: launch your forgot-password flow
            Toast.makeText(this, "Forgot password flow not implemented yet.", Toast.LENGTH_SHORT).show()
        }

        // Recompute once in case Autofill populated fields
        updateButtonState()
    }

    override fun onResume() {
        super.onResume()
        updateButtonState()
    }

    private fun updateButtonState() {
        val emailOk = isEmailValid(edtEmail.text?.toString())
        val passOk  = !edtPassword.text?.toString().isNullOrBlank()
        btnSignIn.isEnabled = emailOk && passOk
    }

    private fun attemptSignIn() {
        val email = edtEmail.text?.toString()?.trim().orEmpty()
        val pass  = edtPassword.text?.toString()?.trim().orEmpty()

        var ok = true
        if (!isEmailValid(email)) {
            tilEmail.error = "Enter a valid email"
            ok = false
        }
        if (pass.isEmpty()) {
            tilPassword.error = "Password can't be empty"
            ok = false
        }
        if (!ok) return

        // Compare with account saved by CreateAccountActivity
        val prefs = getSharedPreferences("auth", MODE_PRIVATE)
        val savedEmail = prefs.getString("email", null)?.trim()
        val savedPass  = prefs.getString("password_plain", null)?.trim()

        if (savedEmail.isNullOrEmpty() || savedPass.isNullOrEmpty()) {
            Toast.makeText(this, "No account found. Please create one first.", Toast.LENGTH_SHORT).show()
            return
        }
        if (!email.equals(savedEmail, ignoreCase = true) || pass != savedPass) {
            tilPassword.error = "Incorrect email or password"
            return
        }

        // Remember me (optional)
        if (cbRemember.isChecked) {
            prefs.edit().putString("email_remembered", email).apply()
        } else {
            prefs.edit().remove("email_remembered").apply()
        }

        // Mark session and go to Main
        prefs.edit().putBoolean("logged_in", true).apply()
        navigateToMain()
    }

    private fun navigateToMain() {
        val intent = Intent(this, MainActivity::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
        startActivity(intent)
    }

    private fun isEmailValid(email: String?) =
        !email.isNullOrBlank() && Patterns.EMAIL_ADDRESS.matcher(email).matches()
}
