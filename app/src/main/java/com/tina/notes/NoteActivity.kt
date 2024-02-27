package com.tina.notes

import android.content.Context
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.text.InputType
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import com.google.android.material.textfield.TextInputLayout

class NoteActivity : AppCompatActivity() {
    companion object {
        const val NOTE_ID = "noteId"
        const val CATEGORY_ID = "categoryId"
        public fun splitKeywords(keywords: String): List<String> {
            // Regular expression for splitting on non-alphanumeric characters:
            // \\w: This is a character class representing alphanumeric characters (a-z, A-Z, 0-9, and the underscore _).
            // [^\\w]+  -> array of one or more non-alphanumeric characters
            val pattern = Regex("[^\\w]+")

            // Split the 'keywords' string into words (on non-alphanumeric characters), removing empty elements:
            return keywords.trim().split(pattern).filter { it.isNotEmpty() }
        }
    }

    private var db: DatabaseHandler = DatabaseHandler(this)
    private var categories: ArrayList<Category> = ArrayList<Category>()
    private var note: Note = Note(0, 0, "", "", "")
    private var readOnlyMode: Boolean = true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_note)

        categories = db.getAllCategories()

        val noteId = intent.getIntExtra(NoteActivity.NOTE_ID, 0)
        if (noteId != 0) {
            val dbNote = db.getNote(noteId, true)
            if (dbNote != null) {
                note = dbNote
            }
        }

        // for new note, get the default value for category ID from parameter NoteActivity.CATEGORY_ID
        val categoryId = intent.getIntExtra(NoteActivity.CATEGORY_ID, 0)
        if (note.id == 0) {
            note.catId = categoryId
        }

        val etTitle = findViewById<EditText>(R.id.etTitle)
        etTitle.setText(note.title)

        val etKeywords = findViewById<EditText>(R.id.etKeywords)
        etKeywords.setText(note.keywords)

        val etContent = findViewById<EditText>(R.id.etContent)
        etContent.setText(note.content)

        val adapter =
            ArrayAdapter(this, R.layout.spinner_selected_category, categories.map { it.name })
        adapter.setDropDownViewResource(R.layout.spinner_dropdown_category)
        val spinnerCategory = findViewById<Spinner>(R.id.spinnerCategory)
        spinnerCategory.adapter = adapter
        val categoryIndex = categories.indexOfFirst { it.id == note.catId }
        spinnerCategory.setSelection(categoryIndex)

        val etCategory = findViewById<EditText>(R.id.etCategory)
        etCategory.setText(categories[categoryIndex].name)
        setReadOnly(etCategory, true) // this field is always read-only

        if (note.id == 0) {
            switchToReadOnly(false)
        } else {
            switchToReadOnly(true)
        }

        val btnDelete = findViewById<Button>(R.id.btnDelete)
        btnDelete.setOnClickListener {
            if (deleteNote()) {
                finish()
            }
        }

        val btnEditSave = findViewById<Button>(R.id.btnEditSave)
        btnEditSave.setOnClickListener {
            if (readOnlyMode) {
                switchToReadOnly(false)
            } else {
                if (etTitle.getText().toString() != "") {
                    saveNote()
                    switchToReadOnly(true)
                } else {
                    etTitle.requestFocus()
                    // Show the keyboard
                    val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                    imm.showSoftInput(etTitle, InputMethodManager.SHOW_IMPLICIT)

                    Toast.makeText( this, getString(R.string.enter_title_mssg), Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    private fun switchToReadOnly(readOnly: Boolean) {
        val etTitle = findViewById<EditText>(R.id.etTitle)
        val etKeywords = findViewById<EditText>(R.id.etKeywords)
        val etContent = findViewById<EditText>(R.id.etContent)
        val spinnerCategory = findViewById<Spinner>(R.id.spinnerCategory)
        val ilCategory = findViewById<TextInputLayout>(R.id.categoryTextInputLayout)
        val btnEditSave = findViewById<Button>(R.id.btnEditSave)

        // NOTE: this line must be executed BEFORE setting fields to readonly
        // otherwise, 'this.currentFocus' will return null and the keyboard will remain visible
        val viewWithFocus = this.currentFocus

        setReadOnly(etTitle, readOnly)
        setReadOnly(etKeywords, readOnly)
        setReadOnly(etContent, readOnly)

        if (readOnly) {
            ilCategory.visibility = View.VISIBLE
            spinnerCategory.visibility = View.GONE
            btnEditSave.text = getString(R.string.note_edit)
            // Hide the keyboard
            val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            if (viewWithFocus != null) {
                imm.hideSoftInputFromWindow(viewWithFocus.windowToken, 0)
            }
        } else {
            ilCategory.visibility = View.GONE
            spinnerCategory.visibility = View.VISIBLE
            btnEditSave.text = getString(R.string.note_save)

            var gotoField: EditText = etContent
            if (etTitle.text.toString() == "") {
                gotoField = etTitle
            } else if (etKeywords.text.toString() == "") {
                gotoField = etKeywords
            }
            // Goto first empty field (or content field if there is no empty fields)
            gotoField.requestFocus()
            // Show the keyboard
            val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            imm.showSoftInput(gotoField, InputMethodManager.SHOW_IMPLICIT)
        }

        readOnlyMode = readOnly
    }

    private fun setReadOnly(editText: EditText, readOnly: Boolean) {
        editText.isFocusable = !readOnly
        editText.isFocusableInTouchMode = !readOnly
        editText.isCursorVisible = !readOnly
    }

    private fun saveNote() {
        // Copy current spinner selected text to etCategory field
        val spinnerCategory = findViewById<Spinner>(R.id.spinnerCategory)
        val adapter = spinnerCategory.adapter as ArrayAdapter<*>
        val selectedText = adapter.getItem(spinnerCategory.selectedItemPosition).toString()
        val etCategory = findViewById<EditText>(R.id.etCategory)
        etCategory.setText(selectedText)

        // Check if the note still exists in db
        // if not, set id to 0, so new note will be created
        if (note.id != 0 && db.getNote(note.id, false) == null) {
            note.id = 0
        }

        note.catId = categories[spinnerCategory.selectedItemPosition].id

        val etTitle = findViewById<EditText>(R.id.etTitle)
        val etKeywords = findViewById<EditText>(R.id.etKeywords)
        val etContent = findViewById<EditText>(R.id.etContent)
        note.title = etTitle.getText().toString()
        note.keywords = etKeywords.getText().toString()
        note.content = etContent.getText().toString()

        note.id = db.saveNote(note)

        if (note.id != 0) {
            db.deleteNoteKeywords(note.id)
            val words = splitKeywords(note.keywords)
            db.insertNoteKeywords(note.id, words)
            Toast.makeText(this, getString(R.string.note_is_saved) , Toast.LENGTH_LONG).show()
        } else {
            Toast.makeText(this, getString(R.string.note_is_not_saved) , Toast.LENGTH_LONG).show()
        }
    }

    private fun deleteNote(): Boolean {
        val delOk: Boolean

        if (note.id != 0 && db.getNote(note.id, false) != null) { // if the note exists in db
            delOk = db.deleteNote(note.id)
            db.deleteNoteKeywords(note.id)
        } else { // if this is a new note or note does not exists in the database anymore, then we simple set delOk = true
            delOk = true
        }

        if (delOk) {
            Toast.makeText(this, getString(R.string.note_is_deleted) , Toast.LENGTH_LONG).show()
        } else {
            Toast.makeText(this, getString(R.string.note_is_not_deleted) , Toast.LENGTH_LONG).show()
        }

        return delOk
    }
}