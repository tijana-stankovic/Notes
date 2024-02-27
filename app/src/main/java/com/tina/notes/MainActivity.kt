package com.tina.notes

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Button
import android.widget.Toast

// Notes main screen
class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val btnNotes = findViewById<Button>(R.id.btnNotes)
        btnNotes.setOnClickListener {
            onNotesButtonClick()
        }

        val btnCategories = findViewById<Button>(R.id.btnCategories)
        btnCategories.setOnClickListener {
            onCategoriesButtonClick()
        }

        val btnAbout = findViewById<Button>(R.id.btnAbout)
        btnAbout.setOnClickListener {
            onAboutButtonClick()
        }

        val databaseHandler: DatabaseHandler = DatabaseHandler(this)
        databaseHandler.getReadableDatabase()
    }

    private fun onNotesButtonClick() {
        val intent = Intent(this, NotesActivity::class.java)
        startActivity(intent)
    }

    private fun onCategoriesButtonClick() {
        val intent = Intent(this, CategoriesActivity::class.java)
        startActivity(intent)
    }

    private fun onAboutButtonClick() {
        val intent = Intent(this, AboutActivity::class.java)
        startActivity(intent)
    }
}