package com.nicoqueijo.android.currencyconverter.kotlin.views

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.nicoqueijo.android.currencyconverter.R

class SelectableCurrenciesFragment_kt : Fragment() {

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_selectable_currency_kt, container, false)
    }

}
