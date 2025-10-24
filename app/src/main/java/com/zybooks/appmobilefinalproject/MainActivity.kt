package com.zybooks.appmobilefinalproject

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.tabs.TabLayout
import android.widget.ImageButton
import android.content.Intent

class MainActivity : AppCompatActivity(R.layout.activity_main) {

    enum class Category { TABLES, CHAIRS, DESKS }

    data class FurnitureItem(
        val id: String,
        val name: String,
        val category: Category,
        val imageRes: Int
    )

    private lateinit var rv: RecyclerView
    private lateinit var adapter: FurnitureAdapter
    private lateinit var tabLayout: TabLayout

    // TODO replace with your real data/images
    private val allItems = listOf(
        FurnitureItem("t1", "Oak Table", Category.TABLES, R.drawable.placeholdertile),
        FurnitureItem("t2", "Glass Table", Category.TABLES, R.drawable.placeholdertile),
        FurnitureItem("c1", "Dining Chair", Category.CHAIRS, R.drawable.placeholdertile),
        FurnitureItem("c2", "Arm Chair", Category.CHAIRS, R.drawable.placeholdertile),
        FurnitureItem("d1", "Standing Desk", Category.DESKS, R.drawable.placeholdertile),
        FurnitureItem("d2", "Corner Desk", Category.DESKS, R.drawable.placeholdertile),
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // RecyclerView
        rv = findViewById(R.id.rvItems)
        rv.layoutManager = GridLayoutManager(this, 3) // 3-column grid
        rv.setHasFixedSize(true)
        rv.addItemDecoration(GridSpacingItemDecoration(3, dp(12), includeEdge = true))

        adapter = FurnitureAdapter()
        rv.adapter = adapter

        // Tabs (already defined via TabItems in XML)
        tabLayout = findViewById(R.id.tabLayout)

        // Initial category = Tables (index 0)
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
        // Open Settings when top-left button is tapped
        findViewById<ImageButton>(R.id.btnHamburger).setOnClickListener {
            startActivity(Intent(this, SettingsActivity::class.java))
        }

        val avatar = findViewById<ImageButton>(R.id.imgAvatar)
        avatar.setOnClickListener {
            // Sanity log to confirm click is firing
            // Log.d("MainActivity", "Avatar clicked")
            startActivity(Intent(this, AccountSettingsActivity::class.java))
        }
    }

    private fun showCategory(cat: Category) {
        val filtered = allItems.filter { it.category == cat }
        adapter.submitList(filtered)
        rv.scrollToPosition(0)
    }

    // dp helper
    private fun dp(v: Int): Int = (v * resources.displayMetrics.density).toInt()
}
