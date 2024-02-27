package com.tina.notes

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

class NotesActivity : AppCompatActivity() {
    private var db: DatabaseHandler = DatabaseHandler(this)
    var categories : ArrayList<Category> = ArrayList<Category>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_notes)

        categories = db.getAllCategories()

        val noNotes = findViewById<TextView>(R.id.noNotes)
        noNotes.visibility = View.GONE

        val rvNotes = findViewById<RecyclerView>(R.id.rvNotes)
        // Set the LayoutManager that RecycleView rvNotes will use
        rvNotes.layoutManager = LinearLayoutManager(this)
        // Set the adapter that RecycleView rvNotes will use
        rvNotes.adapter = NotesRecyclerAdapter(this, ArrayList<Note>())
        rvNotes.visibility = View.GONE

        val btnSearch = findViewById<Button>(R.id.btnSearch)
        btnSearch.setOnClickListener {
            val etKeywords = findViewById<EditText>(R.id.etKeywords)
            val keywords = etKeywords.text.toString()

            val spinnerCategory = findViewById<Spinner>(R.id.spinnerCategory)
            val selectedPosition = spinnerCategory.selectedItemPosition
            val selectedCatId = categories[selectedPosition].id

            refreshNoteList(keywords, selectedCatId)
        }

        val btnNewNote = findViewById<Button>(R.id.btnNewNote)
        btnNewNote.setOnClickListener {
            createNewNote()
        }

        val adapter = ArrayAdapter(this, R.layout.spinner_selected_category, categories.map {it.name})
        adapter.setDropDownViewResource(R.layout.spinner_dropdown_category)
        val spinnerCategory = findViewById<Spinner>(R.id.spinnerCategory)
        spinnerCategory.adapter = adapter

        spinnerCategory.onItemSelectedListener = object: AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: View, position: Int, id: Long) {
                // This function is called when an item is selected
                // 'position' is the position of the selected item in the Spinner list
                val selectedCat = categories[position]
                val selectedCatId = selectedCat.id

                val etKeywords = findViewById<EditText>(R.id.etKeywords)
                val keywords = etKeywords.text.toString()

                refreshNoteList(keywords, selectedCatId)
            }

            override fun onNothingSelected(parent: AdapterView<*>) {
                // This function is called when no item is selected
                Toast.makeText(parent.context, getString(R.string.no_cat_is_selected), Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun refreshNoteList(keywords: String, categoryId: Int) {
        val words = NoteActivity.splitKeywords(keywords)

        var categories : ArrayList<Category> = ArrayList<Category>()
        if (categoryId == DatabaseHandler.ROOT_PARENT) {
            categories = db.getAllCategories()
        } else {
            val category = db.getCategory(categoryId)
            if (category != null) {
                categories.add(category)
                // Add subcategories
                val childCategories = db.getCategories(category.id)
                childCategories.forEach {childCat ->
                    categories.add(Category(id = childCat.id, parentId = childCat.parentId, name = childCat.name))
                }
            }
        }

        var noteList: ArrayList<Note> = ArrayList<Note>()

        // Now, we need to find all note IDs that have one of the specified keywords (any of words in words[] in its 'keywords' string) and
        // belong to the category categoryID or its subcategories (note category is any of categories from categories[])
        // Also, we want to keep the order in which we found the note IDs
        // So, we will use LinkedHashSet (prevents duplicates and preserve insertion order)
        var noteIDs: LinkedHashSet<Int> = LinkedHashSet<Int>()
        for (category in categories) {
            if (words.isEmpty()) {
                noteIDs.addAll(db.getNotesId("", category.id))
            } else {
                for (word in words) {
                    noteIDs.addAll(db.getNotesId(word, category.id))
                }
            }
        }

        // Now, reads notes data (without content)
        var note: Note? = null
        for (id in noteIDs) {
            note = db.getNote(id, false) // false = no need to read note content
            if (note != null) {
                noteList.add(note)
            }
        }

        val rvNotes = findViewById<RecyclerView>(R.id.rvNotes)
        val noNotes = findViewById<TextView>(R.id.noNotes)
        if (noteList.isEmpty()) {
            noNotes.visibility = View.VISIBLE
            rvNotes.visibility = View.GONE
        } else {
            rvNotes.adapter = NotesRecyclerAdapter(this, noteList)
            noNotes.visibility = View.GONE
            rvNotes.visibility = View.VISIBLE
        }
    }

    private fun createNewNote() {
        val intent = Intent(this, NoteActivity::class.java).apply {
            putExtra(NoteActivity.NOTE_ID, 0) // Value 0 indicates "New note"

            val spinnerCategory = findViewById<Spinner>(R.id.spinnerCategory)
            // Note: because categories exists in Intent, we had to use this@NotesActivity.categories to reference to our attribute categories
            putExtra(NoteActivity.CATEGORY_ID, this@NotesActivity.categories[spinnerCategory.selectedItemPosition].id)
        }
        startActivity(intent)
    }
}