package eu.kanade.tachiyomi.ui.recents

import android.app.Activity
import android.view.View
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.data.image.coil.loadLibraryManga
import eu.kanade.tachiyomi.data.download.model.Download
import eu.kanade.tachiyomi.databinding.RecentMangaItemBinding
import eu.kanade.tachiyomi.source.LocalSource
import eu.kanade.tachiyomi.ui.manga.chapter.BaseChapterHolder
import eu.kanade.tachiyomi.util.chapter.ChapterUtil
import eu.kanade.tachiyomi.util.system.timeSpanFromNow
import eu.kanade.tachiyomi.util.view.visibleIf

class RecentMangaHolder(
    view: View,
    val adapter: RecentMangaAdapter
) : BaseChapterHolder(view, adapter) {

    private val binding = RecentMangaItemBinding.bind(view)

    init {
        binding.cardLayout.setOnClickListener { adapter.delegate.onCoverClick(flexibleAdapterPosition) }
    }

    fun bind(recentsType: Int) {
        when (recentsType) {
            RecentMangaHeaderItem.CONTINUE_READING -> {
                binding.title.setText(R.string.view_history)
            }
            RecentMangaHeaderItem.NEW_CHAPTERS -> {
                binding.title.setText(R.string.view_all_updates)
            }
        }
    }

    fun bind(item: RecentMangaItem) {
        binding.downloadButton.downloadButton.visibleIf(item.mch.manga.source != LocalSource.ID)

        binding.title.apply {
            text = item.chapter.name
            ChapterUtil.setTextViewForChapter(this, item)
        }
        binding.subtitle.apply {
            text = item.mch.manga.title
            setTextColor(ChapterUtil.readColor(context, item))
        }
        val notValidNum = item.mch.chapter.chapter_number <= 0
        binding.body.text = when {
            item.mch.chapter.id == null -> binding.body.context.getString(
                R.string.added_,
                item.mch.manga.date_added.timeSpanFromNow
            )
            item.mch.history.id == null -> binding.body.context.getString(
                R.string.updated_,
                item.chapter.date_upload.timeSpanFromNow
            )
            item.chapter.id != item.mch.chapter.id ->
                binding.body.context.getString(
                    R.string.read_,
                    item.mch.history.last_read.timeSpanFromNow
                ) + "\n" + binding.body.context.getString(
                    if (notValidNum) R.string.last_read_ else R.string.last_read_chapter_,
                    if (notValidNum) item.mch.chapter.name else adapter.decimalFormat.format(item.mch.chapter.chapter_number)
                )
            item.chapter.pages_left > 0 && !item.chapter.read ->
                binding.body.context.getString(
                    R.string.read_,
                    item.mch.history.last_read.timeSpanFromNow
                ) + "\n" + itemView.resources.getQuantityString(
                    R.plurals.pages_left,
                    item.chapter.pages_left,
                    item.chapter.pages_left
                )
            else -> binding.body.context.getString(
                R.string.read_,
                item.mch.history.last_read.timeSpanFromNow
            )
        }
        if ((itemView.context as? Activity)?.isDestroyed != true) {
            binding.coverThumbnail.loadLibraryManga(item.mch.manga)
        }
        notifyStatus(
            if (adapter.isSelected(flexibleAdapterPosition)) Download.CHECKED else item.status,
            item.progress
        )
        resetFrontView()
    }

    private fun resetFrontView() {
        if (binding.frontView.translationX != 0f) itemView.post { adapter.notifyItemChanged(flexibleAdapterPosition) }
    }

    override fun onLongClick(view: View?): Boolean {
        super.onLongClick(view)
        val item = adapter.getItem(flexibleAdapterPosition) as? RecentMangaItem ?: return false
        return item.mch.history.id != null
    }

    fun notifyStatus(status: Int, progress: Int) =
        binding.downloadButton.downloadButton.setDownloadStatus(status, progress)

    override fun getFrontView(): View {
        return binding.frontView
    }

    override fun getRearRightView(): View {
        return binding.rightView
    }
}
