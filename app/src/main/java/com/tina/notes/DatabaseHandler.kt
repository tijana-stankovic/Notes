package com.tina.notes

import android.annotation.SuppressLint
import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.SQLException
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import android.widget.Toast

class DatabaseHandler (val context: Context) : SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION){

    companion object {
        private const val DATABASE_NAME = "Notes"
        private const val DATABASE_VERSION = 1
        public const val ROOT_PARENT = 0
    }

    override fun onCreate(db: SQLiteDatabase?) {
        db?.execSQL("CREATE TABLE Note (" +
                                        "_id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                                        "cat_id INTEGER, " +
                                        "keywords TEXT, " +
                                        "title TEXT NOT NULL, " +
                                        "content TEXT" +
                                    ")")

        db?.execSQL("CREATE TABLE Category (" +
                                            "_id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                                            "parent_id INTEGER NOT NULL, " +
                                            "name TEXT NOT NULL" +
                                        ")")

        db?.execSQL("CREATE TABLE Keyword_Note (" +
                                                "keyword TEXT NOT NULL, " +
                                                "note_id INTEGER NOT NULL" +
                                            ")")

        db?.execSQL("INSERT INTO Category VALUES ($ROOT_PARENT, $ROOT_PARENT, 'All categories')")
    }

    override fun onUpgrade(db: SQLiteDatabase?, oldVersion: Int, newVersion: Int) {
        dropAllTables(db)
        onCreate(db)
    }

    override fun onDowngrade(db: SQLiteDatabase?, oldVersion: Int, newVersion: Int) {
        dropAllTables(db)
        onCreate(db)
    }

    private fun dropAllTables(db: SQLiteDatabase?) {
        db?.execSQL("DROP TABLE IF EXISTS Note")
        db?.execSQL("DROP TABLE IF EXISTS Category")
        db?.execSQL("DROP TABLE IF EXISTS Keyword_Note")
    }

    fun getAllCategories() : ArrayList<Category> {
        val catList: ArrayList<Category> = ArrayList<Category>()
        val catList0: ArrayList<Category> = getCategories(ROOT_PARENT)
        catList0.forEach {category ->
            catList.add(Category(id = category.id, parentId = category.parentId, name = category.name))
            if (category.id != ROOT_PARENT) {
                // Add subcategories
                val childCategories = getCategories(category.id)
                childCategories.forEach {childCat ->
                    catList.add(Category(id = childCat.id, parentId = childCat.parentId, name = "   / " + childCat.name))
                }
            }
        }
        return catList
    }

    fun getCategories(parentId: Int) : ArrayList<Category> {
        val catList: ArrayList<Category> = ArrayList<Category>()
        val db = this.readableDatabase
        val cursor = db.rawQuery("SELECT * FROM category WHERE parent_id = ? ORDER BY _id", arrayOf(parentId.toString()))

        var id: Int
        var parentId: Int
        var name: String

        if (cursor.moveToFirst()) {
            do {
                id = cursor.getInt(cursor.getColumnIndexOrThrow("_id"))
                parentId = cursor.getInt(cursor.getColumnIndexOrThrow("parent_id"))
                name = cursor.getString(cursor.getColumnIndexOrThrow("name"))

                catList.add(Category(id = id, parentId = parentId, name = name))
            } while (cursor.moveToNext())
        }

        cursor.close()

        return catList
    }

    fun getCategory(id: Int) : Category? {
        val db = this.readableDatabase
        val cursor = db.rawQuery("SELECT * FROM category WHERE _id = ?", arrayOf(id.toString()))

        var id: Int
        var parentId: Int
        var name: String
        var category: Category? = null

        if (cursor.moveToFirst()) {
            do {
                id = cursor.getInt(cursor.getColumnIndexOrThrow("_id"))
                parentId = cursor.getInt(cursor.getColumnIndexOrThrow("parent_id"))
                name = cursor.getString(cursor.getColumnIndexOrThrow("name"))

                category = Category(id = id, parentId = parentId, name = name)
            } while (cursor.moveToNext())
        }

        cursor.close()

        return category
    }

    fun getNotesId(keyword: String, categoryId: Int): ArrayList<Int> {
        val idList: ArrayList<Int> = ArrayList<Int>()
        val db = this.readableDatabase
        var cursor: Cursor
        if (keyword != "") {
            // By default, in SQLite, the LIKE operator is case-insensitive, so there is no need to deal with it
            cursor = db.rawQuery("""
                                    SELECT Note._id FROM Note
                                    INNER JOIN Keyword_Note ON Note._id = Keyword_Note.note_id
                                    WHERE Note.cat_id = ? AND Keyword_Note.keyword LIKE ?
                                    """, arrayOf(categoryId.toString(), "$keyword%"))
        } else {
            cursor = db.rawQuery("""
                                    SELECT Note._id FROM Note
                                    WHERE Note.cat_id = ? 
                                    """, arrayOf(categoryId.toString()))
        }

        var id: Int
        if (cursor.moveToFirst()) {
            do {
                id = cursor.getInt(cursor.getColumnIndexOrThrow("_id"))
                idList.add(id)
            } while (cursor.moveToNext())
        }

        cursor.close()

        return idList
    }

