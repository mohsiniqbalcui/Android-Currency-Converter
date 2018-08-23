package com.nicoqueijo.android.currencyconverter.fragments;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.helper.ItemTouchHelper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.nicoqueijo.android.currencyconverter.R;
import com.nicoqueijo.android.currencyconverter.activities.MainActivity;
import com.nicoqueijo.android.currencyconverter.adapters.ActiveExchangeRatesRecyclerViewAdapter;
import com.nicoqueijo.android.currencyconverter.helpers.Constants;
import com.nicoqueijo.android.currencyconverter.helpers.SwipeAndDragHelper;
import com.nicoqueijo.android.currencyconverter.helpers.Utility;
import com.nicoqueijo.android.currencyconverter.models.Currency;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Map;

import static android.content.Context.MODE_PRIVATE;

/**
 * Fragment that allows the user to add/remove exchange rates and perform conversions.
 */
public class ActiveExchangeRatesFragment extends Fragment {

    public static final String TAG = ActiveExchangeRatesFragment.class.getSimpleName();

    private ArrayList<Currency> mAllCurrencies = new ArrayList<>();
    private ArrayList<Currency> mActiveCurrencies = new ArrayList<>();
    private SharedPreferences mSharedPreferencesRates;

    private RecyclerView mRecyclerView;
    private ActiveExchangeRatesRecyclerViewAdapter mAdapter;
    private RecyclerView.LayoutManager mLayoutManager;
    private FloatingActionButton mFloatingActionButton;
    private SwipeAndDragHelper swipeAndDragHelper;
    private ItemTouchHelper itemTouchHelper;

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        mSharedPreferencesRates = getContext().getSharedPreferences
                (MainActivity.sharedPrefsRatesFilename, MODE_PRIVATE);
        Map<String, ?> keys = mSharedPreferencesRates.getAll();
        for (Map.Entry<String, ?> entry : keys.entrySet()) {
            String currencyCode = entry.getKey();
            double exchangeRate = Utility.getDouble(mSharedPreferencesRates, entry.getKey(), 0.0);
            mAllCurrencies.add(new Currency(currencyCode, exchangeRate));
        }
        Collections.sort(mAllCurrencies, new Comparator<Currency>() {
            @Override
            public int compare(Currency currency1, Currency currency2) {
                return currency1.getCurrencyCode().compareTo(currency2.getCurrencyCode());
            }
        });
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putParcelableArrayList(Constants.ARG_ACTIVE_CURRENCIES, mActiveCurrencies);
        outState.putParcelableArrayList(Constants.ARG_ALL_CURRENCIES, mAllCurrencies);
        mAdapter.notifyDataSetChanged();
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (savedInstanceState != null) {
            mActiveCurrencies = savedInstanceState
                    .getParcelableArrayList(Constants.ARG_ACTIVE_CURRENCIES);
            mAllCurrencies = savedInstanceState
                    .getParcelableArrayList(Constants.ARG_ALL_CURRENCIES);
        } else {
            // I might be able to restore active currencies from shared prefs here
            // since this is only hit once upon app launch
            restoreActiveCurrenciesFromSharedPrefs();
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_active_exchange_rates, container, false);

        mRecyclerView = view.findViewById(R.id.recycler_view_active_rates);
        mFloatingActionButton = view.findViewById(R.id.fab);
        mLayoutManager = new LinearLayoutManager(getContext());
        mRecyclerView.setLayoutManager(mLayoutManager);
        mAdapter = new ActiveExchangeRatesRecyclerViewAdapter(getContext(), mActiveCurrencies);
        swipeAndDragHelper = new SwipeAndDragHelper(mAdapter);
        itemTouchHelper = new ItemTouchHelper(swipeAndDragHelper);
        mRecyclerView.setAdapter(mAdapter);
        itemTouchHelper.attachToRecyclerView(mRecyclerView);

        mFloatingActionButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                FragmentManager fragmentManager = getFragmentManager();
                FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
                if (fragmentManager.findFragmentByTag(SelectExchangeRatesDialog.TAG) == null) {
                    DialogFragment selectExchangeRateDialog =
                            SelectExchangeRatesDialog.newInstance(mAllCurrencies);
                    selectExchangeRateDialog.show(fragmentTransaction,
                            SelectExchangeRatesDialog.TAG);
                }
            }
        });
        return view;
    }

    @Override
    public void onStop() {
        super.onStop();
        saveActiveCurrenciesToSharedPrefs();
    }

    /**
     * Factory method to create a new instance of this fragment using the provided parameters.
     *
     * @return a new instance of fragment
     */
    public static ActiveExchangeRatesFragment newInstance() {
        ActiveExchangeRatesFragment activeExchangeRatesFragment = new ActiveExchangeRatesFragment();
        return activeExchangeRatesFragment;
    }

    /**
     * Adds the newly selected currency to the data set and notifies
     * the adapter so the changes can be reflected on the UI.
     *
     * @param currency the new currency that was selected
     */
    public void addActiveCurrency(Currency currency) {
        mActiveCurrencies.add(currency);
        for (int i = 0; i < mActiveCurrencies.size(); i++) {
            mAdapter.notifyItemChanged(i);
        }
    }

    /**
     * Saves the list of active currencies to shared prefs maintaining the order in which they
     * appear. Does this by first clearing what was already inside the shared prefs to avoid
     * conflicts.
     */
    private void saveActiveCurrenciesToSharedPrefs() {
        SharedPreferences sharedPreferences = getActivity().getSharedPreferences
                (getActivity().getPackageName().concat(".active_rates"), MODE_PRIVATE);
        SharedPreferences.Editor sharedPreferencesEditor = sharedPreferences.edit();
        sharedPreferencesEditor.clear();
        for (int i = 0; i < mActiveCurrencies.size(); i++) {
            Currency currency = mActiveCurrencies.get(i);
            sharedPreferencesEditor.putInt(currency.getCurrencyCode(), i);
        }
        sharedPreferencesEditor.apply();
    }

    private void restoreActiveCurrenciesFromSharedPrefs() {
        SharedPreferences sharedPreferences = getActivity().getSharedPreferences
                (getActivity().getPackageName().concat(".active_rates"), MODE_PRIVATE);
        Map<String, ?> keys = sharedPreferences.getAll();
        Currency[] savedActiveCurrencies = new Currency[keys.size()];
        for (Map.Entry<String, ?> entry : keys.entrySet()) {
            String currencyCode = entry.getKey();
            double exchangeRate = Utility.getDouble(mSharedPreferencesRates, entry.getKey(), 0.0);
            int order = sharedPreferences.getInt(entry.getKey(), 0);
            savedActiveCurrencies[order] = new Currency(currencyCode, exchangeRate);
        }
        mActiveCurrencies.addAll(Arrays.asList(savedActiveCurrencies));
    }
}
