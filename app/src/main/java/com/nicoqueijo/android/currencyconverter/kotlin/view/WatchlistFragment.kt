package com.nicoqueijo.android.currencyconverter.kotlin.view

import android.R.color.black
import android.R.color.white
import android.animation.LayoutTransition
import android.os.Bundle
import android.view.*
import android.view.animation.AnimationUtils
import android.widget.LinearLayout
import android.widget.ScrollView
import androidx.core.content.ContextCompat
import androidx.core.view.children
import androidx.core.view.forEachIndexed
import androidx.core.view.get
import androidx.core.view.isEmpty
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.FragmentNavigatorExtras
import androidx.navigation.fragment.findNavController
import com.getkeepsafe.taptargetview.TapTarget
import com.getkeepsafe.taptargetview.TapTargetSequence
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.snackbar.Snackbar
import com.jmedeisis.draglinearlayout.DragLinearLayout
import com.nicoqueijo.android.currencyconverter.R
import com.nicoqueijo.android.currencyconverter.kotlin.model.Currency
import com.nicoqueijo.android.currencyconverter.kotlin.util.Utils.Order.FIRST
import com.nicoqueijo.android.currencyconverter.kotlin.util.Utils.copyToClipboard
import com.nicoqueijo.android.currencyconverter.kotlin.util.Utils.hasOnlyOneElement
import com.nicoqueijo.android.currencyconverter.kotlin.util.Utils.hide
import com.nicoqueijo.android.currencyconverter.kotlin.util.Utils.isViewVisible
import com.nicoqueijo.android.currencyconverter.kotlin.util.Utils.show
import com.nicoqueijo.android.currencyconverter.kotlin.util.Utils.vibrate
import com.nicoqueijo.android.currencyconverter.kotlin.viewmodel.WatchlistViewModel
import dagger.hilt.android.AndroidEntryPoint
import dagger.hilt.android.scopes.FragmentScoped

@AndroidEntryPoint
@FragmentScoped
class WatchlistFragment : Fragment() {

    private val viewModel: WatchlistViewModel by viewModels()

