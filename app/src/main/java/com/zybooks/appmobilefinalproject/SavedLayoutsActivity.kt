package com.zybooks.appmobilefinalproject

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.PopupMenu
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.textfield.TextInputEditText

class SavedLayoutsActivity : AppCompatActivity(R.layout.saved_lay) {

    private lateinit var rvSaved: RecyclerView
    private lateinit var edtSearch: TextInputEditText
    private lateinit var btnFilters: ImageButton
    private lateinit var btnSearch: ImageButton
    private lateinit var imgAvatar: ImageView

    private lateinit var adapter: SavedLayoutsAdapter
    private var allLayouts: MutableList<SavedLayout> = mutableListOf()
    private var filteredLayouts: List<SavedLayout> = emptyList()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Set up back button in toolbar
        findViewById<MaterialToolbar>(R.id.toolbar)?.setNavigationOnClickListener {
            finish()
        }

        // Bind views
        rvSaved = findViewById(R.id.rvSaved)
        edtSearch = findViewById(R.id.edtSearch)
        btnFilters = findViewById(R.id.btnFilters)
        btnSearch = findViewById(R.id.btnSearch)
        imgAvatar = findViewById(R.id.imgAvatar)

        // Load saved layouts
        allLayouts = loadSavedLayouts().toMutableList()
        filteredLayouts = allLayouts

        // Set up RecyclerView with Grid layout (2 columns)
        setupRecyclerView()

        // Avatar click - go to settings
        imgAvatar.setOnClickListener {
            startActivity(Intent(this, SettingsActivity::class.java))
        }

        // Search functionality
        btnSearch.setOnClickListener {
            performSearch(edtSearch.text.toString())
        }

