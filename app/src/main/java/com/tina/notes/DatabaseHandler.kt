package com.tina.notes

import android.annotation.SuppressLint
import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.SQLException
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import android.widget.Toast
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext

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

    suspend fun getAllCategories() : ArrayList<Category> = withContext(Dispatchers.IO) {
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
        return@withContext catList
    }

    suspend fun getCategories(parentId: Int) : ArrayList<Category> = withContext(Dispatchers.IO) {
        val catList: ArrayList<Category> = ArrayList<Category>()
        val db = this@DatabaseHandler.readableDatabase
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

        return@withContext catList
    }

    suspend fun getCategory(id: Int) : Category? = withContext(Dispatchers.IO) {
        val db = this@DatabaseHandler.readableDatabase
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

        return@withContext category
    }

    suspend fun getNotesId(keyword: String, categoryId: Int): ArrayList<Int> = withContext(Dispatchers.IO) {
        val idList: ArrayList<Int> = ArrayList<Int>()
        val db = this@DatabaseHandler.readableDatabase
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

        return@withContext idList
    }

    suspend fun getNote(id: Int, readContent: Boolean) : Note? = withContext(Dispatchers.IO) {
        val db = this@DatabaseHandler.readableDatabase
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

        return@withContext note
    }

    suspend fun saveNote(note: Note, keywords: List<String>): Int = withContext(Dispatchers.IO) {
        val db = this@DatabaseHandler.writableDatabase
        var noteId: Int

        try {
            db.beginTransaction()  // start DB transaction
            noteId = _saveNote(db, note);
            if (noteId != 0) {
                _deleteNoteKeywords(db, noteId)
                _insertNoteKeywords(db, noteId, keywords)
            }
            db.setTransactionSuccessful()  // commit DB changes
        } catch (ex: Exception) {
            noteId = 0
        } finally {
            // End the transaction (whether successful or not)
            db.endTransaction()
        }

        return@withContext noteId
    }

    private fun _saveNote(db: SQLiteDatabase, note: Note): Int {
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

    suspend fun saveCategory(category: Category): Int = withContext(Dispatchers.IO) {
        val db = this@DatabaseHandler.writableDatabase
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

        return@withContext categoryId
    }

    suspend fun deleteNote(noteId: Int): Boolean = withContext(Dispatchers.IO) {
        val db = this@DatabaseHandler.writableDatabase
        var delOk : Boolean

        try {
            db.beginTransaction()  // start DB transaction
            delOk = _deleteNote(db, noteId)
            if (delOk) {
                _deleteNoteKeywords(db, noteId)
            }
            db.setTransactionSuccessful()  // commit DB changes
        } catch (ex: Exception) {
            delOk = false
        } finally {
            // End the transaction (whether successful or not)
            db.endTransaction()
        }

        return@withContext delOk
    }

    private fun _deleteNote(db: SQLiteDatabase, noteId: Int): Boolean {
        var delOk : Boolean
        val numOfDelRows = db.delete("note", "_id = ?", arrayOf(noteId.toString()))
        if (numOfDelRows > 0) {
            delOk = true
        } else {
            delOk = false
        }
        return delOk
    }

    suspend fun deleteCategory(categoryId: Int, parentId: Int): Boolean = withContext(Dispatchers.IO) {
        val db = this@DatabaseHandler.writableDatabase
        var delOk : Boolean

        try {
            db.beginTransaction()  // start DB transaction
            delOk = _deleteCategory(db, categoryId)
            if (delOk) {
                _changeNotesCategory(db, categoryId, parentId)
                _changeCategoriesParent(db, categoryId, parentId)
            }
            db.setTransactionSuccessful()  // commit DB changes
        } catch (ex: Exception) {
            delOk = false
        } finally {
            // End the transaction (whether successful or not)
            db.endTransaction()
        }

        return@withContext delOk
    }

    private fun _deleteCategory(db: SQLiteDatabase, categoryId: Int): Boolean {
        var delOk : Boolean
        val numOfDelRows = db.delete("category", "_id = ?", arrayOf(categoryId.toString()))
        if (numOfDelRows > 0) {
            delOk = true
        } else {
            delOk = false
        }
        return delOk
    }

    private fun _insertNoteKeywords(db: SQLiteDatabase, noteId: Int, keywords: List<String>) {
        for (keyword in keywords) {
            val values = ContentValues().apply {
                put("keyword", keyword)
                put("note_id", noteId)
            }
            db.insert("keyword_note", null, values)
        }
    }

    private fun _deleteNoteKeywords(db: SQLiteDatabase, noteId: Int): Boolean {
        var delOk : Boolean
        val numOfDelRows = db.delete("keyword_note", "note_id = ?", arrayOf(noteId.toString()))
        if (numOfDelRows > 0) {
            delOk = true
        } else {
            delOk = false
        }
        return delOk
    }

    private fun _changeNotesCategory(db: SQLiteDatabase, fromCatId: Int, toCatId: Int) {
        val values = ContentValues().apply {
            put("cat_id", toCatId)
        }

        db.update("note", values, "cat_id = ?", arrayOf(fromCatId.toString()))
    }

    private fun _changeCategoriesParent(db: SQLiteDatabase, fromParentId: Int, toParentId: Int) {
        val values = ContentValues().apply {
            put("parent_id", toParentId)
        }

        db.update("category", values, "parent_id = ?", arrayOf(fromParentId.toString()))
    }
}