    fun getNote(id: Int, readContent: Boolean) : Note? {
        val db = this.readableDatabase
        var cursor: Cursor
        if (readContent) {
            cursor = db.rawQuery("SELECT * FROM note WHERE _id = ?", arrayOf(id.toString()))
        } else {
            cursor = db.rawQuery("SELECT _id, cat_id, keywords, title FROM note WHERE _id = ?", arrayOf(id.toString()))
        }

        var id: Int
        var catId: Int
        var keywords: String
        var title: String
        var content: String
        var note: Note? = null

        if (cursor.moveToFirst()) {
            do {
                id = cursor.getInt(cursor.getColumnIndexOrThrow("_id"))
                catId = cursor.getInt(cursor.getColumnIndexOrThrow("cat_id"))
                keywords = cursor.getString(cursor.getColumnIndexOrThrow("keywords"))
                title = cursor.getString(cursor.getColumnIndexOrThrow("title"))
                if (readContent) {
                    content = cursor.getString(cursor.getColumnIndexOrThrow("content"))
                } else {
                    content = ""
                }
                note = Note(id = id, catId = catId, keywords = keywords, title = title, content = content)
            } while (cursor.moveToNext())
        }

        cursor.close()

        return note
    }

    fun saveNote(note: Note): Int {
        val db = this.writableDatabase
        var noteId: Int

        val values = ContentValues().apply {
            put("cat_id", note.catId)
            put("keywords", note.keywords)
            put("title", note.title)
            put("content", note.content)
        }

        if (note.id == 0) {
            // Insert new note
            val newRowId = db.insert("note", null, values)
            if (newRowId > 0) {
                noteId = newRowId.toInt()
            } else {
                noteId = 0
            }
        } else {
            val numOfUpdRows = db.update("note", values, "_id = ?", arrayOf(note.id.toString()))
            if (numOfUpdRows > 0) {
                noteId = note.id
            } else {
                noteId = 0
            }
        }

        return noteId
    }

    fun saveCategory(category: Category): Int {
        val db = this.writableDatabase
        var categoryId: Int

        val values = ContentValues().apply {
            put("parent_id", category.parentId)
            put("name", category.name)
        }

        if (category.id == 0) {
            // Insert new category
            val newRowId = db.insert("category", null, values)
            if (newRowId > 0) {
                categoryId = newRowId.toInt()
            } else {
                categoryId = 0
            }
        } else {
            val numOfUpdRows = db.update("category", values, "_id = ?", arrayOf(category.id.toString()))
            if (numOfUpdRows > 0) {
                categoryId = category.id
            } else {
                categoryId = 0
            }
        }

        return categoryId
    }

    fun deleteNote(noteId: Int): Boolean {
        val db = this.writableDatabase
        var delOk : Boolean
        val numOfDelRows = db.delete("note", "_id = ?", arrayOf(noteId.toString()))
        if (numOfDelRows > 0) {
            delOk = true
        } else {
            delOk = false
        }
        return delOk
    }

    fun deleteCategory(categoryId: Int): Boolean {
        val db = this.writableDatabase
        var delOk : Boolean
        val numOfDelRows = db.delete("category", "_id = ?", arrayOf(categoryId.toString()))
        if (numOfDelRows > 0) {
            delOk = true
        } else {
            delOk = false
        }
        return delOk
    }

    fun insertNoteKeywords(noteId: Int, keywords: List<String>) {
        val db = this.writableDatabase

        for (keyword in keywords) {
            val values = ContentValues().apply {
                put("keyword", keyword)
                put("note_id", noteId)
            }
            db.insert("keyword_note", null, values)
        }
    }

    fun deleteNoteKeywords(noteId: Int): Boolean {
        val db = this.writableDatabase
        var delOk : Boolean
        val numOfDelRows = db.delete("keyword_note", "note_id = ?", arrayOf(noteId.toString()))
        if (numOfDelRows > 0) {
            delOk = true
        } else {
            delOk = false
        }
        return delOk
    }

    fun changeNotesCategory(fromCatId: Int, toCatId: Int) {
        val db = this.writableDatabase
        val values = ContentValues().apply {
            put("cat_id", toCatId)
        }

        db.update("note", values, "cat_id = ?", arrayOf(fromCatId.toString()))
    }

    fun changeCategoriesParent(fromParentId: Int, toParentId: Int) {
        val db = this.writableDatabase
        val values = ContentValues().apply {
            put("parent_id", toParentId)
        }

        db.update("category", values, "parent_id = ?", arrayOf(fromParentId.toString()))
    }
}