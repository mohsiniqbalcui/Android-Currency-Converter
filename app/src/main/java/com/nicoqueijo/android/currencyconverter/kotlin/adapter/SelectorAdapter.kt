package com.nicoqueijo.android.currencyconverter.kotlin.adapter

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.Filter
import android.widget.Filterable
import androidx.navigation.findNavController
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.futuremind.recyclerviewfastscroll.SectionTitleProvider
import com.nicoqueijo.android.currencyconverter.databinding.RowSelectorBinding
import com.nicoqueijo.android.currencyconverter.kotlin.model.Currency
import com.nicoqueijo.android.currencyconverter.kotlin.util.CurrencyDiffUtilCallback
import com.nicoqueijo.android.currencyconverter.kotlin.viewmodel.SelectorViewModel

class SelectorAdapter(private val viewModel: SelectorViewModel) :
    ListAdapter<Currency, SelectorAdapter.ViewHolder>(CurrencyDiffUtilCallback()),
    SectionTitleProvider,
    Filterable {

//    private lateinit var interstitialAd: InterstitialAd

//    init {
//        if (viewModel.willShowAd) {
//            interstitialAd = InterstitialAd(viewModel.getApplication())
////            interstitialAd.adUnitId = viewModel.getInterstitialAdId(viewModel.getApplication())
//            interstitialAd.loadAd(AdRequest.Builder().build())
//        }
//    }

    inner class ViewHolder(private val binding: RowSelectorBinding) :
        RecyclerView.ViewHolder(binding.root) {

        init {
            itemView.setOnClickListener { view ->
                viewModel.handleOnClick(adapterPosition)
                view?.findNavController()?.popBackStack()
//                if (viewModel.willShowAd && interstitialAd.isLoaded) {
//                    interstitialAd.show()
//                }
            }
        }

        fun bind(currency: Currency) {
            binding.currency = currency
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = RowSelectorBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    @SuppressLint("DefaultLocale")
    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(viewModel.adapterFilteredCurrencies[position])
    }

    fun setCurrencies(currencies: List<Currency>) {
        viewModel.adapterFilteredCurrencies = ArrayList(currencies)
        viewModel.adapterSelectableCurrencies = ArrayList(currencies)
        submitList(viewModel.adapterFilteredCurrencies)
    }

    override fun getSectionTitle(position: Int): String {
        return viewModel.adapterFilteredCurrencies[position]
            .currencyCode[Currency.CURRENCY_CODE_START_INDEX]
            .toString()
    }

    override fun getFilter() = currenciesFilter

    private val currenciesFilter: Filter = object : Filter() {
        @SuppressLint("DefaultLocale")
        override fun performFiltering(constraint: CharSequence?): FilterResults {
            val filterResults = FilterResults()
            filterResults.values = viewModel.filter(constraint)
            return filterResults
        }

        @Suppress("UNCHECKED_CAST")
        override fun publishResults(constraint: CharSequence, results: FilterResults) {
            val filteredCurrencies = results.values as List<Currency>
            viewModel.adapterFilteredCurrencies.run {
                clear()
                addAll(filteredCurrencies)
            }
            submitList(filteredCurrencies)
        }
    }
}