        // Real-time search as user types
        edtSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                performSearch(s.toString())
            }

            override fun afterTextChanged(s: Editable?) {}
        })

        // Filters button
        btnFilters.setOnClickListener {
            showFilterMenu(it)
        }

        // Handle IME search action
        edtSearch.setOnEditorActionListener { _, _, _ ->
            performSearch(edtSearch.text.toString())
            true
        }
    }

    private fun setupRecyclerView() {
        adapter = SavedLayoutsAdapter(
            layouts = filteredLayouts,
            onItemClick = { layout ->
                // Handle item click - open layout in AR view
                Toast.makeText(this, "Opening: ${layout.name}", Toast.LENGTH_SHORT).show()
                // TODO: Navigate to AR view with this layout

            },
            onLongClick = { layout, view ->
                showMoreOptionsMenu(layout, view)
            }
        )

        // Use GridLayoutManager for 2-column layout
        rvSaved.layoutManager = GridLayoutManager(this, 2)
        rvSaved.adapter = adapter
    }

    private fun performSearch(query: String) {
        val searchQuery = query.trim().lowercase()

        filteredLayouts = if (searchQuery.isEmpty()) {
            allLayouts
        } else {
            allLayouts.filter { layout ->
                layout.name.lowercase().contains(searchQuery) ||
                        layout.roomType.lowercase().contains(searchQuery)
            }
        }

        adapter.updateData(filteredLayouts)

        // Show message if no results
        if (filteredLayouts.isEmpty() && searchQuery.isNotEmpty()) {
            Toast.makeText(this, "No layouts found matching \"$searchQuery\"", Toast.LENGTH_SHORT).show()
        }
    }

    private fun showFilterMenu(view: View) {
        val popup = PopupMenu(this, view)
        popup.menuInflater.inflate(R.menu.filter_menu, popup.menu)

        popup.setOnMenuItemClickListener { item ->
            when (item.itemId) {
                R.id.filter_all -> {
                    filteredLayouts = allLayouts
                    adapter.updateData(filteredLayouts)
                    edtSearch.setText("")
                    Toast.makeText(this, "Showing all layouts", Toast.LENGTH_SHORT).show()
                    true
                }
                R.id.filter_living_room -> {
                    filterByRoomType("Living Room")
                    true
                }
                R.id.filter_bedroom -> {
                    filterByRoomType("Bedroom")
                    true
                }
                R.id.filter_kitchen -> {
                    filterByRoomType("Kitchen")
                    true
                }
                R.id.filter_office -> {
                    filterByRoomType("Office")
                    true
                }
                else -> false
            }
        }

        popup.show()
    }

    private fun filterByRoomType(roomType: String) {
        edtSearch.setText("")
        filteredLayouts = allLayouts.filter { it.roomType == roomType }
        adapter.updateData(filteredLayouts)

        if (filteredLayouts.isEmpty()) {
            Toast.makeText(this, "No $roomType layouts found", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(this, "Showing ${filteredLayouts.size} $roomType layout(s)", Toast.LENGTH_SHORT).show()
        }
    }

    private fun showMoreOptionsMenu(layout: SavedLayout, view: View) {
        val popup = PopupMenu(this, view)
        popup.menuInflater.inflate(R.menu.layout_options_menu, popup.menu)

        popup.setOnMenuItemClickListener { item ->
            when (item.itemId) {
                R.id.option_edit -> {
                    Toast.makeText(this, "Edit: ${layout.name}", Toast.LENGTH_SHORT).show()
                    // TODO: Navigate to edit screen
                    true
                }
                R.id.option_duplicate -> {
                    duplicateLayout(layout)
                    true
                }
                R.id.option_share -> {
                    shareLayout(layout)
                    true
                }
                R.id.option_delete -> {
                    showDeleteConfirmation(layout)
                    true
                }
                else -> false
            }
        }

        popup.show()
    }

    private fun duplicateLayout(layout: SavedLayout) {
        val newLayout = SavedLayout(
            id = "copy_${System.currentTimeMillis()}",
            name = "${layout.name} (Copy)",
            roomType = layout.roomType,
            dateCreated = System.currentTimeMillis(),
            thumbnailResId = layout.thumbnailResId
        )

        allLayouts.add(0, newLayout)
        filteredLayouts = allLayouts
        adapter.updateData(filteredLayouts)

        Toast.makeText(this, "Duplicated: ${layout.name}", Toast.LENGTH_SHORT).show()

        // TODO: Save to persistent storage
    }

    private fun shareLayout(layout: SavedLayout) {
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, "Check out my ${layout.name}")
            putExtra(Intent.EXTRA_TEXT, "I created this ${layout.roomType} layout: ${layout.name}")
        }

        startActivity(Intent.createChooser(shareIntent, "Share layout via"))
    }

    private fun showDeleteConfirmation(layout: SavedLayout) {
        AlertDialog.Builder(this)
            .setTitle("Delete Layout")
            .setMessage("Are you sure you want to delete \"${layout.name}\"?")
            .setPositiveButton("Delete") { _, _ ->
                deleteLayout(layout)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun deleteLayout(layout: SavedLayout) {
        allLayouts.remove(layout)
        filteredLayouts = allLayouts
        adapter.updateData(filteredLayouts)

        Toast.makeText(this, "Deleted: ${layout.name}", Toast.LENGTH_SHORT).show()

        // TODO: Remove from persistent storage
    }

    private fun loadSavedLayouts(): List<SavedLayout> {
        // TODO: Load from SharedPreferences, Room database, or Firebase
        return listOf(
            SavedLayout(
                id = "1",
                name = "Modern Living Room",
                roomType = "Living Room",
                dateCreated = System.currentTimeMillis() - 86400000 * 5,
                thumbnailResId = R.drawable.outline_design_services_24
            ),
            SavedLayout(
                id = "2",
                name = "Cozy Bedroom",
                roomType = "Bedroom",
                dateCreated = System.currentTimeMillis() - 86400000 * 3,
                thumbnailResId = R.drawable.outline_design_services_24
            ),
            SavedLayout(
                id = "3",
                name = "Minimalist Kitchen",
                roomType = "Kitchen",
                dateCreated = System.currentTimeMillis() - 86400000,
                thumbnailResId = R.drawable.outline_design_services_24
            ),
            SavedLayout(
                id = "4",
                name = "Home Office Setup",
                roomType = "Office",
                dateCreated = System.currentTimeMillis(),
                thumbnailResId = R.drawable.outline_design_services_24
            ),
            SavedLayout(
                id = "5",
                name = "Scandinavian Living",
                roomType = "Living Room",
                dateCreated = System.currentTimeMillis() - 86400000 * 2,
                thumbnailResId = R.drawable.outline_design_services_24
            ),
            SavedLayout(
                id = "6",
                name = "Master Bedroom",
                roomType = "Bedroom",
                dateCreated = System.currentTimeMillis() - 86400000 * 7,
                thumbnailResId = R.drawable.outline_design_services_24
            )
        )
    }
}