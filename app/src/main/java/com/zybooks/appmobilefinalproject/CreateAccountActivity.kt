package com.zybooks.appmobilefinalproject

import android.os.Bundle
import android.util.Patterns
import android.view.inputmethod.EditorInfo
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.widget.addTextChangedListener
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.button.MaterialButton
import com.google.android.material.checkbox.MaterialCheckBox
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import android.widget.Toast
import android.content.Intent
import android.util.Log


class CreateAccountActivity : AppCompatActivity(R.layout.create_account_activity) {

    private lateinit var tilFullName: TextInputLayout
    private lateinit var tilEmail: TextInputLayout
    private lateinit var tilPassword: TextInputLayout
    private lateinit var tilConfirm: TextInputLayout

    private lateinit var edtFullName: TextInputEditText
    private lateinit var edtEmail: TextInputEditText
    private lateinit var edtPassword: TextInputEditText
    private lateinit var edtConfirm: TextInputEditText

    private lateinit var cbTerms: MaterialCheckBox
    private lateinit var btnCreate: MaterialButton
    private lateinit var tvSignIn: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Toolbar (optional back)
        findViewById<MaterialToolbar>(R.id.toolbar)?.setNavigationOnClickListener { finish() }

        // Bind views
        tilFullName = findViewById(R.id.tilFullName)
        tilEmail    = findViewById(R.id.tilEmail)
        tilPassword = findViewById(R.id.tilPassword)
        tilConfirm  = findViewById(R.id.tilConfirm)

        edtFullName = findViewById(R.id.edtFullName)
        edtEmail    = findViewById(R.id.edtEmail)
        edtPassword = findViewById(R.id.edtPassword)
        edtConfirm  = findViewById(R.id.edtConfirmPassword)

        cbTerms     = findViewById(R.id.cbTerms)
        btnCreate   = findViewById(R.id.btnCreateAccount)
        tvSignIn    = findViewById(R.id.tvSignIn)

        // Start disabled until valid
        btnCreate.isEnabled = true

        // Live validation & enable/disable button
        edtFullName.addTextChangedListener { tilFullName.error = null; updateButtonState() }
        edtEmail.addTextChangedListener    { tilEmail.error = null;    updateButtonState() }
        edtPassword.addTextChangedListener { tilPassword.error = null; updateButtonState() }
        edtConfirm.addTextChangedListener  { tilConfirm.error = null;  updateButtonState() }
        cbTerms.setOnCheckedChangeListener { _, _ -> updateButtonState() }

        // IME action "Done" submits
        edtConfirm.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                submitIfValid()
                true
            } else false
        }

        btnCreate.setOnClickListener { submitIfValid() }

        tvSignIn.setOnClickListener {
            startActivity(Intent(this, SignInActivity::class.java))
        }
        updateButtonState()
    }

    private fun updateButtonState() {
        btnCreate.isEnabled = true
    }

    private fun submitIfValid() {
        // Set errors where needed (so user knows what to fix)
        val name = edtFullName.text?.toString().orEmpty()
        val email = edtEmail.text?.toString().orEmpty()
        val pass = edtPassword.text?.toString().orEmpty()
        val confirm = edtConfirm.text?.toString().orEmpty()

        var valid = true

        if (!isNameValid(name)) {
            tilFullName.error = "Please enter your full name"
            valid = false
        }
        if (!isEmailValid(email)) {
            tilEmail.error = "Enter a valid email"
            valid = false
        }
        if (!isPasswordStrong(pass)) {
            tilPassword.error = "Min 8 chars, with letters & a number"
            valid = false
        }
        if (!doPasswordsMatch(pass, confirm)) {
            tilConfirm.error = "Passwords don’t match"
            valid = false
        }
        if (!cbTerms.isChecked) {
            Toast.makeText(this, "Please agree to the Terms", Toast.LENGTH_SHORT).show()
            valid = false
        }

        if (!valid) return

        // --- Demo "create account": save locally (replace with real API call)
        val prefs = getSharedPreferences("auth", MODE_PRIVATE)
        prefs.edit()
            .putString("full_name", name)
            .putString("email", email)
            // For demo only; don't store raw passwords in production
            .putString("password_plain", pass)
            .putBoolean("logged_in", true)
            .apply()

        Toast.makeText(this, "Account created!", Toast.LENGTH_SHORT).show()

        // >>> Navigate to main screen (activity_main.xml via MainActivity)
        navigateToMain(name)
    }

    // --- Navigation helper (added) ---
    private fun navigateToMain(fullName: String) {
        val intent = Intent(this, MainActivity::class.java)
            .putExtra("full_name", fullName)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
        startActivity(intent)
        // No finish() needed; flags clear the back stack
    }

    // --- Validation helpers ---
    private fun isNameValid(name: String?) = !name.isNullOrBlank()

    private fun isEmailValid(email: String?): Boolean {
        val e = email?.trim().orEmpty()
        return e.isNotEmpty() && Patterns.EMAIL_ADDRESS.matcher(e).matches()
    }

    private fun isPasswordStrong(pw: String?): Boolean {
        val p = pw?.trim().orEmpty()
        if (p.length < 8) return false
        val hasLetter = p.any { it.isLetter() }
        val hasDigit  = p.any { it.isDigit() }
        return hasLetter && hasDigit
    }

    private fun doPasswordsMatch(p1: String?, p2: String?): Boolean {
        val a = p1?.trim().orEmpty()
        val b = p2?.trim().orEmpty()
        return a.isNotEmpty() && a == b
    }
}