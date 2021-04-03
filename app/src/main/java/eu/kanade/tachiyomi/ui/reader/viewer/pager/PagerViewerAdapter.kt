package eu.kanade.tachiyomi.ui.reader.viewer.pager

import android.view.View
import android.view.ViewGroup
import eu.kanade.tachiyomi.ui.reader.model.ChapterTransition
import eu.kanade.tachiyomi.ui.reader.model.ReaderChapter
import eu.kanade.tachiyomi.ui.reader.model.ReaderPage
import eu.kanade.tachiyomi.ui.reader.model.ViewerChapters
import eu.kanade.tachiyomi.widget.ViewPagerAdapter
import timber.log.Timber
import kotlin.math.max

/**
 * Pager adapter used by this [viewer] to where [ViewerChapters] updates are posted.
 */
class PagerViewerAdapter(private val viewer: PagerViewer) : ViewPagerAdapter() {

    /**
     * List of currently set items.
     */
    var joinedItems: MutableList<Pair<Any, Any?>> = mutableListOf()
        private set

    private var subItems: MutableList<Any> = mutableListOf()

    var nextTransition: ChapterTransition.Next? = null
        private set

    var pageToShift: ReaderPage? = null

    /**
     * List of currently set items.
     */

    private var shifted = viewer.config.shiftDoublePage
    private var doubledUp = viewer.config.doublePages
    var currentChapter: ReaderChapter? = null

    /**
     * Updates this adapter with the given [chapters]. It handles setting a few pages of the
     * next/previous chapter to allow seamless transitions and inverting the pages if the viewer
     * has R2L direction.
     */
    fun setChapters(chapters: ViewerChapters, forceTransition: Boolean) {
        val newItems = mutableListOf<Any>()

        // Add previous chapter pages and transition.
        if (chapters.prevChapter != null) {
            // We only need to add the last few pages of the previous chapter, because it'll be
            // selected as the current chapter when one of those pages is selected.
            val prevPages = chapters.prevChapter.pages
            // We will take an even number of pages if the page count if even
            // however we should take account full pages when deciding
            val numberOfFullPages =
                (
                    chapters.prevChapter.pages?.count { it.fullPage || it.isolatedPage }
                        ?: 0
                    )
            if (prevPages != null) {
                newItems.addAll(prevPages.takeLast(if ((prevPages.size + numberOfFullPages) % 2 == 0) 2 else 3))
            }
        }

        // Skip transition page if the chapter is loaded & current page is not a transition page
        if (forceTransition || chapters.prevChapter?.state !is ReaderChapter.State.Loaded) {
            newItems.add(ChapterTransition.Prev(chapters.currChapter, chapters.prevChapter))
        }

        // Add current chapter.
        val currPages = chapters.currChapter.pages
        if (currPages != null) {
            newItems.addAll(currPages)
        }

        currentChapter = chapters.currChapter

        // Add next chapter transition and pages.
        nextTransition = ChapterTransition.Next(chapters.currChapter, chapters.nextChapter)
            .also {
                if (forceTransition ||
                    chapters.nextChapter?.state !is ReaderChapter.State.Loaded
                ) {
                    newItems.add(it)
                }
            }

        if (chapters.nextChapter != null) {
            // Add at most two pages, because this chapter will be selected before the user can
            // swap more pages.
            val nextPages = chapters.nextChapter.pages
            if (nextPages != null) {
                newItems.addAll(nextPages.take(2))
            }
        }

        subItems = newItems.toMutableList()

        val currentPage = joinedItems.getOrNull(viewer.pager.currentItem)
        var pageToUse = currentPage?.second ?: currentPage?.first

        if (shifted != viewer.config.shiftDoublePage || (doubledUp != viewer.config.doublePages && doubledUp)) {
            if (!shifted || (doubledUp != viewer.config.doublePages && doubledUp)) {
                pageToUse = currentPage?.first
            }
            shifted = viewer.config.shiftDoublePage
        }
        doubledUp = viewer.config.doublePages
        setJoinedItems(
            pageToUse
        )
    }

    /**
     * Returns the amount of items of the adapter.
     */
    override fun getCount(): Int {
        return joinedItems.size
    }

    /**
     * Creates a new view for the item at the given [position].
     */
    override fun createView(container: ViewGroup, position: Int): View {
        val item = joinedItems[position].first
        val item2 = joinedItems[position].second
        return when (item) {
            is ReaderPage -> PagerPageHolder(viewer, item, item2 as? ReaderPage)
            is ChapterTransition -> PagerTransitionHolder(viewer, item)
            else -> throw NotImplementedError("Holder for ${item.javaClass} not implemented")
        }
    }

    /**
     * Returns the current position of the given [view] on the adapter.
     */
    override fun getItemPosition(view: Any): Int {
        if (view is PositionableView) {
            val position = joinedItems.indexOfFirst {
                view.item == (it.first to it.second)
            }
            if (position != -1) {
                return position
            } else {
                Timber.d("Position for ${view.item} not found")
            }
        }
        return POSITION_NONE
    }

    fun onPageSplit(current: ReaderPage) {
        val oldCurrent = joinedItems.getOrNull(viewer.pager.currentItem)
        setJoinedItems(current)

        // The listener may be removed when we split a page, so the ui may not have updated properly

        if (oldCurrent?.first == current || oldCurrent?.second == current) {
            viewer.pager.post {
                viewer.onPageChange(viewer.pager.currentItem)
            }
        }
    }

