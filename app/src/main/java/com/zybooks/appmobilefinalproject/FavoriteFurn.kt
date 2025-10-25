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
import com.google.android.material.appbar.MaterialToolbar
import android.widget.ImageView
import com.google.android.material.textfield.TextInputEditText
import android.text.Editable
import android.text.TextWatcher
import android.view.inputmethod.EditorInfo
import android.view.View
import com.zybooks.appmobilefinalproject.FavoriteFurn.FilterState

class FavoriteFurn : AppCompatActivity(R.layout.fav_furn) {

    enum class Category { TABLES, CHAIRS, DESKS }
    enum class SortMode { PRICE_ASC, PRICE_DESC, NAME_ASC }

    data class FavFurnitureItem(
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
    private lateinit var adapter: FavFurnAdapter
    private lateinit var tabLayout: TabLayout
    private lateinit var searchView: androidx.appcompat.widget.SearchView

    private val allItems = listOf(
        FavFurnitureItem(
            "t1",
            "Oak Table",
            Category.TABLES,
            R.drawable.placeholdertile,
            300,
            "Brown",
            "Wood",
            listOf("dining", "family")
        ),
        FavFurnitureItem(
            "t2",
            "Glass Table",
            Category.TABLES,
            R.drawable.placeholdertile,
            450,
            "Transparent",
            "Glass",
            listOf("modern")
        ),
        FavFurnitureItem(
            "t3",
            "Marble Table",
            Category.TABLES,
            R.drawable.placeholdertile,
            850,
            "White",
            "Stone",
            listOf("luxury", "dining")
        ),
        FavFurnitureItem(
            "c1",
            "Dining Chair",
            Category.CHAIRS,
            R.drawable.placeholdertile,
            80,
            "Brown",
            "Wood",
            listOf("set", "dining")
        ),
        FavFurnitureItem(
            "c2",
            "Arm Chair",
            Category.CHAIRS,
            R.drawable.placeholdertile,
            160,
            "Blue",
            "Fabric",
            listOf("cozy")
        ),
        FavFurnitureItem(
            "c3",
            "Office Chair",
            Category.CHAIRS,
            R.drawable.placeholdertile,
            220,
            "Black",
            "Leather",
            listOf("office", "ergonomic")
        ),
        FavFurnitureItem(
            "d1",
            "Standing Desk",
            Category.DESKS,
            R.drawable.placeholdertile,
            520,
            "Black",
            "Metal",
            listOf("office")
        ),
        FavFurnitureItem(
            "d2",
            "Corner Desk",
            Category.DESKS,
            R.drawable.placeholdertile,
            430,
            "White",
            "Wood",
            listOf("home")
        ),
        FavFurnitureItem(
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

        // RecyclerView
        rv = findViewById(R.id.rvItems)
        rv.layoutManager = GridLayoutManager(this, 3)
        rv.setHasFixedSize(true)
        rv.addItemDecoration(GridSpacingItemDecoration(3, dp(12), includeEdge = true))

        adapter = FavFurnAdapter()
        rv.adapter = adapter

        // Tabs
        tabLayout = findViewById(R.id.tabCategories)
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
        searchView = findViewById(R.id.searchView)

        searchView.setOnQueryTextListener(object : androidx.appcompat.widget.SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                currentQuery = query.orEmpty()
                applyFiltersAndUpdate()
                hideKeyboard()
                return true
            }
            override fun onQueryTextChange(newText: String?): Boolean {
                currentQuery = newText.orEmpty()
                applyFiltersAndUpdate()
                return true
            }
        })



        // Handle typing and the search action directly inside the SearchView
        searchView.setOnQueryTextListener(object : androidx.appcompat.widget.SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                currentQuery = query.orEmpty()
                applyFiltersAndUpdate()
                hideKeyboard()
                return true
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                currentQuery = newText.orEmpty()
                applyFiltersAndUpdate()
                return true
            }
        })


        // Toolbar back arrow
        findViewById<MaterialToolbar>(R.id.toolbar)?.setNavigationOnClickListener {
            finish()
        }

        // imgAvatar
        findViewById<ImageView>(R.id.imgAvatar)?.setOnClickListener {
            startActivity(Intent(this, AccountSettingsActivity::class.java))
        }

        // These views do NOT exist in fav_furn.xml; guard them so they don't crash
        findViewById<ImageButton?>(R.id.btnHamburger)?.setOnClickListener {
            startActivity(Intent(this, SettingsActivity::class.java))
        }

        findViewById<ImageButton?>(R.id.btnFilters)?.setOnClickListener {
            openFilterSheet()
        }
    }

    private fun dp(v: Int): Int = (v * resources.displayMetrics.density).toInt()

    private fun hideKeyboard() {
        val imm = getSystemService(android.content.Context.INPUT_METHOD_SERVICE)
                as android.view.inputmethod.InputMethodManager
        imm.hideSoftInputFromWindow(searchView.windowToken, 0)
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
            ?: run { Toast.makeText(this, "priceSlider missing", Toast.LENGTH_SHORT).show(); return }
        val tvPriceRange = view.findViewById<TextView>(R.id.tvPriceRange)
        val chipColors  = view.findViewById<ChipGroup>(R.id.chipColors)
            ?: run { Toast.makeText(this, "chipColors missing", Toast.LENGTH_SHORT).show(); return }
        val chipMats    = view.findViewById<ChipGroup>(R.id.chipMaterials)
            ?: run { Toast.makeText(this, "chipMaterials missing", Toast.LENGTH_SHORT).show(); return }
        val rgSort      = view.findViewById<RadioGroup>(R.id.rgSort)
            ?: run { Toast.makeText(this, "rgSort missing", Toast.LENGTH_SHORT).show(); return }
        val rbPriceAsc  = view.findViewById<RadioButton>(R.id.sortPriceAsc)
        val rbPriceDesc = view.findViewById<RadioButton>(R.id.sortPriceDesc)
        val rbNameAsc   = view.findViewById<RadioButton>(R.id.sortNameAsc)

        // Init slider
        priceSlider.valueFrom = globalMinPrice.toFloat()
        priceSlider.valueTo   = globalMaxPrice.toFloat()
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
            FavoriteFurn.SortMode.PRICE_ASC  -> rbPriceAsc?.isChecked = true
            FavoriteFurn.SortMode.PRICE_DESC -> rbPriceDesc?.isChecked = true
            FavoriteFurn.SortMode.NAME_ASC   -> rbNameAsc?.isChecked = true
        }

        // Apply button
        view.findViewById<com.google.android.material.button.MaterialButton>(R.id.btnApply)
            ?.setOnClickListener {
                val values = priceSlider.values
                val selectedColors = groupCheckedTexts(chipColors)
                val selectedMats   = groupCheckedTexts(chipMats)
                val sort = when (rgSort.checkedRadioButtonId) {
                    R.id.sortPriceDesc -> FavoriteFurn.SortMode.PRICE_DESC
                    R.id.sortNameAsc   -> FavoriteFurn.SortMode.NAME_ASC
                    else               -> FavoriteFurn.SortMode.PRICE_ASC
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
        if (filters.sort != FavoriteFurn.SortMode.PRICE_ASC) count++
        return count
    }

    private fun applyFiltersAndUpdate() {
        var furnlist = allItems.filter { it.category == currentCategory }

        // Search (name or tags)
        if (currentQuery.isNotBlank()) {
            val q = currentQuery.lowercase().trim()
            furnlist = furnlist.filter {
                it.name.lowercase().contains(q) ||
                        it.tags.any { t -> t.lowercase().contains(q) }
            }
        }

        // Price
        furnlist = furnlist.filter { it.price in filters.minPrice..filters.maxPrice }

        // Colors
        if (filters.colors.isNotEmpty()) {
            furnlist = furnlist.filter { it.color in filters.colors }
        }

        // Materials
        if (filters.materials.isNotEmpty()) {
            furnlist = furnlist.filter { it.material in filters.materials }
        }

        // Sort
        furnlist = when (filters.sort) {
            FavoriteFurn.SortMode.PRICE_ASC  -> furnlist.sortedBy { it.price }
            FavoriteFurn.SortMode.PRICE_DESC -> furnlist.sortedByDescending { it.price }
            FavoriteFurn.SortMode.NAME_ASC   -> furnlist.sortedBy { it.name.lowercase() }
        }

        adapter.submitList(furnlist)


        // Show empty state feedback
        if (furnlist.isEmpty()) {
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

        if (furnlist.isNotEmpty()) {
            rv.scrollToPosition(0)
        }
    }


    private fun showCategory(cat: FavoriteFurn.Category) {
        currentCategory = cat
        applyFiltersAndUpdate()
    }

}