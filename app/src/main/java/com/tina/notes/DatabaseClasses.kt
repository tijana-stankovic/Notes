package com.tina.notes

class Note(var id: Int, var catId: Int, var keywords: String, var title: String, var content: String)

class Category(var id: Int, var parentId: Int, var name: String)

class KeywordNote(var keyword: String, var note_id: Int)


