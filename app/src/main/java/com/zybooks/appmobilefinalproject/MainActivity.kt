package com.zybooks.appmobilefinalproject

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.tabs.TabLayout
import android.widget.ImageButton
import android.content.Intent
import com.google.android.material.slider.RangeSlider
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import com.google.android.material.bottomsheet.BottomSheetDialog
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.Toast
import android.widget.TextView
import com.google.android.material.textfield.TextInputEditText
import android.text.Editable
import android.text.TextWatcher
import android.view.inputmethod.EditorInfo
import android.view.View
import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Bitmap
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider

// File and time utilities for naming saved images
import java.io.File
import java.io.FileInputStream
import java.io.OutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// For saving images to the device's public gallery (MediaStore)
import android.content.ContentValues
import android.hardware.SensorEventListener
import android.net.Uri
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import android.widget.Button
import android.widget.ImageView


const val REQUEST_CAMERA_PERMISSION = 1001 //for camera access

class MainActivity : AppCompatActivity(R.layout.activity_main) {

    private lateinit var btnCamera: Button
    private lateinit var imgPhoto: ImageView

    enum class Category { TABLES, CHAIRS, DESKS }
    enum class SortMode { PRICE_ASC, PRICE_DESC, NAME_ASC }


    private val cameraPermission = Manifest.permission.CAMERA

    data class FurnitureItem(
        val id: String,
        val name: String,
        val category: Category,
        val imageRes: Int,
        val price: Int,
        val color: String,
        val material: String,
        val tags: List<String>
    )

    data class FilterState(
        var minPrice: Int = 0,
        var maxPrice: Int = 10_000,
        var colors: Set<String> = emptySet(),
        var materials: Set<String> = emptySet(),
        var sort: SortMode = SortMode.PRICE_ASC
    )

    private var currentCategory = Category.TABLES
    private var currentQuery = ""
    private var filters = FilterState()

    // For dynamic chips & slider bounds
    private val allColors by lazy { allItems.map { it.color }.toSortedSet() }
    private val allMaterials by lazy { allItems.map { it.material }.toSortedSet() }
    private val globalMinPrice by lazy { allItems.minOf { it.price } }
    private val globalMaxPrice by lazy { allItems.maxOf { it.price } }

    private lateinit var rv: RecyclerView
    private lateinit var adapter: FurnitureAdapter
    private lateinit var tabLayout: TabLayout
    private lateinit var searchInput: TextInputEditText

    private val allItems = listOf(
        FurnitureItem(
            "t1",
            "Oak Table",
            Category.TABLES,
            R.drawable.placeholdertile,
            300,
            "Brown",
            "Wood",
            listOf("dining", "family")
        ),
        FurnitureItem(
            "t2",
            "Glass Table",
            Category.TABLES,
            R.drawable.placeholdertile,
            450,
            "Transparent",
            "Glass",
            listOf("modern")
        ),
        FurnitureItem(
            "t3",
            "Marble Table",
            Category.TABLES,
            R.drawable.placeholdertile,
            850,
            "White",
            "Stone",
            listOf("luxury", "dining")
        ),
        FurnitureItem(
            "c1",
            "Dining Chair",
            Category.CHAIRS,
            R.drawable.placeholdertile,
            80,
            "Brown",
            "Wood",
            listOf("set", "dining")
        ),
        FurnitureItem(
            "c2",
            "Arm Chair",
            Category.CHAIRS,
            R.drawable.placeholdertile,
            160,
            "Blue",
            "Fabric",
            listOf("cozy")
        ),
        FurnitureItem(
            "c3",
            "Office Chair",
            Category.CHAIRS,
            R.drawable.placeholdertile,
            220,
            "Black",
            "Leather",
            listOf("office", "ergonomic")
        ),
        FurnitureItem(
            "d1",
            "Standing Desk",
            Category.DESKS,
            R.drawable.placeholdertile,
            520,
            "Black",
            "Metal",
            listOf("office")
        ),
        FurnitureItem(
            "d2",
            "Corner Desk",
            Category.DESKS,
            R.drawable.placeholdertile,
            430,
            "White",
            "Wood",
            listOf("home")
        ),
        FurnitureItem(
            "d3",
            "Writing Desk",
            Category.DESKS,
            R.drawable.placeholdertile,
            280,
            "Brown",
            "Wood",
            listOf("home", "compact")
        )
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        //Camera View

        imgPhoto = findViewById(R.id.imgPhoto)
        btnCamera = findViewById(R.id.btnCamera)

        btnCamera.setOnClickListener { takePhotoPreview() }

        btnCamera.setOnLongClickListener {
            takePhotoFullRes()
            true
        }

        // RecyclerView
        rv = findViewById(R.id.rvItems)
        rv.layoutManager = GridLayoutManager(this, 3)
        rv.setHasFixedSize(true)
        rv.addItemDecoration(GridSpacingItemDecoration(3, dp(12), includeEdge = true))

        adapter = FurnitureAdapter()
        rv.adapter = adapter

        // Tabs
        tabLayout = findViewById(R.id.tabLayout)
        showCategory(Category.TABLES)

        tabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab) {
                when (tab.position) {
                    0 -> showCategory(Category.TABLES)
                    1 -> showCategory(Category.CHAIRS)
                    2 -> showCategory(Category.DESKS)
                }
            }

