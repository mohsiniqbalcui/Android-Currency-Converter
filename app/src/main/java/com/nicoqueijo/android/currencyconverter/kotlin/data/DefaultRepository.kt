package com.nicoqueijo.android.currencyconverter.kotlin.data

import android.content.Context
import com.nicoqueijo.android.currencyconverter.BuildConfig
import com.nicoqueijo.android.currencyconverter.R
import com.nicoqueijo.android.currencyconverter.kotlin.data.Repository.Companion.DEBUG
import com.nicoqueijo.android.currencyconverter.kotlin.data.Repository.Companion.RELEASE
import com.nicoqueijo.android.currencyconverter.kotlin.model.ApiEndPoint
import com.nicoqueijo.android.currencyconverter.kotlin.model.Currency
import com.nicoqueijo.android.currencyconverter.kotlin.model.ExchangeRates
import com.nicoqueijo.android.currencyconverter.kotlin.model.Resource
import com.nicoqueijo.android.currencyconverter.kotlin.util.Utils.isNetworkAvailable
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import retrofit2.Response
import java.net.SocketTimeoutException
import javax.inject.Inject

class DefaultRepository @Inject constructor(
        private val context: Context,
        private val exchangeRateService: ExchangeRateService,
        private val currencyDao: CurrencyDao,
        private val appPrefs: AppPrefs
) : Repository {

    override var isFirstLaunch = appPrefs.isFirstLaunch

    override val timestampInSeconds = appPrefs.timestampInSeconds

    override fun getAllCurrencies() = currencyDao.getAllCurrencies()

    override fun getSelectedCurrencies() = currencyDao.getSelectedCurrencies()

    override fun upsertCurrency(currency: Currency) {
        CoroutineScope(Dispatchers.IO).launch {
            currencyDao.upsertCurrency(currency)
        }
    }

    override fun upsertCurrencies(currencies: List<Currency>) {
        CoroutineScope(Dispatchers.IO).launch {
            currencyDao.upsertCurrencies(currencies)
        }
    }

    override suspend fun getCurrency(currencyCode: String) = currencyDao.getCurrency(currencyCode)

    /**
     * Makes an API call and persists the response if it's successful.
     */
    override suspend fun fetchCurrencies(): Resource {
        if (context.isNetworkAvailable() && (appPrefs.isDataEmpty || appPrefs.isDataStale)) {
            val retrofitResponse: Response<ApiEndPoint>
            try {
                retrofitResponse = exchangeRateService.getExchangeRates(getApiKey())
            } catch (e: SocketTimeoutException) {
                return Resource.Error(NETWORK_TIMEOUT_ERROR_MESSAGE)
            }
            return if (retrofitResponse.isSuccessful) {
                persistResponse(retrofitResponse)
                 Resource.Success
            } else {
                // Retrofit call executed but response wasn't in the 200s
                Resource.Error(retrofitResponse.errorBody()?.string())
            }
        } else if (!appPrefs.isDataEmpty) {
            return Resource.Success
        } else {
            return Resource.Error(NETWORK_OR_DATA_UNAVAILABLE_ERROR_MESSAGE)
        }
    }

    private suspend fun persistResponse(response: Response<ApiEndPoint>) {
        response.body()?.let { responseBody ->
            responseBody.exchangeRates?.let { exchangeRates ->
                persistCurrencies(exchangeRates)
            }
            appPrefs.timestampInSeconds = responseBody.timestamp
        }
    }

    private suspend fun persistCurrencies(exchangeRates: ExchangeRates) {
        when {
            appPrefs.isDataEmpty -> currencyDao.upsertCurrencies(exchangeRates.currencies)
            appPrefs.isDataStale -> currencyDao.updateExchangeRates(exchangeRates.currencies)
        }
    }

    private fun getApiKey(): String {
        with(context.resources) {
            return when (BuildConfig.BUILD_TYPE) {
                RELEASE -> getString(R.string.openexchangerates_release_api_key)
                DEBUG -> getString(R.string.openexchangerates_debug_api_key)
                else -> getString(R.string.openexchangerates_release_api_key)
            }
        }
    }

    companion object {
        const val NETWORK_OR_DATA_UNAVAILABLE_ERROR_MESSAGE = "Network is unavailable and no local data found."
        const val NETWORK_TIMEOUT_ERROR_MESSAGE = "Network request timed out."
    }
}