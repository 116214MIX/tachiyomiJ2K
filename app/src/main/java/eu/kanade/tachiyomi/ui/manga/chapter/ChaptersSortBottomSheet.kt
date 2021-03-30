package eu.kanade.tachiyomi.ui.manga.chapter

import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.data.database.models.Manga
import eu.kanade.tachiyomi.databinding.ChapterSortBottomSheetBinding
import eu.kanade.tachiyomi.ui.manga.MangaDetailsController
import eu.kanade.tachiyomi.util.system.dpToPx
import eu.kanade.tachiyomi.util.view.invisible
import eu.kanade.tachiyomi.util.view.setBottomEdge
import eu.kanade.tachiyomi.util.view.setEdgeToEdge
import eu.kanade.tachiyomi.util.view.visInvisIf
import eu.kanade.tachiyomi.util.view.visibleIf
import kotlin.math.max

class ChaptersSortBottomSheet(controller: MangaDetailsController) : BottomSheetDialog
(controller.activity!!, R.style.BottomSheetDialogTheme) {

    val activity = controller.activity!!

    private var sheetBehavior: BottomSheetBehavior<*>

    private val presenter = controller.presenter

    private val binding = ChapterSortBottomSheetBinding.inflate(activity.layoutInflater)
    init {
        // Use activity theme for this layout
        setContentView(binding.root)

        sheetBehavior = BottomSheetBehavior.from(binding.root.parent as ViewGroup)
        setEdgeToEdge(activity, binding.root)
        val height = activity.window.decorView.rootWindowInsets.systemWindowInsetBottom
        sheetBehavior.peekHeight = 415.dpToPx + height

        sheetBehavior.addBottomSheetCallback(
            object : BottomSheetBehavior.BottomSheetCallback() {
                override fun onSlide(bottomSheet: View, progress: Float) {
                    if (progress.isNaN()) {
                        binding.pill.alpha = 0f
                    } else {
                        binding.pill.alpha = (1 - max(0f, progress)) * 0.25f
                    }
                }

                override fun onStateChanged(p0: View, state: Int) {
                    if (state == BottomSheetBehavior.STATE_EXPANDED) {
                        sheetBehavior.skipCollapsed = true
                    }
                }
            }
        )
    }

    override fun onStart() {
        super.onStart()
        sheetBehavior.skipCollapsed = true
    }

    /**
     * Called when the sheet is created. It initializes the listeners and values of the preferences.
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        initGeneralPreferences()
        setBottomEdge(binding.hideTitles, activity)
        binding.closeButton.setOnClickListener { dismiss() }
        binding.settingsScrollView.viewTreeObserver.addOnGlobalLayoutListener {
            val isScrollable =
                binding.settingsScrollView.height < binding.sortLayout.height +
                    binding.settingsScrollView.paddingTop + binding.settingsScrollView.paddingBottom
            binding.closeButton.visibleIf(isScrollable)
            // making the view gone somehow breaks the layout so lets make it invisible
            binding.pill.visInvisIf(!isScrollable)
        }

        setOnDismissListener {
            presenter.setFilters(
                binding.chapterFilterLayout.showRead.isChecked,
                binding.chapterFilterLayout.showUnread.isChecked,
                binding.chapterFilterLayout.showDownload.isChecked,
                binding.chapterFilterLayout.showBookmark.isChecked
            )
        }
    }

    private fun initGeneralPreferences() {
        binding.chapterFilterLayout.root.setCheckboxes(presenter.manga)

        var defPref = presenter.globalSort()
        binding.sortGroup.check(
            if (presenter.manga.sortDescending(defPref)) R.id.sort_newest else {
                R.id.sort_oldest
            }
        )

        binding.hideTitles.isChecked = presenter.manga.displayMode != Manga.DISPLAY_NAME
        binding.sortMethodGroup.check(
            if (presenter.manga.sorting == Manga.SORTING_SOURCE) R.id.sort_by_source else {
                R.id.sort_by_number
            }
        )

        binding.setAsDefaultSort.visInvisIf(
            defPref != presenter.manga.sortDescending() &&
                presenter.manga.usesLocalSort()
        )
        binding.sortGroup.setOnCheckedChangeListener { _, checkedId ->
            presenter.setSortOrder(checkedId == R.id.sort_newest)
            binding.setAsDefaultSort.visInvisIf(
                defPref != presenter.manga.sortDescending() &&
                    presenter.manga.usesLocalSort()
            )
        }

        binding.setAsDefaultSort.setOnClickListener {
            val desc = binding.sortGroup.checkedRadioButtonId == R.id.sort_newest
            presenter.setGlobalChapterSort(desc)
            defPref = desc
            binding.setAsDefaultSort.invisible()
        }

        binding.sortMethodGroup.setOnCheckedChangeListener { _, checkedId ->
            presenter.setSortMethod(checkedId == R.id.sort_by_source)
        }

        binding.hideTitles.setOnCheckedChangeListener { _, isChecked ->
            presenter.hideTitle(isChecked)
        }
    }
}
