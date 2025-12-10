package com.zybooks.appmobilefinalproject

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
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

import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

//For AR
import com.google.ar.core.ArCoreApk
//import com.google.ar.core.Session
//import com.google.ar.core.exceptions.*


const val REQUEST_CAMERA_PERMISSION = 1001 //for camera access

// Main furniture browsing screen.
// Uses:
// - RecyclerView to show furniture grid
// - Tabs to switch category (Tables / Chairs / Desks)
// - Volley + Room DB to load / cache furniture from a web API
// - Camera for AR-style photo capture (preview + full-res)
class MainActivity : AppCompatActivity(R.layout.activity_main) {

    // Lazy DB + DAO, using DatabaseProvider singleton.
    private val db by lazy { DatabaseProvider.getDatabase(this) }
    private val dao by lazy { db.furnitureDao() }
    private lateinit var btnCamera: Button
    private lateinit var imgPhoto: ImageView

//    // ARCore session management
//    private var mSession: Session? = null
//    private var mUserRequestedInstall = true


    // Volley request queue for API calls.
    private lateinit var requestQueue: com.android.volley.RequestQueue

    // High-level categories shown as tabs.
    enum class Category { TABLES, CHAIRS, DESKS }

    // Sorting modes used in filter bottom sheet.
    enum class SortMode { PRICE_ASC, PRICE_DESC, NAME_ASC }

    // Single permission constant for camera.
    private val cameraPermission = Manifest.permission.CAMERA

    // "Domain model" version of furniture used by UI.
    // This wraps DB entity + some computed values.
    data class FurnitureItem(
        val id: String,
        val name: String,
        val category: Category,
//        val imageRes: Int,
        val imageUrl: String?,
        val price: Int,
        val color: String,
        val material: String,
        val tags: List<String>
    )

    // In-memory representation of active filter choices.
    data class FilterState(
        var minPrice: Int = 0,
        var maxPrice: Int = 10_000,
        var colors: Set<String> = emptySet(),
        var materials: Set<String> = emptySet(),
        var sort: SortMode = SortMode.PRICE_ASC
    )

    // Current search query, and filters.
    private var currentQuery = ""
    private var filters = FilterState()

    // Derived data from allItems, safe when list is empty.
    // Used to build filter chips and price slider range dynamically.
    private val allColors: Set<String>
        get() = allItems.map { it.color }.toSortedSet()

    private val allMaterials: Set<String>
        get() = allItems.map { it.material }.toSortedSet()

    private val globalMinPrice: Int
        get() = allItems.minOfOrNull { it.price } ?: 0

    private val globalMaxPrice: Int
        get() = allItems.maxOfOrNull { it.price } ?: 10_000

    // RecyclerView + adapter + search field
    private lateinit var rv: RecyclerView
    private lateinit var adapter: FurnitureAdapter
    private lateinit var searchInput: TextInputEditText

    // All furniture items loaded from DB.
    // This is the "master list" before filters/search.
    private var allItems: List<FurnitureItem> = emptyList()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        Toast.makeText(this, "MainActivity onCreate()", Toast.LENGTH_SHORT).show()
        Log.d("MainActivity", "onCreate called")

        // Init Volley request queue for network.
        requestQueue = com.android.volley.toolbox.Volley.newRequestQueue(this)
        // Because we used AppCompatActivity(R.layout.activity_main),
        // the layout is already set here. No need to call setContentView() again.

        // Enable AR-related functionality on ARCore supported devices only.
        maybeEnableArButton()

        //Camera View

        imgPhoto = findViewById(R.id.imgPhoto)
        btnCamera = findViewById(R.id.btnCamera)

        btnCamera.setOnClickListener { takePhotoPreview() }

        // Launch the AR view when "AR Mode" button is tapped
        findViewById<Button>(R.id.btnArView).setOnClickListener {
            startActivity(Intent(this, ArViewActivity::class.java))
        }


        btnCamera.setOnLongClickListener {
            takePhotoFullRes()
            true
        }

        // --- RecyclerView setup ---
        rv = findViewById(R.id.rvItems)
        rv.layoutManager = GridLayoutManager(this, 3)
        rv.setHasFixedSize(true)

        // Add spacing between grid items.
        rv.addItemDecoration(GridSpacingItemDecoration(3, dp(12), includeEdge = true))

        adapter = FurnitureAdapter()
        rv.adapter = adapter

        // --- Search bar ---
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

        // Update filtering as user types.
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

        // --- Navigation: menu + account avatar + filters ---
        findViewById<ImageButton>(R.id.btnHamburger).setOnClickListener {
            startActivity(Intent(this, SettingsActivity::class.java))
        }

        findViewById<ImageButton>(R.id.imgAvatar).setOnClickListener {
            startActivity(Intent(this, AccountSettingsActivity::class.java))
        }

