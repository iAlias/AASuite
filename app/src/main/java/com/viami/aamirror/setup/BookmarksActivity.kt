package com.viami.aamirror.setup

import android.app.AlertDialog
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ListView
import android.widget.TextView
import androidx.activity.ComponentActivity
import com.viami.aamirror.R
import com.viami.aamirror.core.Bookmark

/** Phone-side editor for the car browser favorites: add, edit, delete, reorder. */
class BookmarksActivity : ComponentActivity() {

    private lateinit var bookmarks: MutableList<Bookmark>
    private val adapter = BookmarkAdapter()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        title = getString(R.string.bookmarks_title)
        bookmarks = BookmarkStore.load(this).toMutableList()

        val pad = dp(16)
        val addButton = Button(this).apply {
            text = getString(R.string.btn_add_bookmark)
            setOnClickListener { showEditor(index = null) }
        }
        val list = ListView(this).apply { adapter = this@BookmarksActivity.adapter }
        setContentView(
            LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(pad, pad, pad, pad)
                addView(addButton)
                addView(list)
            }
        )
    }

    private fun persist() {
        BookmarkStore.save(this, bookmarks)
        adapter.notifyDataSetChanged()
    }

    private fun move(index: Int, delta: Int) {
        val target = index + delta
        if (target < 0 || target >= bookmarks.size) return
        val item = bookmarks.removeAt(index)
        bookmarks.add(target, item)
        persist()
    }

    private fun showEditor(index: Int?) {
        val existing = index?.let { bookmarks[it] }
        val pad = dp(16)
        val titleInput = EditText(this).apply {
            hint = getString(R.string.bookmark_title_hint)
            setText(existing?.title.orEmpty())
        }
        val urlInput = EditText(this).apply {
            hint = getString(R.string.bookmark_url_hint)
            setText(existing?.url.orEmpty())
        }
        val container = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(pad, pad, pad, 0)
            addView(titleInput)
            addView(urlInput)
        }
        val builder = AlertDialog.Builder(this)
            .setTitle(
                if (existing == null) R.string.btn_add_bookmark else R.string.edit_bookmark
            )
            .setView(container)
            .setPositiveButton(R.string.save) { _, _ ->
                val title = titleInput.text.toString().trim()
                val url = urlInput.text.toString().trim()
                if (title.isEmpty() || url.isEmpty()) return@setPositiveButton
                val bookmark = Bookmark(title, url)
                if (index == null) bookmarks.add(bookmark) else bookmarks[index] = bookmark
                persist()
            }
            .setNegativeButton(R.string.cancel, null)
        if (index != null) {
            builder.setNeutralButton(R.string.delete) { _, _ ->
                bookmarks.removeAt(index)
                persist()
            }
        }
        builder.show()
    }

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()

    private inner class BookmarkAdapter : BaseAdapter() {
        override fun getCount(): Int = bookmarks.size
        override fun getItem(position: Int): Bookmark = bookmarks[position]
        override fun getItemId(position: Int): Long = position.toLong()

        override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {
            val bookmark = bookmarks[position]
            val label = TextView(this@BookmarksActivity).apply {
                text = "${bookmark.title}\n${bookmark.url}"
                textSize = 16f
                layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
                setPadding(0, dp(12), 0, dp(12))
                setOnClickListener { showEditor(position) }
            }
            return LinearLayout(this@BookmarksActivity).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                addView(label)
                addView(moveButton("▲") { move(position, -1) })
                addView(moveButton("▼") { move(position, +1) })
            }
        }

        private fun moveButton(symbol: String, onClick: () -> Unit): Button =
            Button(this@BookmarksActivity).apply {
                text = symbol
                minWidth = dp(48)
                minimumWidth = dp(48)
                setOnClickListener { onClick() }
            }
    }
}
