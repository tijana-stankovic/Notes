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
import androidx.activity.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

class NotesActivity : AppCompatActivity(), AdapterView.OnItemSelectedListener {
    private val viewModel: NotesViewModel by viewModels {
        NotesViewModelFactory(applicationContext)
    }

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

        val progressBar = findViewById<ProgressBar>(R.id.progressBar)
        progressBar.visibility = View.VISIBLE

        viewModel.readCategories()

        val currentContext = this

        viewModel.allCategories.observe(this) { allCategories ->
            // Set the LayoutManager that RecycleView rvNotes will use
            rvNotes.layoutManager = LinearLayoutManager(currentContext)
            // Set the adapter that RecycleView rvNotes will use
            rvNotes.adapter = NotesRecyclerAdapter(currentContext, ArrayList<Note>(), allCategories)

            val adapter = ArrayAdapter(currentContext, R.layout.spinner_selected_category, allCategories.map {it.name})
            adapter.setDropDownViewResource(R.layout.spinner_dropdown_category)
            spinnerCategory.adapter = adapter

            spinnerCategory.onItemSelectedListener = this

            etKeywords.isEnabled = true
            spinnerCategory.isEnabled = true
            btnSearch.isEnabled = true
            btnNewNote.isEnabled = true
            progressBar.visibility = View.GONE
        }

        viewModel.noteList.observe(this) { noteList ->
            val rvNotes = findViewById<RecyclerView>(R.id.rvNotes)
            val noNotes = findViewById<TextView>(R.id.noNotes)
            if (noteList.isEmpty()) {
                noNotes.visibility = View.VISIBLE
                rvNotes.visibility = View.GONE
            } else {
                rvNotes.adapter = NotesRecyclerAdapter(currentContext, noteList, viewModel.allCategories())
                noNotes.visibility = View.GONE
                rvNotes.visibility = View.VISIBLE
            }

            progressBar.visibility = View.GONE
        }

        btnSearch.setOnClickListener {
            refreshNoteList()
        }

        btnNewNote.setOnClickListener {
            createNewNote()
        }
    }

    // This function is called when an spinner item is selected
    override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
        // When activity is recreated (e.g. during screen rotation) this event will be triggered
        // which is unnecessary (because spinner value is not really changed)
        // So, this if prevent unnecessary refreshNoteList() call
        if (viewModel.spinnerLastPosition != position) {
            viewModel.spinnerLastPosition = position
            refreshNoteList()
        }
    }

    override fun onNothingSelected(parent: AdapterView<*>?) {
        // This function is called when no item is selected
        Toast.makeText(this, getString(R.string.no_cat_is_selected), Toast.LENGTH_SHORT).show()
    }

    private fun refreshNoteList() {
        val progressBar = findViewById<ProgressBar>(R.id.progressBar)
        progressBar.visibility = View.VISIBLE

        val etKeywords = findViewById<EditText>(R.id.etKeywords)
        val keywords = etKeywords.text.toString()

        val spinnerCategory = findViewById<Spinner>(R.id.spinnerCategory)
        val selectedPosition = spinnerCategory.selectedItemPosition
        val selectedCatId = viewModel.allCategories()[selectedPosition].id

        viewModel.searchNotes(keywords, selectedCatId)
    }

    private fun createNewNote() {
        val intent = Intent(this, NoteActivity::class.java).apply {
            putExtra(NoteActivity.NOTE_ID, 0) // Value 0 indicates "New note"

            val spinnerCategory = findViewById<Spinner>(R.id.spinnerCategory)
            // Note: because we are in Intent, we had to use this@NotesActivity.viewModel to reference to viewModel
            putExtra(NoteActivity.CATEGORY_ID, this@NotesActivity.viewModel.allCategories()[spinnerCategory.selectedItemPosition].id)
        }
        startActivity(intent)
    }
}