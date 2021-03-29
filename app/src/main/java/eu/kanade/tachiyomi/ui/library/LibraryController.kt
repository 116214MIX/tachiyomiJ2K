package eu.kanade.tachiyomi.ui.library

import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.os.Handler
import android.util.TypedValue
import android.view.Gravity
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.ViewPropertyAnimator
import android.view.inputmethod.InputMethodManager
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.view.ActionMode
import androidx.appcompat.widget.SearchView
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.core.view.GestureDetectorCompat
import androidx.recyclerview.widget.DefaultItemAnimator
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.afollestad.materialdialogs.MaterialDialog
import com.afollestad.materialdialogs.list.listItemsSingleChoice
import com.bluelinelabs.conductor.ControllerChangeHandler
import com.bluelinelabs.conductor.ControllerChangeType
import com.github.florent37.viewtooltip.ViewTooltip
import com.google.android.material.snackbar.BaseTransientBottomBar
import com.google.android.material.snackbar.Snackbar
import eu.davidea.flexibleadapter.FlexibleAdapter
import eu.davidea.flexibleadapter.SelectableAdapter
import eu.davidea.flexibleadapter.items.IFlexible
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.data.database.models.Category
import eu.kanade.tachiyomi.data.database.models.LibraryManga
import eu.kanade.tachiyomi.data.database.models.Manga
import eu.kanade.tachiyomi.data.download.DownloadService
import eu.kanade.tachiyomi.data.library.LibraryServiceListener
import eu.kanade.tachiyomi.data.library.LibraryUpdateService
import eu.kanade.tachiyomi.data.notification.NotificationReceiver
import eu.kanade.tachiyomi.data.notification.Notifications
import eu.kanade.tachiyomi.data.preference.PreferencesHelper
import eu.kanade.tachiyomi.data.preference.getOrDefault
import eu.kanade.tachiyomi.databinding.LibraryListControllerBinding
import eu.kanade.tachiyomi.source.LocalSource
import eu.kanade.tachiyomi.ui.base.MaterialMenuSheet
import eu.kanade.tachiyomi.ui.base.controller.BaseController
import eu.kanade.tachiyomi.ui.category.CategoryController
import eu.kanade.tachiyomi.ui.category.ManageCategoryDialog
import eu.kanade.tachiyomi.ui.library.LibraryGroup.BY_DEFAULT
import eu.kanade.tachiyomi.ui.library.LibraryGroup.BY_SOURCE
import eu.kanade.tachiyomi.ui.library.LibraryGroup.BY_STATUS
import eu.kanade.tachiyomi.ui.library.LibraryGroup.BY_TAG
import eu.kanade.tachiyomi.ui.library.LibraryGroup.BY_TRACK_STATUS
import eu.kanade.tachiyomi.ui.library.LibraryGroup.UNGROUPED
import eu.kanade.tachiyomi.ui.library.display.TabbedLibraryDisplaySheet
import eu.kanade.tachiyomi.ui.library.filter.FilterBottomSheet
import eu.kanade.tachiyomi.ui.main.BottomSheetController
import eu.kanade.tachiyomi.ui.main.MainActivity
import eu.kanade.tachiyomi.ui.main.RootSearchInterface
import eu.kanade.tachiyomi.ui.manga.MangaDetailsController
import eu.kanade.tachiyomi.ui.migration.manga.design.PreMigrationController
import eu.kanade.tachiyomi.ui.reader.ReaderActivity
import eu.kanade.tachiyomi.ui.source.global_search.GlobalSearchController
import eu.kanade.tachiyomi.util.system.dpToPx
import eu.kanade.tachiyomi.util.system.getBottomGestureInsets
import eu.kanade.tachiyomi.util.system.getResourceColor
import eu.kanade.tachiyomi.util.system.launchUI
import eu.kanade.tachiyomi.util.view.activityBinding
import eu.kanade.tachiyomi.util.view.collapse
import eu.kanade.tachiyomi.util.view.expand
import eu.kanade.tachiyomi.util.view.getItemView
import eu.kanade.tachiyomi.util.view.gone
import eu.kanade.tachiyomi.util.view.hide
import eu.kanade.tachiyomi.util.view.invisible
import eu.kanade.tachiyomi.util.view.isExpanded
import eu.kanade.tachiyomi.util.view.isHidden
import eu.kanade.tachiyomi.util.view.isVisible
import eu.kanade.tachiyomi.util.view.scrollViewWith
import eu.kanade.tachiyomi.util.view.setOnQueryTextChangeListener
import eu.kanade.tachiyomi.util.view.setStyle
import eu.kanade.tachiyomi.util.view.snack
import eu.kanade.tachiyomi.util.view.updateLayoutParams
import eu.kanade.tachiyomi.util.view.updatePaddingRelative
import eu.kanade.tachiyomi.util.view.visible
import eu.kanade.tachiyomi.util.view.visibleIf
import eu.kanade.tachiyomi.util.view.withFadeTransaction
import eu.kanade.tachiyomi.widget.EndAnimatorListener
import kotlinx.coroutines.delay
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.roundToInt
import kotlin.random.Random
import kotlin.random.nextInt

