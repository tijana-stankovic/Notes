package com.tina.notes

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.ProgressBar
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

class NotesActivity : AppCompatActivity() {
    private var db: DatabaseHandler = DatabaseHandler(this)
    private var allCategories : ArrayList<Category> = ArrayList<Category>()
    private var searchJob : Job? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_notes)

        val etKeywords = findViewById<EditText>(R.id.etKeywords)
        val spinnerCategory = findViewById<Spinner>(R.id.spinnerCategory)
        val rvNotes = findViewById<RecyclerView>(R.id.rvNotes)
        val noNotes = findViewById<TextView>(R.id.noNotes)
        val btnSearch = findViewById<Button>(R.id.btnSearch)
        val btnNewNote = findViewById<Button>(R.id.btnNewNote)

        etKeywords.isEnabled = false
        spinnerCategory.isEnabled = false
        rvNotes.visibility = View.GONE
        noNotes.visibility = View.GONE
        btnSearch.isEnabled = false
        btnNewNote.isEnabled = false

        val currentContext = this
        lifecycleScope.launch {
            val progressBar = findViewById<ProgressBar>(R.id.progressBar)
            progressBar.visibility = View.VISIBLE

            allCategories = db.getAllCategories()

            // Set the LayoutManager that RecycleView rvNotes will use
            rvNotes.layoutManager = LinearLayoutManager(currentContext)
            // Set the adapter that RecycleView rvNotes will use
            rvNotes.adapter = NotesRecyclerAdapter(currentContext, ArrayList<Note>(), allCategories)

            val adapter = ArrayAdapter(currentContext, R.layout.spinner_selected_category, allCategories.map {it.name})
            adapter.setDropDownViewResource(R.layout.spinner_dropdown_category)
            spinnerCategory.adapter = adapter

            etKeywords.isEnabled = true
            spinnerCategory.isEnabled = true
            btnSearch.isEnabled = true
            btnNewNote.isEnabled = true
            progressBar.visibility = View.GONE

            spinnerCategory.onItemSelectedListener = object: AdapterView.OnItemSelectedListener {
                override fun onItemSelected(parent: AdapterView<*>, view: View, position: Int, id: Long) {
                    // This function is called when an item is selected
                    // 'position' is the position of the selected item in the Spinner list
                    val selectedCat = allCategories[position]
                    val selectedCatId = selectedCat.id

                    val keywords = etKeywords.text.toString()

                    refreshNoteList(keywords, selectedCatId)
                }

                override fun onNothingSelected(parent: AdapterView<*>) {
                    // This function is called when no item is selected
                    Toast.makeText(parent.context, getString(R.string.no_cat_is_selected), Toast.LENGTH_SHORT).show()
                }
            }
        }

        btnSearch.setOnClickListener {
            val etKeywords = findViewById<EditText>(R.id.etKeywords)
            val keywords = etKeywords.text.toString()

            val spinnerCategory = findViewById<Spinner>(R.id.spinnerCategory)
            val selectedPosition = spinnerCategory.selectedItemPosition
            val selectedCatId = allCategories[selectedPosition].id

            refreshNoteList(keywords, selectedCatId)
        }

        btnNewNote.setOnClickListener {
            createNewNote()
        }
    }

    suspend private fun getNotesFromDB(keywords: String, categoryId: Int) : ArrayList<Note> {
        val words = NoteActivity.splitKeywords(keywords)

        var categories: ArrayList<Category> = ArrayList<Category>()
        if (categoryId == DatabaseHandler.ROOT_PARENT) {
            categories = allCategories; //db.getAllCategories()
        } else {
            val category = db.getCategory(categoryId)
            if (category != null) {
                categories.add(category)
                // Add subcategories
                val childCategories = db.getCategories(category.id)
                childCategories.forEach { childCat ->
                    categories.add(
                        Category(
                            id = childCat.id,
                            parentId = childCat.parentId,
                            name = childCat.name
                        )
                    )
                }
            }
        }

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

        var noteList: ArrayList<Note> = ArrayList<Note>()

        // Now, reads notes data (without content)
        var note: Note? = null
        for (id in noteIDs) {
            note = db.getNote(id, false) // false = no need to read note content
            if (note != null) {
                noteList.add(note)
            }
        }

        return noteList
    }

    private fun refreshNoteList(keywords: String, categoryId: Int) {
        val currentContext = this
        searchJob?.cancel()  // cancel previous search job (if it is not yet finished)
        searchJob = lifecycleScope.launch {
            val progressBar = findViewById<ProgressBar>(R.id.progressBar)
            progressBar.visibility = View.VISIBLE

            var noteList: ArrayList<Note> = getNotesFromDB(keywords, categoryId)

            val rvNotes = findViewById<RecyclerView>(R.id.rvNotes)
            val noNotes = findViewById<TextView>(R.id.noNotes)
            if (noteList.isEmpty()) {
                noNotes.visibility = View.VISIBLE
                rvNotes.visibility = View.GONE
            } else {
                rvNotes.adapter = NotesRecyclerAdapter(currentContext, noteList, allCategories)
                noNotes.visibility = View.GONE
                rvNotes.visibility = View.VISIBLE
            }

            progressBar.visibility = View.GONE
        }
    }

    private fun createNewNote() {
        val intent = Intent(this, NoteActivity::class.java).apply {
            putExtra(NoteActivity.NOTE_ID, 0) // Value 0 indicates "New note"

            val spinnerCategory = findViewById<Spinner>(R.id.spinnerCategory)
            // Note: because categories exists in Intent, we had to use this@NotesActivity.categories to reference to our attribute categories
            putExtra(NoteActivity.CATEGORY_ID, this@NotesActivity.allCategories[spinnerCategory.selectedItemPosition].id)
        }
        startActivity(intent)
    }
}