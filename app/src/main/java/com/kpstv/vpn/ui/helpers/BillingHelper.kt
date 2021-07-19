package com.kpstv.vpn.ui.helpers

import android.content.Context
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStoreFile
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import com.android.billingclient.api.*
import com.kpstv.vpn.R
import es.dmoral.toasty.Toasty
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*

data class BillingSku(val sku: String) {
  companion object {
    fun createEmpty() = BillingSku(sku = "")
  }
}

class BillingHelper(private val activity: ComponentActivity) {

  private val dataStoreHelper = BillingDataStoreHelper(activity)

  val isPurchased: Flow<Boolean> = dataStoreHelper.watcher
  private val purchaseCompleteStateFlow: MutableStateFlow<BillingSku> = MutableStateFlow(BillingSku.createEmpty())
  val purchaseComplete: StateFlow<BillingSku> = purchaseCompleteStateFlow.asStateFlow()

  companion object {
    const val purchase_sku = "gear_premium"
  }

  private var sku: SkuDetails? = null

  private val purchasesUpdatedListener = PurchasesUpdatedListener { billingResult, purchases ->
    if (billingResult.responseCode == BillingClient.BillingResponseCode.OK && purchases != null) {
      purchases.forEach {
        activity.lifecycleScope.launchWhenStarted { handlePurchase(it) }
      }
    }
  }

  private var billingClient = BillingClient.newBuilder(activity)
    .setListener(purchasesUpdatedListener)
    .enablePendingPurchases()
    .build()

  private val activityObserver = object: DefaultLifecycleObserver {
    override fun onDestroy(owner: LifecycleOwner) {
      dataStoreHelper.cancel()
      activity.lifecycle.removeObserver(this)
    }
  }

  fun init() {
    activity.lifecycle.addObserver(activityObserver)
    billingClient.startConnection(object : BillingClientStateListener {
      override fun onBillingSetupFinished(billingResult: BillingResult) {
        if (billingResult.responseCode ==  BillingClient.BillingResponseCode.OK) {
          activity.lifecycleScope.launchWhenStarted {
            querySkuDetails()
          }
        } else {
          Log.e("BillingHelper", "Invalid Response code: ${billingResult.responseCode}, Message: ${billingResult.debugMessage}")
        }
      }
      override fun onBillingServiceDisconnected() {
        Log.e("BillingHelper", "Service disconnected")
      }
    })
  }

  fun launch() {
    val sku = sku ?: run {
      Toasty.error(activity, activity.getString(R.string.purchase_err_client)).show()
      return
    }

    val flowParams = BillingFlowParams.newBuilder()
      .setSkuDetails(sku)
      .build()

    billingClient.launchBillingFlow(activity, flowParams).responseCode
  }

  private suspend fun querySkuDetails() {
    val params = SkuDetailsParams.newBuilder()
    params.setSkusList(listOf(purchase_sku)).setType(BillingClient.SkuType.INAPP)

    val skuDetailsResult = withContext(Dispatchers.IO) {
      billingClient.querySkuDetails(params.build())
    }

    skuDetailsResult.skuDetailsList?.let { list ->
      sku = list.find { it.sku == purchase_sku }
    }
  }

  private suspend fun handlePurchase(purchase: Purchase) {
    if (purchase.purchaseState == Purchase.PurchaseState.PURCHASED) {
      if (!purchase.isAcknowledged) {
        val acknowledgePurchaseParams = AcknowledgePurchaseParams.newBuilder()
          .setPurchaseToken(purchase.purchaseToken)
        val ackPurchaseResult = withContext(Dispatchers.IO) {
          billingClient.acknowledgePurchase(acknowledgePurchaseParams.build())
        }
        if (ackPurchaseResult.responseCode != BillingClient.BillingResponseCode.OK) {
          Toasty.error(activity, activity.getString(R.string.purchase_ack)).show()
          return
        }
      }

      if (purchase.skus.contains(purchase_sku)) {
        dataStoreHelper.update(true)
        purchaseCompleteStateFlow.emit(BillingSku(purchase_sku))
      }
    }
  }

  internal class BillingDataStoreHelper(context: Context) {
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val dataStore: DataStore<Preferences> = PreferenceDataStoreFactory.create(
      produceFile = { context.preferencesDataStoreFile(BILLING_PB) },
      scope = scope
    )

    val watcher: Flow<Boolean> = dataStore.data.map { preferences ->
      preferences[purchaseKey] ?: return@map false
    }

    suspend fun clear() = dataStore.edit { it.clear() }

    suspend fun update(value: Boolean) {
      dataStore.edit { prefs ->
        prefs[purchaseKey] = value
      }
    }

    fun cancel() = scope.cancel()

    private val purchaseKey = booleanPreferencesKey(HAS_PURCHASED)

    companion object {
      private const val HAS_PURCHASED = "has_purchased"
      private const val BILLING_PB = "billing"
    }
  }
}