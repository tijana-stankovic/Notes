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

class CategoriesRecyclerAdapter(val context: Context, val categories: ArrayList<Category>) : RecyclerView.Adapter<CategoriesRecyclerAdapter.ViewHolder>() {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CategoriesRecyclerAdapter.ViewHolder {
        val v = LayoutInflater.from(context).inflate(R.layout.categories_item, parent, false)
        return ViewHolder(v)
    }

    override fun onBindViewHolder(holder: CategoriesRecyclerAdapter.ViewHolder, position: Int) {
        holder.tvCategory.text = categories[position].name

        if (position % 2 == 0) {
            holder.cvCategoriesListRow.setBackgroundColor(
                ContextCompat.getColor(context, R.color.even_row_color)
            )
        } else {
            holder.cvCategoriesListRow.setBackgroundColor(
                ContextCompat.getColor(context, R.color.odd_row_color)
            )
        }
    }

    override fun getItemCount(): Int {
        return categories.size
    }

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        var cvCategoriesListRow : CardView
        var tvCategory : TextView

        init {
            cvCategoriesListRow = itemView.findViewById(R.id.cvCategoriesListRow)
            tvCategory = itemView.findViewById(R.id.tvCategory)

            itemView.setOnClickListener {
                val position : Int = adapterPosition

                editCategory(categories[position].id)
            }
        }
    }

    private fun editCategory(categoryId: Int) {
        val intent = Intent(context, CategoryActivity::class.java).apply {
            putExtra(CategoryActivity.CATEGORY_ID, categoryId)
        }
        context.startActivity(intent)
    }
}