    private fun setJoinedItems(currentPage: Any?) {
        val oldCurrent = joinedItems.getOrNull(viewer.pager.currentItem)
        if (!viewer.config.doublePages) {
            // If not in double mode, set up items like before
            subItems.forEach {
                (it as? ReaderPage)?.shiftedPage = false
            }
            this.joinedItems = subItems.map { Pair<Any, Any?>(it, null) }.toMutableList()
            if (viewer is R2LPagerViewer) {
                joinedItems.reverse()
            }
        } else {
            val pagedItems = mutableListOf<MutableList<ReaderPage?>>()
            val otherItems = mutableListOf<Any>()
            pagedItems.add(mutableListOf())
            // Step 1: segment the pages and transition pages
            subItems.forEach {
                if (it is ReaderPage) {
                    pagedItems.last().add(it)
                } else {
                    otherItems.add(it)
                    pagedItems.add(mutableListOf())
                }
            }
            var pagedIndex = 0
            val subJoinedItems = mutableListOf<Pair<Any, Any?>>()
            // Step 2: run through each set of pages
            pagedItems.forEach { items ->

                items.forEach {
                    it?.shiftedPage = false
                }
                // Step 3: If pages have been shifted,
                if (viewer.config.shiftDoublePage) {
                    run loop@{
                        var index = items.indexOf(pageToShift ?: currentPage)
                        if ((currentPage as? ReaderPage)?.fullPage == true) {
                            index = max(0, index - 1)
                        }
                        // Go from the current page and work your way back to the first page,
                        // or the first page that's a full page.
                        // This is done in case user tries to shift a page after a full page
                        val fullPageBeforeIndex = max(
                            0,
                            (
                                if (index > -1) (
                                    items.subList(0, index)
                                        .indexOfFirst { it?.fullPage == true }
                                    ) else -1
                                )
                        )
                        // Add a shifted page to the first place there isnt a full page
                        (fullPageBeforeIndex until items.size).forEach {
                            if (items[it]?.fullPage == false) {
                                items[it]?.shiftedPage = true
                                return@loop
                            }
                        }
                    }
                }

                // Step 4: Add blanks for chunking
                var itemIndex = 0
                while (itemIndex < items.size) {
                    items[itemIndex]?.isolatedPage = false
                    if (items[itemIndex]?.fullPage == true || items[itemIndex]?.shiftedPage == true) {
                        // Add a 'blank' page after each full page. It will be used when chunked to solo a page
                        items.add(itemIndex + 1, null)
                        if (items[itemIndex]?.fullPage == true && itemIndex > 0 &&
                            items[itemIndex - 1] != null && (itemIndex - 1) % 2 == 0
                        ) {
                            // If a page is a full page, check if the previous page needs to be isolated
                            // we should check if it's an even or odd page, since even pages need shifting
                            // For example if Page 1 is full, Page 0 needs to be isolated
                            // No need to take account shifted pages, because null additions should
                            // always have an odd index in the list
                            items[itemIndex - 1]?.isolatedPage = true
                            items.add(itemIndex, null)
                            itemIndex++
                        }
                        itemIndex++
                    }
                    itemIndex++
                }

                // Step 5: chunk em
                if (items.isNotEmpty()) {
                    subJoinedItems.addAll(
                        items.chunked(2).map { Pair(it.first()!!, it.getOrNull(1)) }
                    )
                }
                otherItems.getOrNull(pagedIndex)?.let {
                    subJoinedItems.add(Pair(it, null))
                    pagedIndex++
                }
            }
            if (viewer is R2LPagerViewer) {
                subJoinedItems.reverse()
            }

            this.joinedItems = subJoinedItems
        }
        notifyDataSetChanged()

        if (currentPage != null && (
            (currentPage as? ReaderPage)?.chapter == currentChapter ||
                (shifted && (currentPage as? ChapterTransition)?.from == currentChapter)
            )
        ) {
            // Step 6: Move back to our previous page or transition page
            // The listener is likely off around now, but either way when shifting or doubling,
            // we need to set the page back correctly
            if (oldCurrent?.first == currentPage || oldCurrent?.second == currentPage) {
                val index =
                    joinedItems.indexOfFirst { it.first == currentPage || it.second == currentPage }
                viewer.pager.setCurrentItem(index, false)
            } else {
                val newPage = oldCurrent?.first ?: currentPage
                val index = joinedItems.indexOfFirst { it.first == newPage || it.second == newPage }
                viewer.pager.setCurrentItem(index, false)
            }
        } else if (currentPage is ReaderPage && currentPage.chapter != currentChapter &&
            (oldCurrent?.first !is ChapterTransition)
        ) {
            val subIndex = subItems.find { (it as? ReaderPage)?.chapter == currentChapter }
            val index = joinedItems.indexOfFirst { it.first == subIndex }
            viewer.pager.setCurrentItem(index, false)
        } else if (oldCurrent?.first is ChapterTransition) {
            val newPage = oldCurrent.first
            val index = joinedItems.indexOfFirst { it.first == newPage || it.second == newPage }
            viewer.pager.setCurrentItem(index, false)
        }
    }
}
