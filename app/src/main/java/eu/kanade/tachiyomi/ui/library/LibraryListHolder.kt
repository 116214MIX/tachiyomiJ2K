package eu.kanade.tachiyomi.ui.library

import android.view.View
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.signature.ObjectKey
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.data.database.models.MangaImpl
import eu.kanade.tachiyomi.data.glide.GlideApp
import kotlinx.android.synthetic.main.catalogue_list_item.*
import kotlinx.android.synthetic.main.catalogue_list_item.view.*
import kotlinx.android.synthetic.main.unread_download_badge.*

/**
 * Class used to hold the displayed data of a manga in the library, like the cover or the title.
 * All the elements from the layout file "item_library_list" are available in this class.
 *
 * @param view the inflated view for this holder.
 * @param adapter the adapter handling this holder.
 * @param listener a listener to react to single tap and long tap events.
 * @constructor creates a new library holder.
 */

class LibraryListHolder(
    private val view: View,
    adapter: LibraryCategoryAdapter
) : LibraryHolder(view, adapter) {

    /**
     * Method called from [LibraryCategoryAdapter.onBindViewHolder]. It updates the data for this
     * holder with the given manga.
     *
     * @param item the manga item to bind.
     */
    override fun onSetValues(item: LibraryItem) {
        // Update the title of the manga.
        title.text = item.manga.title
        setUnreadBadge(badge_view, item)

        subtitle.text = item.manga.author?.trim()
        subtitle.visibility = if (!item.manga.author.isNullOrBlank()) View.VISIBLE
        else View.GONE

        play_layout.visibility = if (item.manga.unread > 0 && item.unreadType > 0)
            View.VISIBLE else View.GONE
        play_layout.setOnClickListener { playButtonClicked() }

        // Update the cover.
        if (item.manga.thumbnail_url == null) Glide.with(view.context).clear(cover_thumbnail)
        else {
            val id = item.manga.id ?: return
            val height = itemView.context.resources.getDimensionPixelSize(R.dimen
                .material_component_lists_single_line_with_avatar_height)
            GlideApp.with(view.context).load(item.manga)
                .diskCacheStrategy(DiskCacheStrategy.RESOURCE)
                .signature(ObjectKey(MangaImpl.getLastCoverFetch(id).toString()))
                .override(height)
                .into(cover_thumbnail)
        }
    }

    private fun playButtonClicked() {
        adapter.libraryListener.startReading(adapterPosition)
    }

    override fun onActionStateChanged(position: Int, actionState: Int) {
        super.onActionStateChanged(position, actionState)
        if (actionState == 2) {
            view.card.isDragged = true
        }
    }

    override fun onItemReleased(position: Int) {
        super.onItemReleased(position)
        view.card.isDragged = false
    }
}
