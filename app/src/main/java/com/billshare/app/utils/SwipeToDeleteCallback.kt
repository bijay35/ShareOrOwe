package com.billshare.app.utils

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.RecyclerView

class SwipeToDeleteCallback(
    private val onSwiped: (position: Int) -> Unit,
    private val canSwipe: (position: Int) -> Boolean = { true }
) : ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT or ItemTouchHelper.RIGHT) {

    override fun getSwipeDirs(recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder): Int {
        return if (canSwipe(viewHolder.bindingAdapterPosition)) super.getSwipeDirs(recyclerView, viewHolder) else 0
    }

    private val background = ColorDrawable(Color.parseColor("#E53935"))

    override fun onMove(
        recyclerView: RecyclerView,
        viewHolder: RecyclerView.ViewHolder,
        target: RecyclerView.ViewHolder
    ): Boolean = false

    override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
        onSwiped(viewHolder.bindingAdapterPosition)
    }

    override fun onChildDraw(
        c: Canvas,
        recyclerView: RecyclerView,
        viewHolder: RecyclerView.ViewHolder,
        dX: Float,
        dY: Float,
        actionState: Int,
        isCurrentlyActive: Boolean
    ) {
        val itemView = viewHolder.itemView
        val top = itemView.top
        val bottom = itemView.bottom

        when {
            dX > 0 -> background.setBounds(itemView.left, top, itemView.left + dX.toInt(), bottom)
            dX < 0 -> background.setBounds(itemView.right + dX.toInt(), top, itemView.right, bottom)
            else -> background.setBounds(0, 0, 0, 0)
        }
        background.draw(c)
        super.onChildDraw(c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive)
    }
}
