package com.billshare.app.utils

import android.content.Context
import android.graphics.drawable.GradientDrawable
import android.widget.TextView
import androidx.core.content.ContextCompat
import com.billshare.app.R
import com.billshare.app.models.Person

object AvatarHelper {

    private val palette = intArrayOf(
        R.color.avatar_1,
        R.color.avatar_2,
        R.color.avatar_3,
        R.color.avatar_4,
        R.color.avatar_5,
        R.color.avatar_6,
        R.color.avatar_7,
        R.color.avatar_8,
    )

    fun colorFor(context: Context, person: Person): Int {
        val idx = (person.id.hashCode() and 0x7FFFFFFF) % palette.size
        return ContextCompat.getColor(context, palette[idx])
    }

    fun initialFor(person: Person): String {
        val trimmed = person.name.trim()
        if (trimmed.isEmpty()) return "?"
        val parts = trimmed.split(Regex("\\s+"))
        return when {
            parts.size >= 2 -> "${parts[0].first()}${parts[1].first()}".uppercase()
            else -> trimmed.first().uppercase()
        }
    }

    fun bind(textView: TextView, person: Person) {
        val ctx = textView.context
        textView.text = initialFor(person)
        val bg = ContextCompat.getDrawable(ctx, R.drawable.bg_avatar_circle)?.mutate()
        if (bg is GradientDrawable) {
            bg.setColor(colorFor(ctx, person))
        }
        textView.background = bg
    }
}