class LibraryController(
    bundle: Bundle? = null,
    val preferences: PreferencesHelper = Injekt.get()
) : BaseController<LibraryListControllerBinding>(bundle),
    ActionMode.Callback,
    ChangeMangaCategoriesDialog.Listener,
    FlexibleAdapter.OnItemClickListener,
    FlexibleAdapter.OnItemLongClickListener,
    FlexibleAdapter.OnItemMoveListener,
    LibraryCategoryAdapter.LibraryListener,
    BottomSheetController,
    RootSearchInterface,
    LibraryServiceListener {

    init {
        setHasOptionsMenu(true)
        retainViewMode = RetainViewMode.RETAIN_DETACH
    }

    /**
     * Position of the active category.
     */
    private var activeCategory: Int = preferences.lastUsedCategory().getOrDefault()
    private var lastUsedCategory: Int = preferences.lastUsedCategory().getOrDefault()

    private var justStarted = true

    /**
     * Action mode for selections.
     */
    private var actionMode: ActionMode? = null

    private var libraryLayout: Int = preferences.libraryLayout().getOrDefault()

    var singleCategory: Boolean = false
        private set

    /**
     * Library search query.
     */
    private var query = ""

    /**
     * Currently selected mangas.
     */
    private val selectedMangas = mutableSetOf<Manga>()

    private lateinit var adapter: LibraryCategoryAdapter

    private var lastClickPosition = -1

    private var lastItemPosition: Int? = null
    private var lastItem: IFlexible<*>? = null

    lateinit var presenter: LibraryPresenter
        private set

    private var observeLater: Boolean = false
    var searchItem = SearchGlobalItem()

    var snack: Snackbar? = null
    var displaySheet: TabbedLibraryDisplaySheet? = null

    private var scrollDistance = 0f
    private val scrollDistanceTilHidden = 1000.dpToPx
    private var textAnim: ViewPropertyAnimator? = null
    private var hasExpanded = false

    var hopperGravity: Int = preferences.hopperGravity().get()
        @SuppressLint("RtlHardcoded")
        set(value) {
            field = value
            binding.jumperCategoryText.updateLayoutParams<CoordinatorLayout.LayoutParams> {
                anchorGravity = when (value) {
                    0 -> Gravity.RIGHT or Gravity.CENTER_VERTICAL
                    2 -> Gravity.LEFT or Gravity.CENTER_VERTICAL
                    else -> Gravity.TOP or Gravity.CENTER_HORIZONTAL
                }
                gravity = anchorGravity
            }
        }

    private var filterTooltip: ViewTooltip? = null
    private var isAnimatingHopper: Boolean? = null
    private var animatorSet: AnimatorSet? = null
    var hasMovedHopper = preferences.shownHopperSwipeTutorial().get()
    private var shouldScrollToTop = false
    private val showCategoryInTitle
        get() = preferences.showCategoryInTitle().get() && presenter.showAllCategories
    private lateinit var elevateAppBar: ((Boolean) -> Unit)
    private var hopperOffset = 0f

    override fun getTitle(): String? {
        return if (!showCategoryInTitle || binding.headerTitle.text.isNullOrBlank() || binding.recyclerCover.isClickable) {
            view?.context?.getString(R.string.library)
        } else {
            binding.headerTitle.text.toString()
        }
    }

    private var scrollListener = object : RecyclerView.OnScrollListener() {
        override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
            super.onScrolled(recyclerView, dx, dy)
            val recyclerCover = binding.recyclerCover
            if (!recyclerCover.isClickable && isAnimatingHopper != true) {
                if (preferences.autohideHopper().get()) {
                    hopperOffset += dy
                    hopperOffset = hopperOffset.coerceIn(0f, 55f.dpToPx)
                }
                if (!preferences.hideBottomNavOnScroll().get()) {
                    updateFilterSheetY()
                }
                binding.roundedCategoryHopper.upCategory.alpha = if (isAtTop()) 0.25f else 1f
                binding.roundedCategoryHopper.downCategory.alpha = if (isAtBottom()) 0.25f else 1f
            }
            if (!binding.filterBottomSheet.filterBottomSheet.sheetBehavior.isHidden()) {
                scrollDistance += abs(dy)
                if (scrollDistance > scrollDistanceTilHidden) {
                    binding.filterBottomSheet.filterBottomSheet.sheetBehavior?.hide()
                    scrollDistance = 0f
                }
            } else scrollDistance = 0f
            val currentCategory = getHeader()?.category ?: return
            if (currentCategory.order != activeCategory) {
                saveActiveCategory(currentCategory)
                if (!showCategoryInTitle && presenter.categories.size > 1 && dy != 0 && recyclerView.translationY == 0f) {
                    showCategoryText(currentCategory.name)
                }
            }
            val savedCurrentCategory = getHeader(true)?.category ?: return
            if (savedCurrentCategory.order != lastUsedCategory) {
                lastUsedCategory = savedCurrentCategory.order
                preferences.lastUsedCategory().set(savedCurrentCategory.order)
            }
        }

        override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
            super.onScrollStateChanged(recyclerView, newState)
            when (newState) {
                RecyclerView.SCROLL_STATE_DRAGGING -> {
                    binding.fastScroller.showScrollbar()
                }
                RecyclerView.SCROLL_STATE_IDLE -> {
                    updateHopperPosition()
                }
            }
        }
    }

    fun updateFilterSheetY() {
        val bottomBar = activityBinding?.bottomNav
        if (bottomBar != null) {
            if (binding.filterBottomSheet.filterBottomSheet.sheetBehavior.isHidden()) {
                val pad = bottomBar.translationY - bottomBar.height
                binding.filterBottomSheet.filterBottomSheet.translationY = pad
            } else {
                binding.filterBottomSheet.filterBottomSheet.translationY = 0f
            }
            val pad = bottomBar.translationY - bottomBar.height
            binding.shadow2.translationY = pad
            binding.filterBottomSheet.filterBottomSheet.updatePaddingRelative(
                bottom = max(
                    (-pad).toInt(),
                    view?.rootWindowInsets?.getBottomGestureInsets() ?: 0
                )
            )

            val padding = max(
                (-pad).toInt(),
                view?.rootWindowInsets?.getBottomGestureInsets() ?: 0
            )
            binding.filterBottomSheet.filterBottomSheet.sheetBehavior?.peekHeight = 60.dpToPx + padding
            updateHopperY()
            binding.fastScroller.updateLayoutParams<ViewGroup.MarginLayoutParams> {
                bottomMargin = -pad.toInt()
            }
        }
    }

    fun updateHopperPosition() {
        val shortAnimationDuration = resources?.getInteger(
            android.R.integer.config_shortAnimTime
        ) ?: 0
        if (preferences.autohideHopper().get()) {
            // Flow same snap rules as bottom nav
            val closerToHopperBottom = hopperOffset > 25f.dpToPx
            val halfWayBottom = activityBinding?.bottomNav?.height?.toFloat()?.div(2) ?: 0f
            val closerToBottom = (activityBinding?.bottomNav?.translationY ?: 0f) > halfWayBottom
            val atTop = !binding.libraryGridRecycler.recycler.canScrollVertically(-1)
            val closerToEdge = if (preferences.hideBottomNavOnScroll().get()) (closerToBottom && !atTop) else closerToHopperBottom
            val end = if (closerToEdge) 55f.dpToPx else 0f
            val alphaAnimation = ValueAnimator.ofFloat(hopperOffset, end)
            alphaAnimation.addUpdateListener { valueAnimator ->
                hopperOffset = valueAnimator.animatedValue as Float
                updateHopperY()
            }
            alphaAnimation.addListener(
                EndAnimatorListener {
                    hopperOffset = end
                    updateHopperY()
                }
            )
            alphaAnimation.duration = shortAnimationDuration.toLong()
            alphaAnimation.start()
        }
    }

    fun saveActiveCategory(category: Category) {
        activeCategory = category.order
        val headerItem = getHeader() ?: return
        binding.headerTitle.text = headerItem.category.name
        setActiveCategory()
    }

    private fun setActiveCategory() {
        val currentCategory = presenter.categories.indexOfFirst {
            if (presenter.showAllCategories) it.order == activeCategory else presenter.currentCategory == it.id
        }
        if (currentCategory > -1) {
            binding.categoryRecycler.setCategories(currentCategory)
            binding.headerTitle.text = presenter.categories[currentCategory].name
            setTitle()
        }
    }

    fun showMiniBar() {
        binding.headerTitle.visibleIf(showCategoryInTitle)
        setTitle()
    }

    fun showCategoryText(name: String) {
        textAnim?.cancel()
        textAnim = binding.jumperCategoryText.animate().alpha(0f).setDuration(250L).setStartDelay(2000)
        textAnim?.start()
        binding.jumperCategoryText.alpha = 1f
        binding.jumperCategoryText.text = name
    }

    fun isAtTop(): Boolean {
        return if (presenter.showAllCategories) {
            !binding.libraryGridRecycler.recycler.canScrollVertically(-1)
        } else {
            getVisibleHeader()?.category?.order == presenter.categories.minOfOrNull { it.order }
        }
    }

    fun isAtBottom(): Boolean {
        return if (presenter.showAllCategories) {
            !binding.libraryGridRecycler.recycler.canScrollVertically(1)
        } else {
            getVisibleHeader()?.category?.order == presenter.categories.maxOfOrNull { it.order }
        }
    }

    private fun showFilterTip() {
        if (preferences.shownFilterTutorial().get() || !hasExpanded) return
        val activityBinding = activityBinding ?: return
        val activity = activity ?: return
        val icon = activityBinding.bottomNav.getItemView(R.id.nav_library) ?: return
        filterTooltip =
            ViewTooltip.on(activity, icon).autoHide(false, 0L).align(ViewTooltip.ALIGN.START)
                .position(ViewTooltip.Position.TOP).text(R.string.tap_library_to_show_filters)
                .color(activity.getResourceColor(R.attr.colorAccent))
                .textSize(TypedValue.COMPLEX_UNIT_SP, 15f).textColor(Color.WHITE).withShadow(false)
                .corner(30).arrowWidth(15).arrowHeight(15).distanceWithView(0)

        filterTooltip?.show()
    }

    private fun closeTip() {
        if (filterTooltip != null) {
            filterTooltip?.close()
            filterTooltip = null
            preferences.shownFilterTutorial().set(true)
        }
    }

    override fun onViewCreated(view: View) {
        super.onViewCreated(view)
        if (!::presenter.isInitialized) presenter = LibraryPresenter(this)

        adapter = LibraryCategoryAdapter(this)
        setRecyclerLayout()

        binding.libraryGridRecycler.recycler.manager.spanSizeLookup = (
            object : GridLayoutManager.SpanSizeLookup() {
                override fun getSpanSize(position: Int): Int {
                    if (libraryLayout == 0) return 1
                    val item = this@LibraryController.adapter.getItem(position)
                    return if (item is LibraryHeaderItem || item is SearchGlobalItem || (item is LibraryItem && item.manga.isBlank())) {
                        binding.libraryGridRecycler.recycler.manager.spanCount
                    } else {
                        1
                    }
                }
            }
            )
        binding.libraryGridRecycler.recycler.setHasFixedSize(true)
        binding.libraryGridRecycler.recycler.adapter = adapter

        adapter.fastScroller = binding.fastScroller
        binding.libraryGridRecycler.recycler.addOnScrollListener(scrollListener)

        binding.swipeRefresh.setStyle()

        binding.recyclerCover.setOnClickListener {
            showCategories(false)
        }
        binding.categoryRecycler.onCategoryClicked = {
            binding.libraryGridRecycler.recycler.itemAnimator = null
            scrollToHeader(it)
            showCategories(show = false)
        }
        binding.categoryRecycler.onShowAllClicked = { isChecked ->
            preferences.showAllCategories().set(isChecked)
            presenter.getLibrary()
        }
        setupFilterSheet()
        setUpHopper()

        elevateAppBar =
            scrollViewWith(
                binding.libraryGridRecycler.recycler,
                swipeRefreshLayout = binding.swipeRefresh,
                afterInsets = { insets ->
                    binding.categoryRecycler.updateLayoutParams<ViewGroup.MarginLayoutParams> {
                        topMargin = binding.libraryGridRecycler.recycler.paddingTop
                    }
                    binding.fastScroller.updateLayoutParams<ViewGroup.MarginLayoutParams> {
                        topMargin = binding.libraryGridRecycler.recycler.paddingTop
                    }
                    binding.headerTitle.updatePaddingRelative(top = insets.systemWindowInsetTop + 2.dpToPx)
                },
                onLeavingController = {
                    binding.headerTitle.gone()
                },
                onBottomNavUpdate = {
                    updateFilterSheetY()
                }
            )

        binding.swipeRefresh.setOnRefreshListener {
            binding.swipeRefresh.isRefreshing = false
            if (!LibraryUpdateService.isRunning()) {
                when {
                    !presenter.showAllCategories && presenter.groupType == BY_DEFAULT -> {
                        presenter.allCategories.find { it.id == presenter.currentCategory }?.let {
                            updateLibrary(it)
                        }
                    }
                    presenter.allCategories.size <= 1 || presenter.groupType > BY_DEFAULT -> {
                        updateLibrary()
                    }
                    preferences.updateOnRefresh().getOrDefault() == -1 -> {
                        MaterialDialog(activity!!).title(R.string.what_should_update)
                            .negativeButton(android.R.string.cancel)
                            .listItemsSingleChoice(
                                items = listOf(
                                    view.context.getString(
                                        R.string.top_category,
                                        presenter.allCategories.first().name
                                    ),
                                    view.context.getString(
                                        R.string.categories_in_global_update
                                    )
                                ),
                                selection = { _, index, _ ->
                                    preferences.updateOnRefresh().set(index)
                                    when (index) {
                                        0 -> updateLibrary(presenter.allCategories.first())
                                        else -> updateLibrary()
                                    }
                                }
                            ).positiveButton(R.string.update).show()
                    }
                    else -> {
                        when (preferences.updateOnRefresh().getOrDefault()) {
                            0 -> updateLibrary(presenter.allCategories.first())
                            else -> updateLibrary()
                        }
                    }
                }
            }
        }

        if (selectedMangas.isNotEmpty()) {
            createActionModeIfNeeded()
        }

        presenter.onRestore()
        if (presenter.libraryItems.isNotEmpty()) {
            presenter.restoreLibrary()
        } else {
            binding.recyclerLayout.alpha = 0f
            presenter.getLibrary()
        }
    }

    private fun setupFilterSheet() {
        binding.filterBottomSheet.filterBottomSheet.onCreate(this)

        binding.filterBottomSheet.filterBottomSheet.onGroupClicked = {
            when (it) {
                FilterBottomSheet.ACTION_REFRESH -> onRefresh()
                FilterBottomSheet.ACTION_FILTER -> onFilterChanged()
                FilterBottomSheet.ACTION_HIDE_FILTER_TIP -> showFilterTip()
                FilterBottomSheet.ACTION_DISPLAY -> {
                    displaySheet = TabbedLibraryDisplaySheet(this)
                    displaySheet?.show()
                }
                FilterBottomSheet.ACTION_EXPAND_COLLAPSE_ALL -> presenter.toggleAllCategoryVisibility()
                FilterBottomSheet.ACTION_GROUP_BY -> {
                    val groupItems = mutableListOf(BY_DEFAULT, BY_TAG, BY_SOURCE, BY_STATUS)
                    if (presenter.isLoggedIntoTracking) {
                        groupItems.add(BY_TRACK_STATUS)
                    }
                    if (presenter.allCategories.size > 1) {
                        groupItems.add(UNGROUPED)
                    }
                    val items = groupItems.map { id ->
                        MaterialMenuSheet.MenuSheetItem(
                            id,
                            LibraryGroup.groupTypeDrawableRes(id),
                            LibraryGroup.groupTypeStringRes(id, presenter.allCategories.size > 1)
                        )
                    }
                    MaterialMenuSheet(
                        activity!!,
                        items,
                        activity!!.getString(R.string.group_library_by),
                        presenter.groupType
                    ) { _, item ->
                        preferences.groupLibraryBy().set(item)
                        presenter.groupType = item
                        shouldScrollToTop = true
                        presenter.getLibrary()
                        true
                    }.show()
                }
            }
        }
    }

    @SuppressLint("RtlHardcoded", "ClickableViewAccessibility")
    private fun setUpHopper() {
        binding.categoryHopperFrame.gone()
        binding.roundedCategoryHopper.downCategory.setOnClickListener {
            jumpToNextCategory(true)
        }
        binding.roundedCategoryHopper.upCategory.setOnClickListener {
            jumpToNextCategory(false)
        }
        binding.roundedCategoryHopper.downCategory.setOnLongClickListener {
            binding.libraryGridRecycler.recycler.scrollToPosition(adapter.itemCount - 1)
            true
        }
        binding.roundedCategoryHopper.upCategory.setOnLongClickListener {
            binding.libraryGridRecycler.recycler.scrollToPosition(0)
            true
        }
        binding.roundedCategoryHopper.categoryButton.setOnClickListener {
            val items = presenter.categories.map { category ->
                MaterialMenuSheet.MenuSheetItem(category.order, text = category.name)
            }
            MaterialMenuSheet(
                activity!!,
                items,
                it.context.getString(R.string.jump_to_category),
                activeCategory,
                300.dpToPx
            ) { _, item ->
                scrollToHeader(item)
                true
            }.show()
        }

        binding.roundedCategoryHopper.categoryButton.setOnLongClickListener {
            activityBinding?.toolbar?.menu?.performIdentifierAction(R.id.action_search, 0)
            true
        }

        val gravityPref = if (!hasMovedHopper) {
            Random.nextInt(0..2)
        } else {
            preferences.hopperGravity().get()
        }
        hideHopper(preferences.hideHopper().get())
        binding.categoryHopperFrame.updateLayoutParams<CoordinatorLayout.LayoutParams> {
            gravity = Gravity.TOP or when (gravityPref) {
                0 -> Gravity.LEFT
                2 -> Gravity.RIGHT
                else -> Gravity.CENTER
            }
        }
        hopperGravity = gravityPref

        val gestureDetector = GestureDetectorCompat(activity, LibraryGestureDetector(this))
        with(binding.roundedCategoryHopper) {
            listOf(categoryHopperLayout, upCategory, downCategory, categoryButton).forEach {
                it.setOnTouchListener { _, event ->
                    if (event?.action == MotionEvent.ACTION_DOWN) {
                        animatorSet?.end()
                    }
                    if (event?.action == MotionEvent.ACTION_UP) {
                        val result = gestureDetector.onTouchEvent(event)
                        if (!result) {
                            binding.categoryHopperFrame.animate().setDuration(150L).translationX(0f)
                                .start()
                        }
                        result
                    } else {
                        gestureDetector.onTouchEvent(event)
                    }
                }
            }
        }
    }

    fun updateHopperY() {
        val view = view ?: return
        val listOfYs = mutableListOf(
            binding.filterBottomSheet.filterBottomSheet.y,
            activityBinding?.bottomNav?.y ?: binding.filterBottomSheet.filterBottomSheet.y
        )
        val insetBottom = view.rootWindowInsets?.systemWindowInsetBottom ?: 0
        if (!preferences.autohideHopper().get()) {
            listOfYs.add(view.height - (insetBottom).toFloat())
        }
        binding.categoryHopperFrame.y = -binding.categoryHopperFrame.height +
            (listOfYs.minOrNull() ?: binding.filterBottomSheet.filterBottomSheet.y) +
            hopperOffset +
            binding.libraryGridRecycler.recycler.translationY
        if (view.height - insetBottom < binding.categoryHopperFrame.y) {
            binding.jumperCategoryText.translationY =
                -(binding.categoryHopperFrame.y - (view.height - insetBottom)) +
                binding.libraryGridRecycler.recycler.translationY
        } else {
            binding.jumperCategoryText.translationY = binding.libraryGridRecycler.recycler.translationY
        }
    }

    fun resetHopperY() {
        hopperOffset = 0f
    }

    fun hideHopper(hide: Boolean) {
        binding.categoryHopperFrame.visibleIf(!hide)
        binding.jumperCategoryText.visibleIf(!hide)
    }

    private fun jumpToNextCategory(next: Boolean) {
        val category = getVisibleHeader() ?: return
        if (presenter.showAllCategories) {
            if (!next) {
                val fPosition =
                    (binding.libraryGridRecycler.recycler.layoutManager as LinearLayoutManager).findFirstVisibleItemPosition()
                if (fPosition != adapter.currentItems.indexOf(category)) {
                    scrollToHeader(category.category.order)
                    return
                }
            }
            val newOffset = adapter.headerItems.indexOf(category) + (if (next) 1 else -1)
            if (if (!next) newOffset > -1 else newOffset < adapter.headerItems.size) {
                val newCategory = (adapter.headerItems[newOffset] as LibraryHeaderItem).category
                val newOrder = newCategory.order
                scrollToHeader(newOrder)
                showCategoryText(newCategory.name)
            } else {
                binding.libraryGridRecycler.recycler.scrollToPosition(if (next) adapter.itemCount - 1 else 0)
            }
        } else {
            val newOffset =
                presenter.categories.indexOfFirst { presenter.currentCategory == it.id } +
                    (if (next) 1 else -1)
            if (if (!next) {
                newOffset > -1
            } else {
                    newOffset < presenter.categories.size
                }
            ) {
                val newCategory = presenter.categories[newOffset]
                val newOrder = newCategory.order
                scrollToHeader(newOrder)
                showCategoryText(newCategory.name)
            }
        }
    }

    private fun getHeader(firstCompletelyVisible: Boolean = false): LibraryHeaderItem? {
        val position = if (firstCompletelyVisible) {
            (binding.libraryGridRecycler.recycler.layoutManager as LinearLayoutManager).findFirstCompletelyVisibleItemPosition()
        } else {
            -1
        }
        if (position > 0) {
            when (val item = adapter.getItem(position)) {
                is LibraryHeaderItem -> return item
                is LibraryItem -> return item.header
            }
        } else {
            val fPosition =
                (binding.libraryGridRecycler.recycler.layoutManager as LinearLayoutManager).findFirstVisibleItemPosition()
            when (val item = adapter.getItem(fPosition)) {
                is LibraryHeaderItem -> return item
                is LibraryItem -> return item.header
            }
        }
        return null
    }

    private fun getVisibleHeader(): LibraryHeaderItem? {
        val fPosition =
            (binding.libraryGridRecycler.recycler.layoutManager as LinearLayoutManager).findFirstVisibleItemPosition()
        when (val item = adapter.getItem(fPosition)) {
            is LibraryHeaderItem -> return item
            is LibraryItem -> return item.header
        }
        return adapter.headerItems.firstOrNull() as? LibraryHeaderItem
    }

    override fun inflateView(inflater: LayoutInflater, container: ViewGroup): View {
        binding = LibraryListControllerBinding.inflate(inflater)
        return binding.root
    }

    private fun anchorView(): View {
        return if (binding.categoryHopperFrame.isVisible()) {
            binding.categoryHopperFrame
        } else {
            binding.filterBottomSheet.filterBottomSheet
        }
    }

    private fun updateLibrary(category: Category? = null) {
        val view = view ?: return
        LibraryUpdateService.start(view.context, category)
        snack = view.snack(R.string.updating_library) {
            anchorView = anchorView()
            view.elevation = 15f.dpToPx
            setAction(R.string.cancel) {
                LibraryUpdateService.stop(context)
                Handler().post { NotificationReceiver.dismissNotification(context, Notifications.ID_LIBRARY_PROGRESS) }
            }
        }
    }

    private fun setRecyclerLayout() {
        binding.libraryGridRecycler.recycler.post {
            binding.libraryGridRecycler.recycler.updatePaddingRelative(bottom = 50.dpToPx + (activityBinding?.bottomNav?.height ?: 0))
        }
        if (libraryLayout == 0) {
            binding.libraryGridRecycler.recycler.spanCount = 1
            binding.libraryGridRecycler.recycler.updatePaddingRelative(
                start = 0,
                end = 0
            )
        } else {
            binding.libraryGridRecycler.recycler.columnWidth = when (preferences.gridSize().getOrDefault()) {
                1 -> 1f
                2 -> 1.25f
                3 -> 1.66f
                4 -> 3f
                else -> .75f
            }
            binding.libraryGridRecycler.recycler.updatePaddingRelative(
                start = 5.dpToPx,
                end = 5.dpToPx
            )
        }
    }

    override fun onChangeStarted(handler: ControllerChangeHandler, type: ControllerChangeType) {
        super.onChangeStarted(handler, type)
        if (type.isEnter) {
            binding.filterBottomSheet.filterBottomSheet.visible()
            presenter.getLibrary()
            DownloadService.callListeners()
            LibraryUpdateService.setListener(this)
            binding.recyclerCover.isClickable = false
            binding.recyclerCover.isFocusable = false
            showDropdown()
        } else {
            updateFilterSheetY()
            closeTip()
            if (binding.filterBottomSheet.filterBottomSheet.sheetBehavior.isHidden()) {
                binding.filterBottomSheet.filterBottomSheet.invisible()
            }
            activityBinding?.toolbar?.hideDropdown()
        }
    }

    override fun onChangeEnded(handler: ControllerChangeHandler, type: ControllerChangeType) {
        super.onChangeEnded(handler, type)
        if (!type.isEnter) {
            activityBinding?.toolbar?.hideDropdown()
        }
    }

    override fun onActivityResumed(activity: Activity) {
        super.onActivityResumed(activity)
        if (view == null) return
        if (observeLater && ::presenter.isInitialized) {
            presenter.getLibrary()
        }
    }

    override fun onActivityPaused(activity: Activity) {
        super.onActivityPaused(activity)
        observeLater = true
        if (::presenter.isInitialized) presenter.onDestroy()
    }

    override fun onDestroy() {
        if (::presenter.isInitialized) presenter.onDestroy()
        super.onDestroy()
    }

    override fun onDestroyView(view: View) {
        LibraryUpdateService.removeListener(this)
        destroyActionModeIfNeeded()
        binding.libraryGridRecycler.recycler.removeOnScrollListener(scrollListener)
        super.onDestroyView(view)
    }

    fun onNextLibraryUpdate(mangaMap: List<LibraryItem>, freshStart: Boolean = false) {
        view ?: return
        destroyActionModeIfNeeded()
        if (mangaMap.isNotEmpty()) {
            binding.emptyView.hide()
        } else {
            binding.emptyView.show(
                R.drawable.ic_heart_off_24dp,
                if (binding.filterBottomSheet.filterBottomSheet.hasActiveFilters()) R.string.no_matches_for_filters
                else R.string.library_is_empty_add_from_browse
            )
        }
        adapter.setItems(mangaMap)
        if (binding.libraryGridRecycler.recycler.itemAnimator == null) {
            binding.libraryGridRecycler.recycler.post {
                binding.libraryGridRecycler.recycler.itemAnimator = DefaultItemAnimator()
            }
        }
        singleCategory = presenter.categories.size <= 1
        showDropdown()
        binding.progress.gone()
        if (!freshStart) {
            justStarted = false
            if (binding.recyclerLayout.alpha == 0f) binding.recyclerLayout.animate().alpha(1f).setDuration(500)
                .start()
        } else binding.recyclerLayout.alpha = 1f
        if (justStarted && freshStart) {
            scrollToHeader(activeCategory)
        }
        binding.libraryGridRecycler.recycler.post {
            elevateAppBar(binding.libraryGridRecycler.recycler.canScrollVertically(-1))
            setActiveCategory()
        }

        binding.categoryHopperFrame.visibleIf(!singleCategory && !preferences.hideHopper().get())
        binding.filterBottomSheet.filterBottomSheet.updateButtons(
            showExpand = !singleCategory && presenter.showAllCategories,
            groupType = presenter.groupType
        )
        adapter.isLongPressDragEnabled = canDrag()
        binding.categoryRecycler.setCategories(presenter.categories)
        binding.filterBottomSheet.filterBottomSheet.setExpandText(preferences.collapsedCategories().getOrDefault().isNotEmpty())
        if (shouldScrollToTop) {
            binding.libraryGridRecycler.recycler.scrollToPosition(0)
            shouldScrollToTop = false
        }
        if (onRoot) {
            listOf(activityBinding?.toolbar, binding.headerTitle).forEach {
                it?.setOnClickListener {
                    val recycler = binding.libraryGridRecycler.recycler
                    if (singleCategory) {
                        recycler.scrollToPosition(0)
                    } else {
                        showCategories(recycler.translationY == 0f)
                    }
                }
                if (!hasMovedHopper && isAnimatingHopper == null) {
                    showSlideAnimation()
                }
            }
            showMiniBar()
        }
    }

    private fun showDropdown() {
        if (onRoot) {
            if (!singleCategory) {
                activityBinding?.toolbar?.showDropdown()
            } else {
                activityBinding?.toolbar?.hideDropdown()
            }
        }
    }

    private fun showSlideAnimation() {
        isAnimatingHopper = true
        val slide = 25f.dpToPx
        val animatorSet = AnimatorSet()
        this.animatorSet = animatorSet
        val animations = listOf(
            slideAnimation(0f, slide, 200),
            slideAnimation(slide, -slide),
            slideAnimation(-slide, slide),
            slideAnimation(slide, -slide),
            slideAnimation(-slide, 0f, 233)
        )
        animatorSet.playSequentially(animations)
        animatorSet.startDelay = 1250
        animatorSet.addListener(
            EndAnimatorListener {
                binding.categoryHopperFrame.translationX = 0f
                isAnimatingHopper = false
                this.animatorSet = null
            }
        )
        animatorSet.start()
    }

    private fun slideAnimation(from: Float, to: Float, duration: Long = 400): ObjectAnimator {
        return ObjectAnimator.ofFloat(binding.categoryHopperFrame, View.TRANSLATION_X, from, to)
            .setDuration(duration)
    }

    private fun showCategories(show: Boolean) {
        binding.recyclerCover.isClickable = show
        binding.recyclerCover.isFocusable = show
        val full = binding.categoryRecycler.height.toFloat() + binding.libraryGridRecycler.recycler.paddingTop
        val translateY = if (show) full else 0f
        binding.libraryGridRecycler.recycler.animate().translationY(translateY).apply {
            setUpdateListener {
                activityBinding?.appBar?.y = 0f
                updateHopperY()
            }
        }.start()
        binding.recyclerShadow.animate().translationY(translateY - 8.dpToPx).start()
        binding.recyclerCover.animate().translationY(translateY).start()
        binding.recyclerCover.animate().alpha(if (show) 0.75f else 0f).start()
        binding.libraryGridRecycler.recycler.suppressLayout(show)
        activityBinding?.toolbar?.showDropdown(!show)
        binding.swipeRefresh.isEnabled = !show
        setTitle()
        if (show) {
            binding.categoryRecycler.scrollToCategory(activeCategory)
            binding.fastScroller.hideScrollbar()
            activityBinding?.appBar?.y = 0f
            elevateAppBar(false)
            binding.filterBottomSheet.filterBottomSheet.sheetBehavior?.hide()
        } else {
            val notAtTop = binding.libraryGridRecycler.recycler.canScrollVertically(-1)
            elevateAppBar(notAtTop)
        }
    }

    private fun scrollToHeader(pos: Int) {
        if (!presenter.showAllCategories) {
            presenter.switchSection(pos)
            activeCategory = pos
            setActiveCategory()
            shouldScrollToTop = true
            return
        }
        val headerPosition = adapter.indexOf(pos)
        if (headerPosition > -1) {
            val appbar = activityBinding?.appBar
            binding.libraryGridRecycler.recycler.suppressLayout(true)
            val appbarOffset = if (appbar?.y ?: 0f > -20) 0 else (
                appbar?.y?.plus(
                    view?.rootWindowInsets?.systemWindowInsetTop ?: 0
                ) ?: 0f
                ).roundToInt() + 30.dpToPx
            val previousHeader = adapter.getItem(adapter.indexOf(pos - 1)) as? LibraryHeaderItem
            (binding.libraryGridRecycler.recycler.layoutManager as LinearLayoutManager).scrollToPositionWithOffset(
                headerPosition,
                (
                    when {
                        headerPosition == 0 -> 0
                        previousHeader?.category?.isHidden == true -> (-3).dpToPx
                        else -> (-30).dpToPx
                    }
                    ) + appbarOffset
            )
            (adapter.getItem(headerPosition) as? LibraryHeaderItem)?.category?.let {
                saveActiveCategory(it)
            }
            activeCategory = pos
            preferences.lastUsedCategory().set(pos)
            binding.libraryGridRecycler.recycler.suppressLayout(false)
        }
    }

    private fun onRefresh() {
        activity?.invalidateOptionsMenu()
        showCategories(false)
        presenter.getLibrary()
        destroyActionModeIfNeeded()
    }

    /**
     * Called when a filter is changed.
     */
    private fun onFilterChanged() {
        activity?.invalidateOptionsMenu()
        presenter.requestFilterUpdate()
        destroyActionModeIfNeeded()
    }

    fun reattachAdapter() {
        libraryLayout = preferences.libraryLayout().getOrDefault()
        setRecyclerLayout()
        val position =
            (binding.libraryGridRecycler.recycler.layoutManager as LinearLayoutManager).findFirstVisibleItemPosition()
        binding.libraryGridRecycler.recycler.adapter = adapter

        (binding.libraryGridRecycler.recycler.layoutManager as LinearLayoutManager).scrollToPositionWithOffset(position, 0)
    }

    fun search(query: String?): Boolean {
        this.query = query ?: ""
        if (this.query.isNotBlank() && adapter.scrollableHeaders.isEmpty()) {
            searchItem.string = this.query
            adapter.addScrollableHeader(searchItem)
        } else if (this.query.isNotBlank()) {
            searchItem.string = this.query
            (binding.libraryGridRecycler.recycler.findViewHolderForAdapterPosition(0) as? SearchGlobalItem.Holder)?.bind(this.query)
        } else if (this.query.isBlank() && adapter.scrollableHeaders.isNotEmpty()) {
            adapter.removeAllScrollableHeaders()
        }
        adapter.setFilter(query)
        adapter.performFilter()
        return true
    }

    override fun onDestroyActionMode(mode: ActionMode?) {
        selectedMangas.clear()
        actionMode = null
        adapter.mode = SelectableAdapter.Mode.SINGLE
        adapter.clearSelection()
        adapter.notifyDataSetChanged()
        lastClickPosition = -1
        adapter.isLongPressDragEnabled = canDrag()
    }

    private fun setSelection(manga: Manga, selected: Boolean) {
        if (manga.isBlank()) return
        val currentMode = adapter.mode
        if (selected) {
            if (selectedMangas.add(manga)) {
                val positions = adapter.allIndexOf(manga)
                if (adapter.mode != SelectableAdapter.Mode.MULTI) {
                    adapter.mode = SelectableAdapter.Mode.MULTI
                }
                launchUI {
                    delay(100)
                    adapter.isLongPressDragEnabled = false
                }
                positions.forEach { position ->
                    adapter.addSelection(position)
                    (binding.libraryGridRecycler.recycler.findViewHolderForAdapterPosition(position) as? LibraryHolder)?.toggleActivation()
                }
            }
        } else {
            if (selectedMangas.remove(manga)) {
                val positions = adapter.allIndexOf(manga)
                lastClickPosition = -1
                if (selectedMangas.isEmpty()) {
                    adapter.mode = SelectableAdapter.Mode.SINGLE
                    adapter.isLongPressDragEnabled = canDrag()
                }
                positions.forEach { position ->
                    adapter.removeSelection(position)
                    (binding.libraryGridRecycler.recycler.findViewHolderForAdapterPosition(position) as? LibraryHolder)?.toggleActivation()
                }
            }
        }
        updateHeaders(currentMode != adapter.mode)
    }

    private fun updateHeaders(changedMode: Boolean = false) {
        val headerPositions = adapter.getHeaderPositions()
        headerPositions.forEach {
            if (changedMode) {
                adapter.notifyItemChanged(it)
            } else {
                (binding.libraryGridRecycler.recycler.findViewHolderForAdapterPosition(it) as? LibraryHeaderHolder)?.setSelection()
            }
        }
    }

    override fun startReading(position: Int) {
        if (adapter.mode == SelectableAdapter.Mode.MULTI) {
            toggleSelection(position)
            return
        }
        val manga = (adapter.getItem(position) as? LibraryItem)?.manga ?: return
        val activity = activity ?: return
        val chapter = presenter.getFirstUnread(manga) ?: return
        val intent = ReaderActivity.newIntent(activity, manga, chapter)
        destroyActionModeIfNeeded()
        startActivity(intent)
    }

    private fun toggleSelection(position: Int) {
        val item = adapter.getItem(position) as? LibraryItem ?: return
        if (item.manga.isBlank()) return
        setSelection(item.manga, !adapter.isSelected(position))
        invalidateActionMode()
    }

    override fun canDrag(): Boolean {
        val filterOff = !binding.filterBottomSheet.filterBottomSheet.hasActiveFilters() && presenter.groupType == BY_DEFAULT
        return filterOff && adapter.mode != SelectableAdapter.Mode.MULTI
    }

    /**
     * Called when a manga is clicked.
     *
     * @param position the position of the element clicked.
     * @return true if the item should be selected, false otherwise.
     */
    override fun onItemClick(view: View?, position: Int): Boolean {
        val item = adapter.getItem(position) as? LibraryItem ?: return false
        return if (adapter.mode == SelectableAdapter.Mode.MULTI) {
            lastClickPosition = position
            toggleSelection(position)
            false
        } else {
            openManga(item.manga)
            false
        }
    }

    private fun openManga(manga: Manga) = router.pushController(
        MangaDetailsController(
            manga
        ).withFadeTransaction()
    )

    /**
     * Called when a manga is long clicked.
     *
     * @param position the position of the element clicked.
     */
    override fun onItemLongClick(position: Int) {
        if (adapter.getItem(position) !is LibraryItem) return
        createActionModeIfNeeded()
        when {
            lastClickPosition == -1 -> setSelection(position)
            lastClickPosition > position -> for (i in position until lastClickPosition) setSelection(
                i
            )
            lastClickPosition < position -> for (i in lastClickPosition + 1..position) setSelection(
                i
            )
            else -> setSelection(position)
        }
        lastClickPosition = position
    }

    override fun globalSearch(query: String) {
        router.pushController(GlobalSearchController(query).withFadeTransaction())
    }

    override fun onActionStateChanged(viewHolder: RecyclerView.ViewHolder?, actionState: Int) {
        val position = viewHolder?.bindingAdapterPosition ?: return
        binding.swipeRefresh.isEnabled = actionState != ItemTouchHelper.ACTION_STATE_DRAG
        if (actionState == ItemTouchHelper.ACTION_STATE_DRAG) {
            if (lastItemPosition != null &&
                position != lastItemPosition &&
                lastItem == adapter.getItem(position)
            ) {
                // because for whatever reason you can repeatedly tap on a currently dragging manga
                adapter.removeSelection(position)
                (binding.libraryGridRecycler.recycler.findViewHolderForAdapterPosition(position) as? LibraryHolder)?.toggleActivation()
                adapter.moveItem(position, lastItemPosition!!)
            } else {
                lastItem = adapter.getItem(position)
                lastItemPosition = position
                onItemLongClick(position)
            }
        }
    }

    override fun onUpdateManga(manga: Manga?) {
        if (manga == null) adapter.notifyDataSetChanged()
        else presenter.updateManga()
    }

    private fun setSelection(position: Int, selected: Boolean = true) {
        val item = adapter.getItem(position) as? LibraryItem ?: return

        setSelection(item.manga, selected)
        invalidateActionMode()
    }

    override fun onItemMove(fromPosition: Int, toPosition: Int) {
        // Because padding a recycler causes it to scroll up we have to scroll it back down... wild
        if ((
            adapter.getItem(fromPosition) is LibraryItem &&
                adapter.getItem(fromPosition) is LibraryItem
            ) ||
            adapter.getItem(fromPosition) == null
        ) {
            binding.libraryGridRecycler.recycler.scrollBy(0, binding.libraryGridRecycler.recycler.paddingTop)
        }
        if (lastItemPosition == toPosition) lastItemPosition = null
        else if (lastItemPosition == null) lastItemPosition = fromPosition
    }

    override fun shouldMoveItem(fromPosition: Int, toPosition: Int): Boolean {
        if (adapter.isSelected(fromPosition)) toggleSelection(fromPosition)
        val item = adapter.getItem(fromPosition) as? LibraryItem ?: return false
        val newHeader = adapter.getSectionHeader(toPosition) as? LibraryHeaderItem
        if (toPosition < 1) return false
        return (adapter.getItem(toPosition) !is LibraryHeaderItem) && (
            newHeader?.category?.id == item.manga.category || !presenter.mangaIsInCategory(
                item.manga,
                newHeader?.category?.id
            )
            )
    }

    override fun onItemReleased(position: Int) {
        lastItem = null
        if (adapter.selectedItemCount > 0) {
            lastItemPosition = null
            return
        }
        destroyActionModeIfNeeded()
        // if nothing moved
        if (lastItemPosition == null) return
        val item = adapter.getItem(position) as? LibraryItem ?: return
        val newHeader = adapter.getSectionHeader(position) as? LibraryHeaderItem
        val libraryItems = adapter.getSectionItems(adapter.getSectionHeader(position))
            .filterIsInstance<LibraryItem>()
        val mangaIds = libraryItems.mapNotNull { (it as? LibraryItem)?.manga?.id }
        if (newHeader?.category?.id == item.manga.category) {
            presenter.rearrangeCategory(item.manga.category, mangaIds)
        } else {
            if (presenter.mangaIsInCategory(item.manga, newHeader?.category?.id)) {
                adapter.moveItem(position, lastItemPosition!!)
                snack = view?.snack(R.string.already_in_category) {
                    anchorView = anchorView()
                    view.elevation = 15f.dpToPx
                }
                return
            }
            if (newHeader?.category != null) moveMangaToCategory(
                item.manga,
                newHeader.category,
                mangaIds
            )
        }
        lastItemPosition = null
    }

    private fun moveMangaToCategory(
        manga: LibraryManga,
        category: Category?,
        mangaIds: List<Long>
    ) {
        if (category?.id == null) return
        val oldCatId = manga.category
        presenter.moveMangaToCategory(manga, category.id, mangaIds)
        snack?.dismiss()
        snack = view?.snack(
            resources!!.getString(R.string.moved_to_, category.name)
        ) {
            anchorView = anchorView()
            view.elevation = 15f.dpToPx
            setAction(R.string.undo) {
                manga.category = category.id!!
                presenter.moveMangaToCategory(manga, oldCatId, mangaIds)
            }
        }
    }

    override fun updateCategory(catId: Int): Boolean {
        val category = (adapter.getItem(catId) as? LibraryHeaderItem)?.category ?: return false
        val inQueue = LibraryUpdateService.categoryInQueue(category.id)
        snack?.dismiss()
        snack = view?.snack(
            resources!!.getString(
                when {
                    inQueue -> R.string._already_in_queue
                    LibraryUpdateService.isRunning() -> R.string.adding_category_to_queue
                    else -> R.string.updating_
                },
                category.name
            ),
            Snackbar.LENGTH_LONG
        ) {
            anchorView = anchorView()
            view.elevation = 15f.dpToPx
            setAction(R.string.cancel) {
                LibraryUpdateService.stop(context)
                Handler().post {
                    NotificationReceiver.dismissNotification(
                        context,
                        Notifications.ID_LIBRARY_PROGRESS
                    )
                }
            }
        }
        if (!inQueue) LibraryUpdateService.start(
            view!!.context,
            category,
            mangaToUse = if (category.isDynamic) {
                presenter.getMangaInCategories(category.id)
            } else null
        )
        return true
    }

    override fun toggleCategoryVisibility(position: Int) {
        if (!presenter.showAllCategories) {
            showCategories(true)
            return
        }
        val catId = (adapter.getItem(position) as? LibraryHeaderItem)?.category?.id ?: return
        presenter.toggleCategoryVisibility(catId)
    }

    override fun manageCategory(position: Int) {
        val category = (adapter.getItem(position) as? LibraryHeaderItem)?.category ?: return
        if (!category.isDynamic) {
            ManageCategoryDialog(this, category).showDialog(router)
        }
    }

    override fun sortCategory(catId: Int, sortBy: Int) {
        presenter.sortCategory(catId, sortBy)
    }

    override fun selectAll(position: Int) {
        val header = adapter.getSectionHeader(position) ?: return
        val items = adapter.getSectionItemPositions(header)
        val allSelected = allSelected(position)
        for (i in items) setSelection(i, !allSelected)
    }

    override fun allSelected(position: Int): Boolean {
        val header = adapter.getSectionHeader(position) ?: return false
        val items = adapter.getSectionItemPositions(header)
        return items.all { adapter.isSelected(it) }
    }

    //region sheet methods
    override fun showSheet() {
        closeTip()
        when {
            binding.filterBottomSheet.filterBottomSheet.sheetBehavior.isHidden() -> binding.filterBottomSheet.filterBottomSheet.sheetBehavior?.collapse()
            !binding.filterBottomSheet.filterBottomSheet.sheetBehavior.isExpanded() -> binding.filterBottomSheet.filterBottomSheet.sheetBehavior?.expand()
            else -> TabbedLibraryDisplaySheet(this).show()
        }
    }

    override fun toggleSheet() {
        closeTip()
        when {
            binding.filterBottomSheet.filterBottomSheet.sheetBehavior.isHidden() -> binding.filterBottomSheet.filterBottomSheet.sheetBehavior?.collapse()
            !binding.filterBottomSheet.filterBottomSheet.sheetBehavior.isExpanded() -> binding.filterBottomSheet.filterBottomSheet.sheetBehavior?.expand()
            else -> binding.filterBottomSheet.filterBottomSheet.sheetBehavior?.hide()
        }
    }

    override fun sheetIsExpanded(): Boolean = false

    override fun handleSheetBack(): Boolean {
        if (binding.recyclerCover.isClickable) {
            showCategories(false)
            return true
        }
        if (binding.filterBottomSheet.filterBottomSheet.sheetBehavior.isExpanded()) {
            binding.filterBottomSheet.filterBottomSheet.sheetBehavior?.collapse()
            return true
        }
        return false
    }
    //endregion

    //region Toolbar options methods
    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.library, menu)

        val searchItem = menu.findItem(R.id.action_search)
        val searchView = searchItem.actionView as SearchView
        searchView.queryHint = resources?.getString(R.string.library_search_hint)

        searchItem.collapseActionView()
        if (query.isNotEmpty()) {
            searchItem.expandActionView()
            searchView.setQuery(query, true)
            searchView.clearFocus()
        }

        setOnQueryTextChangeListener(searchView) { search(it) }
        searchItem.fixExpand(onExpand = { invalidateMenuOnExpand() })
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.action_search -> expandActionViewFromInteraction = true
            R.id.action_filter -> {
                hasExpanded = true
                binding.filterBottomSheet.filterBottomSheet.sheetBehavior?.expand()
            }
            else -> return super.onOptionsItemSelected(item)
        }
        return true
    }
    //endregion

    //region Action Mode Methods
    /**
     * Creates the action mode if it's not created already.
     */
    private fun createActionModeIfNeeded() {
        if (actionMode == null) {
            actionMode = (activity as AppCompatActivity).startSupportActionMode(this)
            val view = activity?.window?.currentFocus ?: return
            val imm =
                activity?.getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager
                    ?: return
            imm.hideSoftInputFromWindow(view.windowToken, 0)
        }
    }

    fun showCategoriesController() {
        router.pushController(CategoryController().withFadeTransaction())
        displaySheet?.dismiss()
    }

    /**
     * Destroys the action mode.
     */
    private fun destroyActionModeIfNeeded() {
        actionMode?.finish()
    }

    /**
     * Invalidates the action mode, forcing it to refresh its content.
     */
    private fun invalidateActionMode() {
        actionMode?.invalidate()
    }

    override fun onCreateActionMode(mode: ActionMode, menu: Menu): Boolean {
        mode.menuInflater.inflate(R.menu.library_selection, menu)
        return true
    }

    override fun onPrepareActionMode(mode: ActionMode, menu: Menu): Boolean {
        val count = selectedMangas.size
        // Destroy action mode if there are no items selected.
        val migrationItem = menu.findItem(R.id.action_migrate)
        val shareItem = menu.findItem(R.id.action_share)
        val categoryItem = menu.findItem(R.id.action_move_to_category)
        categoryItem.isVisible = presenter.categories.size > 1
        migrationItem.isVisible = selectedMangas.any { it.source != LocalSource.ID }
        shareItem.isVisible = migrationItem.isVisible
        if (count == 0) destroyActionModeIfNeeded()
        else mode.title = resources?.getString(R.string.selected_, count)
        return false
    }
    //endregion

    override fun onActionItemClicked(mode: ActionMode, item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.action_move_to_category -> showChangeMangaCategoriesDialog()
            R.id.action_share -> shareManga()
            R.id.action_delete -> {
                MaterialDialog(activity!!).message(R.string.remove_from_library_question)
                    .positiveButton(R.string.remove) {
                        deleteMangasFromLibrary()
                    }.negativeButton(android.R.string.no).show()
            }
            R.id.action_download_unread -> {
                presenter.downloadUnread(selectedMangas.toList())
            }
            R.id.action_mark_as_read -> {
                presenter.markReadStatus(selectedMangas.toList(), true)
                destroyActionModeIfNeeded()
            }
            R.id.action_mark_as_unread -> {
                presenter.markReadStatus(selectedMangas.toList(), false)
                destroyActionModeIfNeeded()
            }
            R.id.action_migrate -> {
                val skipPre = preferences.skipPreMigration().getOrDefault()
                PreMigrationController.navigateToMigration(
                    skipPre,
                    router,
                    selectedMangas.filter { it.id != LocalSource.ID }.mapNotNull { it.id }
                )
                destroyActionModeIfNeeded()
            }
            else -> return false
        }
        return true
    }

    private fun shareManga() {
        val context = view?.context ?: return
        val mangas = selectedMangas.toList()
        val urlList = presenter.getMangaUrls(mangas)
        if (urlList.isEmpty()) return
        val urls = presenter.getMangaUrls(mangas).joinToString("\n")
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/*"
            putExtra(Intent.EXTRA_TEXT, urls)
        }
        startActivity(Intent.createChooser(intent, context.getString(R.string.share)))
    }

    private fun deleteMangasFromLibrary() {
        val mangas = selectedMangas.toList()
        presenter.removeMangaFromLibrary(mangas)
        destroyActionModeIfNeeded()
        snack?.dismiss()
        snack = view?.snack(
            activity?.getString(R.string.removed_from_library) ?: "",
            Snackbar.LENGTH_INDEFINITE
        ) {
            anchorView = anchorView()
            view.elevation = 15f.dpToPx
            var undoing = false
            setAction(R.string.undo) {
                presenter.reAddMangas(mangas)
                undoing = true
            }
            addCallback(
                object : BaseTransientBottomBar.BaseCallback<Snackbar>() {
                    override fun onDismissed(transientBottomBar: Snackbar?, event: Int) {
                        super.onDismissed(transientBottomBar, event)
                        if (!undoing) presenter.confirmDeletion(mangas)
                    }
                }
            )
        }
        (activity as? MainActivity)?.setUndoSnackBar(snack)
    }

    override fun updateCategoriesForMangas(mangas: List<Manga>, categories: List<Category>) {
        presenter.moveMangasToCategories(categories, mangas)
        destroyActionModeIfNeeded()
    }

    /**
     * Move the selected manga to a list of categories.
     */
    private fun showChangeMangaCategoriesDialog() {
        // Create a copy of selected manga
        val mangas = selectedMangas.toList()

        // Hide the default category because it has a different behavior than the ones from db.
        val categories = presenter.allCategories.filter { it.id != 0 }

        // Get indexes of the common categories to preselect.
        val commonCategoriesIndexes =
            presenter.getCommonCategories(mangas).map { categories.indexOf(it) }.toTypedArray()

        ChangeMangaCategoriesDialog(this, mangas, categories, commonCategoriesIndexes).showDialog(
            router
        )
    }
}