    private lateinit var emptyList: LinearLayout
    private lateinit var dragLinearLayout: DragLinearLayout
    private lateinit var floatingActionButton: FloatingActionButton
    private lateinit var keyboard: DecimalNumberKeyboard
    private lateinit var scrollView: ScrollView
    private var menuItem: MenuItem? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.fragment_watchlist, container, false)
        viewModel.initDefaultCurrencies()
        setHasOptionsMenu(true)
        initViews(view)
        observeObservables()
        return view
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)
        inflater.inflate(R.menu.menu_remove_all, menu)
        this.menuItem = menu.findItem(R.id.remove_all)
        toggleViewVisibility()
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        fireRemoveAllDialog()
        return super.onOptionsItemSelected(item)
    }

    private fun fireRemoveAllDialog() {
        MaterialAlertDialogBuilder(requireActivity())
                .setTitle(resources.getString(R.string.remove_all_dialog_title))
                .setBackground(ContextCompat.getDrawable(requireActivity(), R.drawable.dialog_background))
                .setPositiveButton(resources.getString(R.string.ok)) { _, _ ->
                    removeAllCurrencies()
                    toggleViewVisibility()
                }
                .setNegativeButton(resources.getString(R.string.cancel)) { _, _ ->
                }
                .show()
    }

    private fun removeAllCurrencies() {
        viewModel.removeAllCurrencies()
        dragLinearLayout.removeAllViews()
    }

    private fun observeObservables() {
        viewModel.databaseSelectedCurrencies.observe(viewLifecycleOwner, { databaseSelectedCurrencies ->
            if (databaseSelectedCurrencies.isNotEmpty()) {
                initSelectedCurrencies(databaseSelectedCurrencies)
                styleRows()
            }
            toggleViewVisibility()
        })
        viewModel.focusedCurrency.observe(viewLifecycleOwner, {
            updateHints()
        })
    }

    private fun initViews(view: View) {
        emptyList = view.findViewById(R.id.empty_list)
        scrollView = view.findViewById(R.id.scroll_view)
        keyboard = view.findViewById(R.id.keyboard)
        adjustViews(emptyList, keyboard)
        initDragLinearLayout(view)
        initFloatingActionButton(view)
        initKeyboardListener()
        if (viewModel.wasListConstructed) {
            restoreSelectedCurrencies()
        }
    }

    /**
     * If the device has a height of less than 1920px then the paddingTop of the emptyList is reduced
     * from 60dp to 20dp and the keyboards height is reduced from 250dp to 200dp. This is to optimize
     * screen space and not have any views cut out.
     */
    private fun adjustViews(emptyList: LinearLayout, keyboard: DecimalNumberKeyboard) {
        if (resources.displayMetrics.heightPixels < 1920) {
            emptyList.setPadding(0, (20f * resources.displayMetrics.density).toInt(), 0, 0)
            keyboard.layoutParams.height = (200f * resources.displayMetrics.density).toInt()
        }
    }

    private fun initDragLinearLayout(view: View) {
        dragLinearLayout = view.findViewById<DragLinearLayout>(R.id.drag_linear_layout).apply {
            setContainerScrollView(scrollView)
            setOnViewSwapListener { _, startPosition, _, endPosition ->
                viewModel.swapCurrencies(startPosition, endPosition)
            }
        }
    }

    private fun initFloatingActionButton(view: View) {
        floatingActionButton = view.findViewById<FloatingActionButton>(R.id.floating_action_button).apply {
            setOnClickListener {
                findNavController().navigate(R.id.action_watchlistFragment_to_selectorFragment,
                        null,
                        null,
                        FragmentNavigatorExtras(this to "shared_element_container"))
            }
        }
    }

    /**
     * On each key click event, we want to validate the input against what already is in the
     * TextView. If it is valid we want to run the conversion of that value against all other
     * currencies and update the TextView of all other currencies with he converter value.
     */
    private fun initKeyboardListener() {
        keyboard.onKeyClickedListener { button ->
            scrollToFocusedCurrency()
            if (viewModel.processKeyboardInput(button)) {
                viewModel.runConversions()
                updateRows()
            } else {
                vibrateAndShake()
            }
        }
        keyboard.onKeyLongClickedListener {
            scrollToFocusedCurrency()
            viewModel.clearConversions()
            updateRows()
        }
    }

    private fun vibrateAndShake() {
        keyboard.context.vibrate()
        viewModel.run {
            val focusedRow = (dragLinearLayout[memorySelectedCurrencies
                    .indexOf(focusedCurrency.value)] as RowSelectedCurrency)
            focusedRow.conversion.startAnimation(AnimationUtils
                    .loadAnimation(getApplication(), R.anim.shake))
        }
    }

    private fun scrollToFocusedCurrency() {
        viewModel.focusedCurrency.value?.let {
            val focusedRow = dragLinearLayout.getChildAt(viewModel.memorySelectedCurrencies.indexOf(it))
            if (!scrollView.isViewVisible(focusedRow)) {
                scrollView.smoothScrollTo(0, focusedRow.top)
            }
        }
    }

    private fun restoreSelectedCurrencies() {
        viewModel.memorySelectedCurrencies.forEach { addRow(it) }
    }

    /**
     * Determines how it should inflate the list of currencies when the database storing the state
     * of the currencies emits updates.
     */
    private fun initSelectedCurrencies(databaseSelectedCurrencies: List<Currency>) {
        viewModel.run {
            if (!wasListConstructed) {
                constructSelectedCurrencies(databaseSelectedCurrencies)
            }
            if (wasCurrencyAddedViaFab(databaseSelectedCurrencies)) {
                databaseSelectedCurrencies.takeLast(1).single().let {
                    memorySelectedCurrencies.add(it)
                    addRow(it)
                    if (!memorySelectedCurrencies.hasOnlyOneElement()) {
                        runConversions()
                        updateRows()
                        scrollToFocusedCurrency()
                    }
                }
                this@WatchlistFragment.updateHints()
            }
            setDefaultFocus()
        }
    }

    private fun updateHints() {
        viewModel.focusedCurrency.value?.let {
            viewModel.updateHints()
            dragLinearLayout.children
                    .forEachIndexed { i, row ->
                        row as RowSelectedCurrency
                        row.conversion.hint = viewModel.memorySelectedCurrencies[i].conversion.conversionHint
                    }
        }
    }

    /**
     * This inflates the DragLinearLayout with the selected currencies from the database when the
     * activity starts for the first time.
     */
    private fun constructSelectedCurrencies(databaseSelectedCurrencies: List<Currency>) {
        databaseSelectedCurrencies.forEach { currency ->
            viewModel.memorySelectedCurrencies.add(currency)
            addRow(currency)
        }
        viewModel.wasListConstructed = true
        if (viewModel.isFirstLaunch()) {
//            showTargets()
            viewModel.setFirstLaunch(false)
        }
    }

    /**
     * Walks the user through some of the app's features on the first launch.
     */
    private fun showTargets() {
        val addCurrencyTarget = buildTapTarget(floatingActionButton, getString(R.string.add_target_message))
        val dragCurrencyTarget = buildTapTarget((dragLinearLayout[FIRST.position] as RowSelectedCurrency).flag,
                getString(R.string.drag_target_message))
        val removeCurrencyTarget = buildTapTarget((dragLinearLayout[FIRST.position] as RowSelectedCurrency).currencyCode,
                getString(R.string.remove_target_message))
        TapTargetSequence(this.activity)
                .targets(addCurrencyTarget, dragCurrencyTarget, removeCurrencyTarget)
                .start()
    }

    private fun buildTapTarget(view: View, title: CharSequence): TapTarget {
        return TapTarget.forView(view, title)
                .outerCircleColor(white)
                .outerCircleAlpha(0.90f)
                .targetCircleColor(black)
                .textColor(black)
                .cancelable(false)
                .transparentTarget(true)
                .targetRadius(50)
    }

    private fun styleRows() {
        dragLinearLayout.forEachIndexed { i, row ->
            styleRow(viewModel.memorySelectedCurrencies[i], row as RowSelectedCurrency)
        }
    }

    /**
     * Styles the row in accordance to the focus state of its Currency. A row containing a focused
     * Currency should have blinking cursor at the end of its conversion field and a dark gray background.
     */
    private fun styleRow(currency: Currency, row: RowSelectedCurrency) {
        row.run {
            if (currency.isFocused) {
                rowCanvas.setBackgroundColor(ContextCompat.getColor(context, R.color.dark_gray))
                blinkingCursor.startAnimation(AnimationUtils.loadAnimation(viewModel.getApplication(), R.anim.blink))
            } else {
                rowCanvas.background = ContextCompat.getDrawable(context, R.drawable.row_selected_currency_background)
                blinkingCursor.clearAnimation()
            }
        }
    }

    private fun updateRows() {
        dragLinearLayout.children
                .forEachIndexed { i, row ->
                    row as RowSelectedCurrency
                    row.conversion.text = viewModel.memorySelectedCurrencies[i].conversion.conversionText
                }
    }

    /**
     * Creates a row from a [currency], adds that row to the DragLinearLayout, and sets up
     * its listeners so it could be dragged, removed, and restored.
     */
    private fun addRow(currency: Currency) {
        RowSelectedCurrency(activity).run row@{
            initRow(currency)
            dragLinearLayout.run {
                addView(this@row)
                setViewDraggable(this@row, this@row)
                /**
                 * Removes this [currency] and adjusts the state accordingly.
                 */
                currencyCode.setOnLongClickListener {
                    val currencyToRemove = viewModel.memorySelectedCurrencies[indexOfChild(this@row)]
                    val positionOfCurrencyToRemove = currencyToRemove.order
                    viewModel.handleRemove(currencyToRemove, positionOfCurrencyToRemove)
                    activity?.vibrate()
                    layoutTransition = LayoutTransition()
                    removeDragView(this@row)
                    layoutTransition = null
                    styleRows()
                    toggleViewVisibility()
                    /**
                     * Restores the removed currency and restores the state before the currency was removed.
                     */
                    Snackbar.make(this, R.string.item_removed, Snackbar.LENGTH_SHORT)
                            .setAction(R.string.undo) {
                                layoutTransition = LayoutTransition()
                                addDragView(this@row, this@row, positionOfCurrencyToRemove)
                                layoutTransition = null
                                viewModel.handleUndo(currencyToRemove, positionOfCurrencyToRemove)
                                toggleViewVisibility()
                            }.show()
                    true
                }
                conversion.setOnLongClickListener {
                    activity?.copyToClipboard(this@row.conversion.text)
                    true
                }
                conversion.setOnClickListener {
                    viewModel.changeFocusedCurrency(indexOfChild(this@row))
                    styleRows()
                }
            }
        }
    }

    private fun toggleViewVisibility() {
        when (dragLinearLayout.isEmpty()) {
            true -> {
                emptyList.show()
                menuItem?.hide()
            }
            false -> {
                emptyList.hide()
                menuItem?.show()
            }
        }
    }
}
