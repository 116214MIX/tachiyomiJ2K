package eu.kanade.tachiyomi.ui.reader.settings

import android.view.View
import com.google.android.material.tabs.TabLayout
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.ui.reader.ReaderActivity
import eu.kanade.tachiyomi.util.view.gone
import eu.kanade.tachiyomi.util.view.visInvisIf
import eu.kanade.tachiyomi.util.view.visible
import eu.kanade.tachiyomi.widget.TabbedBottomSheetDialog

class TabbedReaderSettingsSheet(val readerActivity: ReaderActivity) : TabbedBottomSheetDialog(
    readerActivity
) {
    private val generalView: ReaderGeneralView = View.inflate(
        readerActivity,
        R.layout.reader_general_layout,
        null
    ) as ReaderGeneralView
    private val pagedView: ReaderPagedView = View.inflate(
        readerActivity,
        R.layout.reader_paged_layout,
        null
    ) as ReaderPagedView
    private val filterView: ReaderFilterView = View.inflate(
        readerActivity,
        R.layout.reader_color_filter,
        null
    ) as ReaderFilterView

    var showWebview: Boolean = {
        val mangaViewer = readerActivity.presenter.getMangaViewer()
        mangaViewer == ReaderActivity.WEBTOON || mangaViewer == ReaderActivity.VERTICAL_PLUS
    }()

    override var offset = 0

    override fun getTabViews(): List<View> = listOf(
        generalView,
        pagedView,
        filterView
    )

    override fun getTabTitles(): List<Int> = listOf(
        R.string.general,
        if (showWebview) R.string.webtoon else R.string.paged,
        R.string.filter
    )

    init {
        generalView.activity = readerActivity
        pagedView.activity = readerActivity
        filterView.activity = readerActivity
        generalView.sheet = this

        binding.menu.gone()
        val attrs = window?.attributes
        val ogDim = attrs?.dimAmount ?: 0.25f
        binding.pager.adapter?.notifyDataSetChanged()
        binding.tabs.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab?) {
                window?.setDimAmount(if (tab?.position == 2) 0f else ogDim)
                readerActivity.binding.appBar.visInvisIf(tab?.position != 2)
            }

            override fun onTabUnselected(tab: TabLayout.Tab?) {
            }

            override fun onTabReselected(tab: TabLayout.Tab?) {
            }
        })
    }

    override fun dismiss() {
        super.dismiss()
        readerActivity.binding.appBar.visible()
    }

    fun updateTabs(isWebtoon: Boolean) {
        showWebview = isWebtoon
        binding.pager.adapter?.notifyDataSetChanged()
        pagedView.updatePrefs()
    }
}