            override fun onTabUnselected(tab: TabLayout.Tab) {}
            override fun onTabReselected(tab: TabLayout.Tab) {}
        })

        // Search functionality
        searchInput = findViewById(R.id.edtSearch)

        // Real-time search as user types
        searchInput.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                currentQuery = s?.toString() ?: ""
                applyFiltersAndUpdate()
            }
        })

        // Search button (optional - already handled by TextWatcher)
        findViewById<ImageButton>(R.id.btnSearch).setOnClickListener {
            currentQuery = searchInput.text?.toString() ?: ""
            applyFiltersAndUpdate()
            hideKeyboard()
        }

        // Handle "Search" button on keyboard
        searchInput.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                currentQuery = searchInput.text?.toString() ?: ""
                applyFiltersAndUpdate()
                hideKeyboard()
                true
            } else {
                false
            }
        }

        // Navigation buttons
        findViewById<ImageButton>(R.id.btnHamburger).setOnClickListener {
            startActivity(Intent(this, SettingsActivity::class.java))
        }

        findViewById<ImageButton>(R.id.imgAvatar).setOnClickListener {
            startActivity(Intent(this, AccountSettingsActivity::class.java))
        }

        findViewById<ImageButton>(R.id.btnFilters).setOnClickListener {
            openFilterSheet()
        }
    }

    private fun dp(v: Int): Int = (v * resources.displayMetrics.density).toInt()

    private fun hideKeyboard() {
        val imm =
            getSystemService(android.content.Context.INPUT_METHOD_SERVICE) as android.view.inputmethod.InputMethodManager
        imm.hideSoftInputFromWindow(searchInput.windowToken, 0)
    }

    private fun takePhotoPreview(shake: String = "") {
        // Check if CAMERA permission has been granted.
        val granted = ContextCompat.checkSelfPermission(this, cameraPermission) ==
                PackageManager.PERMISSION_GRANTED

        if (!granted) {
            // Ask for CAMERA permission. The result will be handled in onRequestPermissionsResult().
            requestPermissions(
                arrayOf(cameraPermission),
                com.zybooks.appmobilefinalproject.REQUEST_CAMERA_PERMISSION
            )
            return
        }
        takePicturePreview.launch(null)
    }

    private val takePicturePreview = registerForActivityResult(
        ActivityResultContracts.TakePicturePreview()
    ) { thumbnail: Bitmap? ->
        if (thumbnail != null) {
            // Show the thumbnail in the ImageView.
            imgPhoto.setImageBitmap(thumbnail)

            // Save the thumbnail into the device's gallery so it persists.
            saveImageToGallery(thumbnail)

            Toast.makeText(this, "Preview saved to gallery", Toast.LENGTH_SHORT).show()
        } else {
            // If no photo taken or error
            Toast.makeText(this, "No preview returned", Toast.LENGTH_SHORT).show()
        }
    }

    private var photoFile: File? = null
    private var photoUri: Uri? = null


    private val takePicture = registerForActivityResult(
        ActivityResultContracts.TakePicture()
    ) { success: Boolean ->
        if (success) {
            // Display the full-resolution image in the ImageView using the Uri.
            imgPhoto.setImageURI(photoUri)

            // Copy the photo file into the public gallery folder.
            photoFile?.let { file ->
                val saved = saveFullResFileToGallery(file)
                if (saved != null) {
                    Toast.makeText(
                        this,
                        "Saved full-resolution photo to gallery",
                        Toast.LENGTH_SHORT
                    ).show()
                    // Optional: delete the app-private file once it's copied.
                    // file.delete()
                } else {
                    Toast.makeText(
                        this,
                        "Could not save full-resolution photo to gallery",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        } else {
            // The user may have canceled the camera or something failed.
            Toast.makeText(this, "Did not save photo", Toast.LENGTH_SHORT).show()
        }
    }

    private fun takePhotoFullRes() {
        val granted = ContextCompat.checkSelfPermission(this, cameraPermission) ==
                PackageManager.PERMISSION_GRANTED

        if (!granted) {
            requestPermissions(
                arrayOf(cameraPermission),
                com.zybooks.appmobilefinalproject.REQUEST_CAMERA_PERMISSION
            )
            return
        }

        // Create a File in app-specific external storage where the photo will be written.
        photoFile = createImageFile()

        // Convert the File into a content:// Uri using FileProvider.
        // The authority string must match the provider entry in AndroidManifest.xml.
        photoUri = FileProvider.getUriForFile(
            this,
            "${applicationContext.packageName}.fileprovider",
            photoFile!!
        )

        // Launch the full-resolution capture. The camera app will write the image data
        // into the Uri we provided (photoUri).
        photoUri?.let { uri ->
            takePicture.launch(uri)
        }
    }

    private fun createImageFile(): File {
        // Create a timestamp for the file name.
        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
        val imageFilename = "photo_$timeStamp.jpg"

        // getExternalFilesDir() returns a directory like:
        // /storage/emulated/0/Android/data/<package>/files/Pictures
        val storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES)

        // Create a File object in that directory.
        return File(storageDir, imageFilename)
    }


    private fun saveImageToGallery(bitmap: Bitmap) {
        val filename = "preview_${System.currentTimeMillis()}.jpg"

        // Describe the new image to the MediaStore.
        val contentValues = ContentValues().apply {
            put(
                MediaStore.Images.Media.DISPLAY_NAME,
                filename
            )               // File name shown in gallery.
            put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")              // JPEG format.
            put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/MyCustomDirectory")
            // RELATIVE_PATH controls the visible folder inside "Pictures".
        }

        val resolver = contentResolver

        // Insert a new item into the MediaStore and get a Uri pointing to it.
        val uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)

        // Open an OutputStream to the Uri and compress the Bitmap into it.
        uri?.let { mediaUri ->
            resolver.openOutputStream(mediaUri)?.use { outputStream ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, 90, outputStream)
            }
        }
    }

    private fun saveFullResFileToGallery(src: File): Uri? {
        val filename = src.name

        // Describe the new full-resolution image for the MediaStore.
        val values = ContentValues().apply {
            put(
                MediaStore.Images.Media.DISPLAY_NAME,
                filename
            )               // Keep original file name.
            put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")              // JPEG format.
            put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/MyCustomDirectory")
        }

        val resolver = contentResolver

        // Insert a new MediaStore record and get the destination Uri.
        val destUri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
            ?: return null

        // Copy the file's bytes into the MediaStore OutputStream.
        resolver.openOutputStream(destUri).use { out: OutputStream? ->
            FileInputStream(src).use { input ->
                if (out == null) return null
                input.copyTo(out)
            }
        }

        return destUri
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        val granted = grantResults.isNotEmpty() &&
                grantResults[0] == PackageManager.PERMISSION_GRANTED

        // If CAMERA permission was granted, immediately retry the preview capture
        // to give a smooth user experience.
        if (requestCode == com.zybooks.appmobilefinalproject.REQUEST_CAMERA_PERMISSION && granted) {
            takePhotoPreview()
        }
    }

    private fun openFilterSheet() {
        val dialog = BottomSheetDialog(
            this
        )

        val view = try {
            layoutInflater.inflate(R.layout.filter_sheet, null)
        } catch (e: Exception) {
            Toast.makeText(this, "Filter UI failed: ${e.message}", Toast.LENGTH_LONG).show()
            return
        }
        dialog.setContentView(view)

        // Find views
        val priceSlider = view.findViewById<RangeSlider>(R.id.priceSlider)
            ?: run {
                Toast.makeText(this, "priceSlider missing", Toast.LENGTH_SHORT).show(); return
            }
        val tvPriceRange = view.findViewById<TextView>(R.id.tvPriceRange)
        val chipColors = view.findViewById<ChipGroup>(R.id.chipColors)
            ?: run { Toast.makeText(this, "chipColors missing", Toast.LENGTH_SHORT).show(); return }
        val chipMats = view.findViewById<ChipGroup>(R.id.chipMaterials)
            ?: run {
                Toast.makeText(this, "chipMaterials missing", Toast.LENGTH_SHORT).show(); return
            }
        val rgSort = view.findViewById<RadioGroup>(R.id.rgSort)
            ?: run { Toast.makeText(this, "rgSort missing", Toast.LENGTH_SHORT).show(); return }
        val rbPriceAsc = view.findViewById<RadioButton>(R.id.sortPriceAsc)
        val rbPriceDesc = view.findViewById<RadioButton>(R.id.sortPriceDesc)
        val rbNameAsc = view.findViewById<RadioButton>(R.id.sortNameAsc)

        // Init slider
        priceSlider.valueFrom = globalMinPrice.toFloat()
        priceSlider.valueTo = globalMaxPrice.toFloat()
        priceSlider.stepSize = 10f

        val minV = maxOf(filters.minPrice, globalMinPrice).toFloat()
        val maxV = minOf(filters.maxPrice, globalMaxPrice).toFloat()
        priceSlider.values = listOf(minV, maxV)

        // Update price label in real-time
        val updatePriceLabel = {
            val values = priceSlider.values
            tvPriceRange?.text = "$${values[0].toInt()} - $${values[1].toInt()}"
        }
        if (tvPriceRange != null) {
            updatePriceLabel()
            priceSlider.addOnChangeListener { _, _, _ -> updatePriceLabel() }
        }

        // Slider label formatter
        priceSlider.setLabelFormatter { value -> "$${value.toInt()}" }

        // Dynamic chips
        fun addChips(group: ChipGroup, items: Set<String>, selected: Set<String>) {
            group.removeAllViews()
            items.forEach { label ->
                val chip = Chip(this).apply {
                    text = label
                    isCheckable = true
                    isChecked = selected.contains(label)
                }
                group.addView(chip)
            }
        }
        addChips(chipColors, allColors, filters.colors)
        addChips(chipMats, allMaterials, filters.materials)

        // Sort radio
        when (filters.sort) {
            SortMode.PRICE_ASC -> rbPriceAsc?.isChecked = true
            SortMode.PRICE_DESC -> rbPriceDesc?.isChecked = true
            SortMode.NAME_ASC -> rbNameAsc?.isChecked = true
        }

        // Reset button
        view.findViewById<com.google.android.material.button.MaterialButton>(R.id.btnReset)
            ?.setOnClickListener {
                filters = FilterState(
                    minPrice = globalMinPrice,
                    maxPrice = globalMaxPrice,
                    colors = emptySet(),
                    materials = emptySet(),
                    sort = SortMode.PRICE_ASC
                )
                applyFiltersAndUpdate()
                dialog.dismiss()
                Toast.makeText(this, "Filters reset", Toast.LENGTH_SHORT).show()
            }

        // Apply button
        view.findViewById<com.google.android.material.button.MaterialButton>(R.id.btnApply)
            ?.setOnClickListener {
                val values = priceSlider.values
                val selectedColors = groupCheckedTexts(chipColors)
                val selectedMats = groupCheckedTexts(chipMats)
                val sort = when (rgSort.checkedRadioButtonId) {
                    R.id.sortPriceDesc -> SortMode.PRICE_DESC
                    R.id.sortNameAsc -> SortMode.NAME_ASC
                    else -> SortMode.PRICE_ASC
                }

                filters = filters.copy(
                    minPrice = values[0].toInt(),
                    maxPrice = values[1].toInt(),
                    colors = selectedColors,
                    materials = selectedMats,
                    sort = sort
                )
                applyFiltersAndUpdate()
                dialog.dismiss()

                // Show feedback
                val activeFilters = getActiveFilterCount()
                val message = if (activeFilters > 0) {
                    "Applied $activeFilters filter${if (activeFilters > 1) "s" else ""}"
                } else {
                    "Showing all items"
                }
                Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
            }
        dialog.show()
    }

    private fun groupCheckedTexts(group: ChipGroup): Set<String> =
        (0 until group.childCount)
            .map { group.getChildAt(it) as Chip }
            .filter { it.isChecked }
            .map { it.text.toString() }
            .toSet()

    private fun getActiveFilterCount(): Int {
        var count = 0
        if (filters.minPrice > globalMinPrice || filters.maxPrice < globalMaxPrice) count++
        if (filters.colors.isNotEmpty()) count += filters.colors.size
        if (filters.materials.isNotEmpty()) count += filters.materials.size
        if (filters.sort != SortMode.PRICE_ASC) count++
        return count
    }

    private fun applyFiltersAndUpdate() {
        var list = allItems.filter { it.category == currentCategory }

        // Search (name or tags)
        if (currentQuery.isNotBlank()) {
            val q = currentQuery.lowercase().trim()
            list = list.filter {
                it.name.lowercase().contains(q) ||
                        it.tags.any { t -> t.lowercase().contains(q) }
            }
        }

        // Price
        list = list.filter { it.price in filters.minPrice..filters.maxPrice }

        // Colors
        if (filters.colors.isNotEmpty()) {
            list = list.filter { it.color in filters.colors }
        }

        // Materials
        if (filters.materials.isNotEmpty()) {
            list = list.filter { it.material in filters.materials }
        }

        // Sort
        list = when (filters.sort) {
            SortMode.PRICE_ASC -> list.sortedBy { it.price }
            SortMode.PRICE_DESC -> list.sortedByDescending { it.price }
            SortMode.NAME_ASC -> list.sortedBy { it.name.lowercase() }
        }

        adapter.submitList(list)

        // Show empty state feedback
        if (list.isEmpty()) {
            val hasSearch = currentQuery.isNotBlank()
            val hasFilters = getActiveFilterCount() > 0

            val message = when {
                hasSearch && hasFilters -> "No items match \"$currentQuery\" with current filters"
                hasSearch -> "No items found for \"$currentQuery\""
                hasFilters -> "No items match your filters"
                else -> "No items in this category"
            }
            Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
        }

        if (list.isNotEmpty()) {
            rv.scrollToPosition(0)
        }
    }

    private fun showCategory(cat: Category) {
        currentCategory = cat
        applyFiltersAndUpdate()
    }
}