        findViewById<ImageButton>(R.id.btnFilters).setOnClickListener {
            openFilterSheet()
        }

        // Load cached items from DB first (fast), then refresh from API (async).
        loadFromDbAndUpdateUI()
        refreshFromApi()
    }

//    override fun onResume() {
//        super.onResume()
//
//        try {
//            // If we don't have a session yet, try to create one
//            if (mSession == null) {
//                when (ArCoreApk.getInstance().requestInstall(this, mUserRequestedInstall)) {
//                    ArCoreApk.InstallStatus.INSTALLED -> {
//                        // Success: create session
//                        mSession = Session(this)
//                    }
//                    ArCoreApk.InstallStatus.INSTALL_REQUESTED -> {
//                        // ARCore will prompt the user to install/update Play Services for AR
//                        mUserRequestedInstall = false
//                        return
//                    }
//                }
//            }
//
//            // Resume the session if we have one
//            mSession?.resume()
//
//        } catch (e: UnavailableUserDeclinedInstallationException) {
//            Toast.makeText(this, "ARCore installation declined: $e", Toast.LENGTH_LONG).show()
//            return
//        } catch (e: UnavailableApkTooOldException) {
//            Toast.makeText(this, "Please update ARCore", Toast.LENGTH_LONG).show()
//            return
//        } catch (e: UnavailableSdkTooOldException) {
//            Toast.makeText(this, "Please update this app", Toast.LENGTH_LONG).show()
//            return
//        } catch (e: UnavailableDeviceNotCompatibleException) {
//            Toast.makeText(this, "Device not compatible with ARCore", Toast.LENGTH_LONG).show()
//            return
//        } catch (e: Exception) {
//            Toast.makeText(this, "ARCore error: $e", Toast.LENGTH_LONG).show()
//            return
//        }
//    }
//
//    override fun onPause() {
//        super.onPause()
//        mSession?.pause()
//    }

    private fun maybeEnableArButton() {
        ArCoreApk.getInstance().checkAvailabilityAsync(this) { availability ->
            val btnArView = findViewById<Button>(R.id.btnArView)
            if (availability.isSupported) {
                btnArView.visibility = View.VISIBLE
                btnArView.isEnabled = true
            } else { // The device is unsupported or unknown.
                btnArView.visibility = View.INVISIBLE
                btnArView.isEnabled = false
            }
        }
    }

    // Convert dp to px for spacing.
    private fun dp(v: Int): Int = (v * resources.displayMetrics.density).toInt()

    // Hide keyboard from search field.
    private fun hideKeyboard() {
        val imm =
            getSystemService(android.content.Context.INPUT_METHOD_SERVICE) as android.view.inputmethod.InputMethodManager
        imm.hideSoftInputFromWindow(searchInput.windowToken, 0)
    }

    // --- Camera: preview (low-res Bitmap) ---
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

    // --- Camera: full-res capture ---
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

    // --- Filter bottom sheet (price, colors, materials, sort) ---

    private fun openFilterSheet() {
        // if no items yet, don't open filters
        if (allItems.isEmpty()) {
            Toast.makeText(this, "Items are still loading, please try again.", Toast.LENGTH_SHORT).show()
            return
        }

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

        // Find views inside the bottom sheet layout.
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

        // Configure slider bounds from global min/max price.
        priceSlider.valueFrom = globalMinPrice.toFloat()
        priceSlider.valueTo = globalMaxPrice.toFloat()
        priceSlider.stepSize = 10f

        // Start slider values from current filter state, but clamped.
        val minV = maxOf(filters.minPrice, globalMinPrice).toFloat()
        val maxV = minOf(filters.maxPrice, globalMaxPrice).toFloat()
        priceSlider.values = listOf(minV, maxV)

        // Update price label whenever slider changes.
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

        // Helper to dynamically add Chips for colors/materials.
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

        // Populate chips from all available values.
        addChips(chipColors, allColors, filters.colors)
        addChips(chipMats, allMaterials, filters.materials)

        // Reflect current sort choice in the radio buttons.
        when (filters.sort) {
            SortMode.PRICE_ASC -> rbPriceAsc?.isChecked = true
            SortMode.PRICE_DESC -> rbPriceDesc?.isChecked = true
            SortMode.NAME_ASC -> rbNameAsc?.isChecked = true
        }

        // --- Reset button: clear filters back to defaults. ---
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

        // --- Apply button: read UI controls → update filters → refresh list. ---
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

                // Show how many filters are active.
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

    // Read checked chip texts from a ChipGroup.
    private fun groupCheckedTexts(group: ChipGroup): Set<String> =
        (0 until group.childCount)
            .map { group.getChildAt(it) as Chip }
            .filter { it.isChecked }
            .map { it.text.toString() }
            .toSet()

    // Count how many filters are currently active (for toast message).
    private fun getActiveFilterCount(): Int {
        if (allItems.isEmpty()) return 0  // nothing loaded yet

        var count = 0
        if (filters.minPrice > globalMinPrice || filters.maxPrice < globalMaxPrice) count++
        if (filters.colors.isNotEmpty()) count += filters.colors.size
        if (filters.materials.isNotEmpty()) count += filters.materials.size
        if (filters.sort != SortMode.PRICE_ASC) count++
        return count
    }

    // Apply search + filters + sort to allItems, then submit to adapter.
    private fun applyFiltersAndUpdate() {
        // Start from the full catalog
        var list = allItems.toList()

        // Search (name or tags) – works even with 1 character
        if (currentQuery.isNotBlank()) {
            val q = currentQuery.lowercase().trim()
            list = list.filter {
                it.name.lowercase().contains(q) ||
                        it.tags.any { t -> t.lowercase().contains(q) }
            }
        }

        // Price filter, colors, materials, sort stay the same
        list = list.filter { it.price in filters.minPrice..filters.maxPrice }

        if (filters.colors.isNotEmpty()) {
            list = list.filter { it.color in filters.colors }
        }

        if (filters.materials.isNotEmpty()) {
            list = list.filter { it.material in filters.materials }
        }

        list = when (filters.sort) {
            SortMode.PRICE_ASC  -> list.sortedBy { it.price }
            SortMode.PRICE_DESC -> list.sortedByDescending { it.price }
            SortMode.NAME_ASC   -> list.sortedBy { it.name.lowercase() }
        }

        adapter.submitList(list)

        // Empty state feedback if filtered list is empty.
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

        // Scroll back to top when new items are applied.
        if (list.isNotEmpty()) {
            rv.scrollToPosition(0)
        }
    }

    // Extension function to map DB entity -> UI FurnitureItem.
    private fun FurnitureEntity.toDomain(): FurnitureItem {
        return FurnitureItem(
            id = id,
            name = name,
            category = Category.valueOf(category),
            imageUrl = imageUrl,
            price = price,
            color = color,
            material = material,
            tags = tags?.split(",") ?: emptyList()
        )
    }

    // Load all furniture from DB, convert to domain model, update UI.
    private fun loadFromDbAndUpdateUI() {
        lifecycleScope.launch(Dispatchers.IO) {
            try{
                val entities = dao.getAll()
                Log.d("MainActivity", "Loaded ${entities.size} items from DB")
                val domain = entities.map { it.toDomain() }

                withContext(Dispatchers.Main) {
                    allItems = domain
                    applyFiltersAndUpdate()
                }
            } catch(e: Exception) {
                Log.e("MainActivity", "DB load failed", e)
            }

        }
    }

    // Fetch fresh furniture list from DummyJSON furniture category API.
    private fun refreshFromApi() {
        val url = "https://dummyjson.com/products/category/furniture"

        val request = com.android.volley.toolbox.StringRequest(
            com.android.volley.Request.Method.GET,
            url,
            { response ->
                // Parse JSON off the main thread.
                lifecycleScope.launch(Dispatchers.IO) {
                    val root = org.json.JSONObject(response)
                    val products = root.getJSONArray("products")
                    val entities = mutableListOf<FurnitureEntity>()

                    for (i in 0 until products.length()) {
                        val obj = products.getJSONObject(i)

                        val id = obj.getInt("id").toString()
                        val title = obj.getString("title")
                        val price = obj.getDouble("price").toInt()
                        val image = obj.getString("thumbnail")    // use thumbnail as imageUrl
//                        val description = obj.optString("description", "")

                        // Map API title -> internal Category enum for tabs.
                        val categoryEnum = when {
                            title.contains("table", ignoreCase = true) ||
                                    title.contains("desk", ignoreCase = true)  -> Category.TABLES

                            title.contains("chair", ignoreCase = true) ||
                                    title.contains("sofa", ignoreCase = true)  ||
                                    title.contains("stool", ignoreCase = true) -> Category.CHAIRS

                            else -> Category.DESKS
                        }

                        entities += FurnitureEntity(
                            id = id,
                            name = title,
                            price = price,
                            category = categoryEnum.name,
                            imageUrl = image,
                            color = "Unknown",
                            material = "Unknown",
                            tags = null // could parse description into tags in future
                        )
                    }

                    // Replace DB with latest furniture set
                    dao.deleteAll()
                    dao.insertAll(entities)

                    withContext(Dispatchers.Main) {
                        loadFromDbAndUpdateUI()
                    }
                }
            },
            // Handle network errors gracefully.
            {
                Toast.makeText(this, "API load failed", Toast.LENGTH_LONG).show()
            }
        )

        requestQueue.add(request)
    }

}