package com.tina.notes

import android.content.Context
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.ProgressBar
import android.widget.Spinner
import android.widget.Toast
import androidx.lifecycle.lifecycleScope
import com.google.android.material.textfield.TextInputLayout
import kotlinx.coroutines.launch

class CategoryActivity : AppCompatActivity() {
    companion object {
        const val CATEGORY_ID = "categoryId"
        const val PARENT_ID = "parentId"
    }

    private var db: DatabaseHandler = DatabaseHandler(this)
    private var parents: ArrayList<Category> = ArrayList<Category>()
    private var category: Category = Category(0, 0, "")
    private var readOnlyMode: Boolean = true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_category)

        val etCatName = findViewById<EditText>(R.id.etCatName)
        val spinnerParentCategory = findViewById<Spinner>(R.id.spinnerParentCategory)
        val etParentCategory = findViewById<EditText>(R.id.etParentCategory)
        val btnCategoryDelete = findViewById<Button>(R.id.btnCategoryDelete)
        val btnCategoryEditSave = findViewById<Button>(R.id.btnCategoryEditSave)

        setReadOnly(etParentCategory, true) // this field is always read-only

        // initially, all others fields are read only
        switchToReadOnly(true)
        btnCategoryDelete.isEnabled = false
        btnCategoryEditSave.isEnabled = false

        val currentContext = this
        lifecycleScope.launch {
            val progressBar = findViewById<ProgressBar>(R.id.progressBar)
            progressBar.visibility = View.VISIBLE

            parents = db.getCategories(0)

            val categoryId = intent.getIntExtra(CategoryActivity.CATEGORY_ID, 0)
            if (categoryId != 0) {
                val dbCategory = db.getCategory(categoryId)
                if (dbCategory != null) {
                    category = dbCategory
                }
            }

            // for new category, get the default value for parent ID from parameter CategoryActivity.PARENT_ID
            val parentId = intent.getIntExtra(CategoryActivity.PARENT_ID, 0)
            if (category.id == 0) {
                category.parentId = parentId
            }

            etCatName.setText(category.name)

            val adapter = ArrayAdapter(currentContext, R.layout.spinner_selected_category, parents.map {it.name})
            adapter.setDropDownViewResource(R.layout.spinner_dropdown_category)
            spinnerParentCategory.adapter = adapter
            val parentIndex = parents.indexOfFirst { it.id == category.parentId }
            spinnerParentCategory.setSelection(parentIndex)

            etParentCategory.setText(parents[parentIndex].name)

            progressBar.visibility = View.GONE

            if (category.id == 0) {
                switchToReadOnly(false)
            } else {
                switchToReadOnly(true)
            }
            btnCategoryDelete.isEnabled = true
            btnCategoryEditSave.isEnabled = true
        }

        btnCategoryDelete.setOnClickListener {
            btnCategoryDelete.isEnabled = false
            btnCategoryEditSave.isEnabled = false
            lifecycleScope.launch {
                val progressBar = findViewById<ProgressBar>(R.id.progressBar)
                progressBar.visibility = View.VISIBLE

                val deleteOK: Boolean = deleteCategory()

                btnCategoryDelete.isEnabled = true
                btnCategoryEditSave.isEnabled = true
                progressBar.visibility = View.GONE

                if (deleteOK) {
                    finish()
                }
            }
        }

        btnCategoryEditSave.setOnClickListener {
            if (readOnlyMode) {
                switchToReadOnly(false)
            } else {
                if (etCatName.getText().toString() != "") {
                    btnCategoryDelete.isEnabled = false
                    btnCategoryEditSave.isEnabled = false
                    lifecycleScope.launch {
                        val progressBar = findViewById<ProgressBar>(R.id.progressBar)
                        progressBar.visibility = View.VISIBLE

                        saveCategory()
                        switchToReadOnly(true)

                        btnCategoryDelete.isEnabled = true
                        btnCategoryEditSave.isEnabled = true
                        progressBar.visibility = View.GONE
                    }
                } else {
                    etCatName.requestFocus()
                    // Show the keyboard
                    val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                    imm.showSoftInput(etCatName, InputMethodManager.SHOW_IMPLICIT)

                    Toast.makeText( this, getString(R.string.enter_name_mssg), Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    private fun switchToReadOnly(readOnly: Boolean) {
        val etCatName = findViewById<EditText>(R.id.etCatName)
        val spinnerParentCategory = findViewById<Spinner>(R.id.spinnerParentCategory)
        val ilParent = findViewById<TextInputLayout>(R.id.parentCatTextInputLayout)
        val btnCategoryEditSave = findViewById<Button>(R.id.btnCategoryEditSave)

        setReadOnly(etCatName, readOnly)

        if (readOnly) {
            ilParent.visibility = View.VISIBLE
            spinnerParentCategory.visibility = View.GONE
            btnCategoryEditSave.text = getString(R.string.note_edit)
            // Hide the keyboard
            val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            imm.hideSoftInputFromWindow(etCatName.windowToken, 0)
        } else {
            if (category.id == 0 || category.parentId != 0) { // parent change is possible only for new category and for subcategories
                ilParent.visibility = View.GONE
                spinnerParentCategory.visibility = View.VISIBLE
            }
            btnCategoryEditSave.text = getString(R.string.note_save)

            etCatName.requestFocus()
            // Show the keyboard
            val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            imm.showSoftInput(etCatName, InputMethodManager.SHOW_IMPLICIT)
        }

        readOnlyMode = readOnly
    }

    private fun setReadOnly(editText: EditText, readOnly: Boolean) {
        editText.isFocusable = !readOnly
        editText.isFocusableInTouchMode = !readOnly
        editText.isCursorVisible = !readOnly
    }

    suspend private fun saveCategory() {
        // Copy current spinner selected text to etCategory field
        val spinnerParentCategory = findViewById<Spinner>(R.id.spinnerParentCategory)
        val adapter = spinnerParentCategory.adapter as ArrayAdapter<*>
        val selectedText = adapter.getItem(spinnerParentCategory.selectedItemPosition).toString()
        val etParentCategory = findViewById<EditText>(R.id.etParentCategory)
        etParentCategory.setText(selectedText)

        // Check if the category still exists in db
        // if not, set id to 0, so new category will be created
        if (category.id != 0 && db.getCategory(category.id) == null) {
            category.id = 0
        }

        category.parentId = parents[spinnerParentCategory.selectedItemPosition].id

        val etCatName = findViewById<EditText>(R.id.etCatName)
        category.name = etCatName.getText().toString()

        category.id = db.saveCategory(category)

        if (category.id != 0) {
            Toast.makeText(this, getString(R.string.cat_is_saved) , Toast.LENGTH_LONG).show()
        } else {
            Toast.makeText(this, getString(R.string.cat_is_not_saved) , Toast.LENGTH_LONG).show()
        }
    }

    suspend private fun deleteCategory(): Boolean {
        var delOk: Boolean

        if (category.id != 0 && db.getCategory(category.id) != null) { // if the category exists in db
            delOk = db.deleteCategory(category.id, category.parentId)
        } else { // if this is a new category or category does not exists in the database anymore, then we simple set delOk = true
            delOk = true
        }

        if (delOk) {
            Toast.makeText(this, getString(R.string.cat_is_deleted), Toast.LENGTH_LONG).show()
        } else {
            Toast.makeText(this, getString(R.string.cat_is_not_deleted), Toast.LENGTH_LONG).show()
        }

        return delOk
    }
}

