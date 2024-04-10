package com.tina.notes

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class NotesViewModel(private val context: Context) : ViewModel() {
    private var db: DatabaseHandler = DatabaseHandler(context)
    private var searchJob : Job? = null

    private var _allCategories = MutableLiveData<ArrayList<Category>>()
    val allCategories : LiveData<ArrayList<Category>>
        get() = _allCategories

    private var _noteList = MutableLiveData<ArrayList<Note>>()
    val noteList : LiveData<ArrayList<Note>>
        get() = _noteList

    var spinnerLastPosition : Int = -1

    fun readCategories() {
        if (allCategories.value == null) {
            viewModelScope.launch {
                _allCategories.value = db.getAllCategories()
            }
        }
    }

    fun allCategories(): ArrayList<Category> {
        return allCategories.value ?: ArrayList<Category>()
    }

    fun noteList(): ArrayList<Note> {
        return noteList.value ?: ArrayList<Note>()
    }

    fun searchNotes(keywords: String, categoryId: Int) {
        searchJob?.cancel()  // cancel previous search job (if it is not yet finished)
        searchJob = viewModelScope.launch {
            _noteList.value = getNotesFromDB(keywords, categoryId)
        }
    }

    suspend private fun getNotesFromDB(keywords: String, categoryId: Int) : ArrayList<Note> {
        val words = NoteActivity.splitKeywords(keywords)

        var categories: ArrayList<Category> = ArrayList<Category>()
        if (categoryId == DatabaseHandler.ROOT_PARENT) {
            categories = allCategories()
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
        var note: Note?
        for (id in noteIDs) {
            note = db.getNote(id, false) // false = no need to read note content
            if (note != null) {
                noteList.add(note)
            }
        }

        return noteList
    }
}