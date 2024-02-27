package com.tina.notes

import android.content.Context
import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.cardview.widget.CardView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView

class NotesRecyclerAdapter(val context: Context, val notes: ArrayList<Note>) : RecyclerView.Adapter<NotesRecyclerAdapter.ViewHolder>() {
    private var db: DatabaseHandler = DatabaseHandler(context)
    var categories : ArrayList<Category> = ArrayList<Category>()


    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): NotesRecyclerAdapter.ViewHolder {
        categories = db.getAllCategories()
        val v = LayoutInflater.from(context).inflate(R.layout.notes_item, parent, false)
        return ViewHolder(v)
    }

    override fun onBindViewHolder(holder: NotesRecyclerAdapter.ViewHolder, position: Int) {
        val categoryIndex = categories.indexOfFirst { it.id == notes[position].catId }
        val parentId = categories[categoryIndex].parentId
        if (parentId == 0) { // if note category has no parent category
            holder.tvNoteCategory.text = ""
        } else { // if note category has parent category
            val parentIndex = categories.indexOfFirst { it.id == parentId }
            holder.tvNoteCategory.text = categories[parentIndex].name
        }
        holder.tvNoteSubcategory.text = categories[categoryIndex].name
        holder.tvNoteKeywords.text = notes[position].keywords.toString()
        holder.tvNote.text = notes[position].title

        if (position % 2 == 0) {
            holder.cvNotesListRow.setBackgroundColor(
                ContextCompat.getColor(context, R.color.even_row_color)
            )
        } else {
            holder.cvNotesListRow.setBackgroundColor(
                ContextCompat.getColor(context, R.color.odd_row_color)
            )
        }
    }

    override fun getItemCount(): Int {
        return notes.size
    }

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        var cvNotesListRow : CardView
        var tvNoteCategory : TextView
        var tvNoteSubcategory : TextView
        var tvNoteKeywords : TextView
        var tvNote : TextView

        init {
            cvNotesListRow = itemView.findViewById(R.id.cvNotesListRow)
            tvNoteCategory = itemView.findViewById(R.id.tvNoteCategory)
            tvNoteSubcategory = itemView.findViewById(R.id.tvNoteSubcategory)
            tvNoteKeywords = itemView.findViewById(R.id.tvNoteKeywords)
            tvNote = itemView.findViewById(R.id.tvNote)

            itemView.setOnClickListener {
                val position : Int = adapterPosition
                editNote(notes[position].id)
            }
        }
    }

    private fun editNote(noteId: Int) {
        val intent = Intent(context, NoteActivity::class.java).apply {
            putExtra(NoteActivity.NOTE_ID, noteId)
        }
        context.startActivity(intent)
    }
}