package com.zybooks.appmobilefinalproject

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout

class AccountSettingsActivity : AppCompatActivity(R.layout.account_settings) {

    private lateinit var avatar: ImageView
    private lateinit var tilCurrent: TextInputLayout
    private lateinit var tilNew: TextInputLayout
    private lateinit var tilConfirm: TextInputLayout
    private lateinit var edtCurrent: TextInputEditText
    private lateinit var edtNew: TextInputEditText
    private lateinit var edtConfirm: TextInputEditText

    // Use OpenDocument so we can persist read permission
    private val pickImage = registerForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            contentResolver.takePersistableUriPermission(
                uri, Intent.FLAG_GRANT_READ_URI_PERMISSION
            )
            getSharedPreferences("auth", MODE_PRIVATE)
                .edit().putString("avatar_uri", uri.toString()).apply()
            avatar.setImageURI(uri)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        findViewById<MaterialToolbar>(R.id.toolbar)?.setNavigationOnClickListener { finish() }

        avatar      = findViewById(R.id.imgAvatar)
        tilCurrent  = findViewById(R.id.tilCurrent)
        tilNew      = findViewById(R.id.tilNew)
        tilConfirm  = findViewById(R.id.tilConfirm)
        edtCurrent  = findViewById(R.id.edtCurrent)
        edtNew      = findViewById(R.id.edtNew)
        edtConfirm  = findViewById(R.id.edtConfirm)

        findViewById<MaterialButton>(R.id.btnChangePhoto).setOnClickListener {
            pickImage.launch(arrayOf("image/*"))
        }

        // Load saved avatar if any
        val prefs = getSharedPreferences("auth", MODE_PRIVATE)
        prefs.getString("avatar_uri", null)?.let { s ->
            runCatching { Uri.parse(s) }.getOrNull()?.let { avatar.setImageURI(it) }
        }

        findViewById<MaterialButton>(R.id.btnSave).setOnClickListener { saveChanges() }
    }

    private fun saveChanges() {
        val prefs = getSharedPreferences("auth", MODE_PRIVATE)
        val savedPass = prefs.getString("password_plain", "") ?: ""
        val curr = edtCurrent.text?.toString().orEmpty()
        val newP = edtNew.text?.toString().orEmpty()
        val conf = edtConfirm.text?.toString().orEmpty()

        tilCurrent.error = null
        tilNew.error = null
        tilConfirm.error = null

        var ok = true
        if (curr != savedPass) {
            tilCurrent.error = "Current password is incorrect"
            ok = false
        }
        if (!isStrong(newP)) {
            tilNew.error = "Min 8 chars, letters & a number"
            ok = false
        }
        if (newP != conf) {
            tilConfirm.error = "Passwords don’t match"
            ok = false
        }
        if (!ok) return

        // Save new password (demo storage)
        prefs.edit().putString("password_plain", newP).apply()
        Toast.makeText(this, "Changes saved", Toast.LENGTH_SHORT).show()
        finish()
    }

    private fun isStrong(p: String): Boolean {
        if (p.length < 8) return false
        val hasLetter = p.any { it.isLetter() }
        val hasDigit  = p.any { it.isDigit() }
        return hasLetter && hasDigit
    }
}
