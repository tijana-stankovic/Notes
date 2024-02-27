package com.tina.notes

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

class CategoriesActivity : AppCompatActivity() {
    private var db: DatabaseHandler = DatabaseHandler(this)
    var parents : ArrayList<Category> = ArrayList<Category>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_categories)

        val noSub = findViewById<TextView>(R.id.noSubcategories)
        noSub.visibility = View.GONE

        val rvCat = findViewById<RecyclerView>(R.id.rvCategories)
        // Set the LayoutManager that RecycleView rvCat will use
        rvCat.layoutManager = LinearLayoutManager(this)
        // Set the adapter that RecycleView rvCat will use
        rvCat.adapter = CategoriesRecyclerAdapter(this, ArrayList<Category>())
        rvCat.visibility = View.GONE

        parents = db.getCategories(0)
        val adapter = ArrayAdapter(this, R.layout.spinner_selected_category, parents.map {it.name})
        adapter.setDropDownViewResource(R.layout.spinner_dropdown_category)
        val spinnerParentCategory = findViewById<Spinner>(R.id.spinnerParentCategory)
        spinnerParentCategory.adapter = adapter

        spinnerParentCategory.onItemSelectedListener = object: AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: View, position: Int, id: Long) {
                // This function is called when an item is selected
                // 'position' is the position of the selected item in the Spinner list
                val selectedParent = parents[position]
                val selectedParentId = selectedParent.id
                refreshCategoryList(selectedParentId)
            }

            override fun onNothingSelected(parent: AdapterView<*>) {
                // This function is called when no item is selected
                Toast.makeText(parent.context, getString(R.string.no_cat_is_selected), Toast.LENGTH_SHORT).show()
            }
        }

        val btnNewCategory = findViewById<Button>(R.id.btnNewCategory)
        btnNewCategory.setOnClickListener {
            createNewCategory()
        }
    }

    private fun refreshCategoryList(selectedParentId: Int) {
        val rvCat = findViewById<RecyclerView>(R.id.rvCategories)
        val noSub = findViewById<TextView>(R.id.noSubcategories)
        val catList = db.getCategories(selectedParentId)
        if (catList.isEmpty()) {
            noSub.visibility = View.VISIBLE
            rvCat.visibility = View.GONE
        } else {
            rvCat.adapter = CategoriesRecyclerAdapter(this, catList)
            noSub.visibility = View.GONE
            rvCat.visibility = View.VISIBLE
        }
    }

    private fun createNewCategory() {
        val intent = Intent(this, CategoryActivity::class.java).apply {
            putExtra(CategoryActivity.CATEGORY_ID, 0) // Value 0 indicates "New category"
            val spinnerParentCategory = findViewById<Spinner>(R.id.spinnerParentCategory)
            putExtra(CategoryActivity.PARENT_ID, parents[spinnerParentCategory.selectedItemPosition].id)
        }
        startActivity(intent)
    }
}