package com.tzgames.ringer.data;

import android.app.Activity;

import com.android.billingclient.api.AcknowledgePurchaseParams;
import com.android.billingclient.api.AcknowledgePurchaseResponseListener;
import com.google.android.material.snackbar.Snackbar;

import com.android.billingclient.api.BillingClient;
import com.android.billingclient.api.BillingClientStateListener;
import com.android.billingclient.api.BillingFlowParams;
import com.android.billingclient.api.BillingResult;
import com.android.billingclient.api.Purchase;
import com.android.billingclient.api.PurchasesUpdatedListener;
import com.android.billingclient.api.SkuDetails;
import com.android.billingclient.api.SkuDetailsParams;
import com.android.billingclient.api.SkuDetailsResponseListener;
import com.tzgames.ringer.activities.MainActivity;

import java.util.ArrayList;
import java.util.List;

/**
 * Class in charge of handling Google Play actions such as querying whether the user purchased
 * premium or starting the purchasing procedure. Main method: isPremium() to check whether the
 * user is premium.
 */
// TODO: Replace all strings in BillingManager with strings in strings.xml
public class BillingManager implements PurchasesUpdatedListener {

    /** Private BillingClient that connects to Google */
    private BillingClient billingClient;

    /** Activity that owns this BillingManager */
    private MainActivity activity;

    /** Flag to keep track of whether user is premium (premium=true/false) and whether to query
     *  if he is premium (premium=null) */
    private Boolean premium = null;

    /** Unique SKU set in Google Play Developer Console that represents the premium IAP */
    private static final String PREMIUM_IAP_SKU = "premium";

    /**
     * Call at the beginning of app load to set isPremium flag. Starts connection to Google
     * @param act Calling Activity
     */
    public void connectToGoogle(final Activity act) {
        activity = (MainActivity) act;
        billingClient = BillingClient.newBuilder(act).setListener(this).enablePendingPurchases().build();
        billingClient.startConnection(new BillingClientStateListener() {
            @Override
            public void onBillingSetupFinished(BillingResult billingResult) {
                if (billingResult.getResponseCode() == BillingClient.BillingResponseCode.OK) {
                    // The BillingClient is ready. Can query whether user is premium or not
                    isPremium();
                }
            }
            @Override
            public void onBillingServiceDisconnected() {
                /* TODO: Try to restart the connection on the next request to Google Play by
                    calling the startConnection() method. */
                activity.showSnackbar("Disconnected from GooglePlay. Restart app!",
                                Snackbar.LENGTH_SHORT);
            }
        });
    }

    /**
     * Method called when user finishes going through Buying flow or cancels or an error occurs
     */
    @Override
    public void onPurchasesUpdated(BillingResult billingResult, List<Purchase> purchases) {
        if (billingResult.getResponseCode() == BillingClient.BillingResponseCode.OK
                && purchases != null) {
            for (Purchase purchase : purchases) {
                if (purchase.getSku().equals(PREMIUM_IAP_SKU)) {
                    premium = null;
                    isPremium();
                }
            }
        } else if (billingResult.getResponseCode() == BillingClient.BillingResponseCode.USER_CANCELED) {
            // Handle an error caused by a user cancelling the purchase flow.
            activity.showSnackbar("Cancelled Purchase", Snackbar.LENGTH_SHORT);
        } else {
            // Handle any other error codes.
            activity.showSnackbar("Error: " + billingResult.getDebugMessage(), Snackbar.LENGTH_LONG);
        }
    }

    /**
     * Checks if the user is a premium user. If the premium flag hasn't been set yet, then query
     * it from Google Play.
     * @return True if user is premium. False otherwise.
     */
    public boolean isPremium() {
        // Premium has been set already
        if (premium != null) return premium;

        // Premium has not been set, so query it
        Purchase.PurchasesResult results = billingClient.queryPurchases(BillingClient.SkuType.INAPP);
        List<Purchase> purchases = results.getPurchasesList();
        if (purchases == null || purchases.size() == 0) return false;
        for (Purchase item : purchases) {
            if (item.getSku().equals(PREMIUM_IAP_SKU)) {
                premium = true;
                if (!item.isAcknowledged()){
                    AcknowledgePurchaseParams acknowledgePurchaseParams =
                            AcknowledgePurchaseParams.newBuilder()
                                    .setPurchaseToken(item.getPurchaseToken())
                                    .build();
                    billingClient.acknowledgePurchase(acknowledgePurchaseParams,
                            new AcknowledgePurchaseResponseListener() {
                        @Override
                        public void onAcknowledgePurchaseResponse(BillingResult billingResult) {
                            if (billingResult.getResponseCode() != BillingClient.BillingResponseCode.OK) {
                                activity.showSnackbar("Error: " + billingResult.getDebugMessage(),
                                        Snackbar.LENGTH_LONG);
                            }
                            else {
                                activity.showSnackbar("Hello, Premium User!", Snackbar.LENGTH_LONG);
                            }
                        }
                    });
                }
                return true;
            }
        }
        premium = false;
        return false;
    }

    /**
     * Starts the flow of buying premium IAP from Google Play.
     */
    public void buyPremium() {
        List<String> skuList = new ArrayList<>();
        skuList.add(PREMIUM_IAP_SKU);
        SkuDetailsParams.Builder params = SkuDetailsParams.newBuilder();
        params.setSkusList(skuList).setType(BillingClient.SkuType.INAPP);
        billingClient.querySkuDetailsAsync(params.build(),
            new SkuDetailsResponseListener() {
                @Override
                public void onSkuDetailsResponse(BillingResult billingResult,
                                                 List<SkuDetails> skuDetailsList) {
                    // Process the result
                    if (skuDetailsList == null) {
                        activity.showSnackbar("Fatal: Could not communicate with Google Play!",
                                        Snackbar.LENGTH_SHORT);
                        return;
                    }
                    if (skuDetailsList.size() == 1) {
                        BillingFlowParams flowParams = BillingFlowParams.newBuilder()
                                .setSkuDetails(skuDetailsList.get(0))
                                .build();
                        // launch flow for user to buy premium
                        billingClient.launchBillingFlow(activity, flowParams);
                    }
                    else {
                        activity.showSnackbar("Something went wrong!", Snackbar.LENGTH_SHORT);
                    }
                }
            });
    }
}
