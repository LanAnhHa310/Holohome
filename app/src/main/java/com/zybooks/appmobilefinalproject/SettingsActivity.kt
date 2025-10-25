package com.zybooks.appmobilefinalproject

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SearchView
import com.google.android.material.appbar.MaterialToolbar

class SettingsActivity : AppCompatActivity(R.layout.settings_view) {

    private lateinit var searchView: SearchView
    private lateinit var acctSet: Button
    private lateinit var savFurn: Button
    private lateinit var savLay: Button
    private lateinit var addAcct: Button
    private lateinit var logOut: Button
    private lateinit var dividerAboveAddAcct: View

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Toolbar navigation icon acts as back button
        findViewById<MaterialToolbar>(R.id.toolbar)?.setNavigationOnClickListener {
            finish()
        }

        // Bind views
        searchView = findViewById(R.id.searchView)
        acctSet = findViewById(R.id.acctSet)
        savFurn = findViewById(R.id.savFurn)
        savLay = findViewById(R.id.savLay)
        addAcct = findViewById(R.id.addAcct)
        logOut = findViewById(R.id.logOut)
        dividerAboveAddAcct = findViewById(R.id.dividerAboveAddAcct)

        // Clicks
        acctSet.setOnClickListener {
            startActivity(Intent(this, AccountSettingsActivity::class.java))
        }

        savFurn.setOnClickListener {
            startActivity(Intent(this, FavoriteFurn::class.java))
        }

        savLay.setOnClickListener {
            startActivity(Intent(this, SavedLayoutsActivity::class.java))
        }

        addAcct.setOnClickListener {
            startActivity(Intent(this, CreateAccountActivity::class.java))
        }

        logOut.setOnClickListener { doLogout() }

        // Search: filter buttons by their label text
        setUpSearchFiltering()
    }

    private fun setUpSearchFiltering() {
        // Make sure the SearchView starts expanded (you already set iconifiedByDefault="false" in XML)
        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                filterRows(query.orEmpty())
                return true
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                filterRows(newText.orEmpty())
                return true
            }
        })
    }

    private fun filterRows(q: String) {
        val query = q.trim().lowercase()

        // List of buttons with their visible labels
        val rows = listOf(acctSet, savFurn, savLay, addAcct, logOut)

        var visibleCountAboveDivider = 0
        rows.forEach { btn ->
            val label = btn.text?.toString()?.lowercase().orEmpty()
            val show = query.isEmpty() || label.contains(query)
            btn.visibility = if (show) View.VISIBLE else View.GONE

            // Count rows that appear above the divider (everything except addAcct/logOut)
            if (btn !== addAcct && btn !== logOut && show) visibleCountAboveDivider++
        }

        // Show/hide divider based on whether any rows above it are visible
        dividerAboveAddAcct.visibility =
            if (visibleCountAboveDivider > 0) View.VISIBLE else View.GONE
    }

    private fun doLogout() {
        // Clear session flags; keep email if you want “remember me”
        val prefs = getSharedPreferences("auth", MODE_PRIVATE)
        prefs.edit()
            .putBoolean("logged_in", false)
            .remove("password_plain") // don't keep raw password
            .apply()

        // Go to the sign-in screen and clear back stack
        startActivity(
            Intent(this, SignInActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
        )
        // finish() not needed because CLEAR_TASK handles it
    }
}
