package com.example.btcwidget;

import android.content.Context;
import android.os.AsyncTask;
import android.view.View;
import android.widget.RemoteViews;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.DecimalFormat;

public class DataFetcher {
    // Bitcoin price API - No API key required
    private static final String BTC_API_URL = "https://min-api.cryptocompare.com/data/price?fsym=BTC&tsyms=USD";
    
    // Gold price API - Requires API key from https://metals.dev/
    private static final String GOLD_API_URL = "https://api.metals.dev/v1/latest?api_key=" + BuildConfig.METALS_DEV_KEY + "&currency=USD&unit_to_oz=true";
    
    // Gold price - Using static value since there's no reliable API or key is not provided
    private static final double GOLD_PRICE_PER_OZ = 2000.0; // Approximate gold price per oz
    
    // S&P 500 price API - Requires API key from https://financialmodelingprep.com/
    private static final String SP500_API_URL = "https://financialmodelingprep.com/api/v3/quote/%5EGSPC?apikey=" + BuildConfig.FINANCIAL_MODELING_PREP_KEY;
    
    // FRED API for median single-family home sale price (MSPUS series)
    // We'll try calling without an API key (empty api_key) and fall back to static value if it fails
    private static final String FRED_API_URL = "https://api.stlouisfed.org/fred/series/observations?series_id=MSPUS&api_key=&file_type=json";
    // Fallback static median house price (used if FRED fetch fails)
    private static final double MEDIAN_HOUSE_PRICE = 400000.0;
    
    private Context context;
    private int appWidgetId;
    private RemoteViews views;
    private DataFetchCallback callback;
    
    public DataFetcher(Context context, int appWidgetId, RemoteViews views, DataFetchCallback callback) {
        this.context = context;
        this.appWidgetId = appWidgetId;
        this.views = views;
        this.callback = callback;
    }
    
    public void fetchAllData() {
        new FetchDataTask().execute();
    }
    
    private class FetchDataTask extends AsyncTask<Void, Void, FinancialData> {
        @Override
        protected FinancialData doInBackground(Void... voids) {
            FinancialData data = new FinancialData();
            
            try {
                // Fetch Bitcoin price
                data.btcPrice = fetchBitcoinPrice();
                
                // Fetch gold price only if API key is provided
                if (BuildConfig.METALS_DEV_KEY != null && !BuildConfig.METALS_DEV_KEY.isEmpty()) {
                    data.goldPrice = fetchGoldPrice();
                }
                
                // Fetch S&P 500 price
                data.sp500Price = fetchSP500Price();
                
                // Calculate comparisons
                if (data.btcPrice > 0) {
                    if (BuildConfig.METALS_DEV_KEY != null && !BuildConfig.METALS_DEV_KEY.isEmpty()) {
                        data.btcToGold = data.btcPrice / data.goldPrice;
                    }
                    data.btcToSP500 = data.btcPrice / data.sp500Price;
                    // Try to fetch a current median house price from FRED; fallback to static value if that fails
                    double medianHouse = fetchMedianHousePrice();
                    data.btcToHouse = data.btcPrice / medianHouse;
                    data.medianHousePrice = medianHouse;
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            
            return data;
        }
        
        @Override
        protected void onPostExecute(FinancialData data) {
            if (data.btcPrice > 0) {
                DecimalFormat df = new DecimalFormat("#,###.00");
                DecimalFormat dfCompact = new DecimalFormat("0.00");
                
                // Update Bitcoin price
                views.setTextViewText(R.id.btc_price, "BTC: $" + df.format(data.btcPrice));
                
                // Update comparison values
                // Only show gold comparison if metals.dev API key is provided
                if (BuildConfig.METALS_DEV_KEY != null && !BuildConfig.METALS_DEV_KEY.isEmpty()) {
                    views.setViewVisibility(R.id.btc_to_gold, View.VISIBLE);
                    views.setTextViewText(R.id.btc_to_gold, "1 BTC = " + dfCompact.format(data.btcToGold) + " Gold oz");
                }
                views.setTextViewText(R.id.btc_to_sp500, "1 BTC = " + dfCompact.format(data.btcToSP500) + " S&P 500 shares");
                views.setTextViewText(R.id.btc_to_house, "1 BTC = " + dfCompact.format(data.btcToHouse) + " Median homes");
            } else {
                // Handle error case
                views.setTextViewText(R.id.btc_price, "BTC: Data unavailable");
                // Only show gold comparison error if metals.dev API key is provided
                if (BuildConfig.METALS_DEV_KEY != null && !BuildConfig.METALS_DEV_KEY.isEmpty()) {
                    views.setViewVisibility(R.id.btc_to_gold, View.VISIBLE);
                    views.setTextViewText(R.id.btc_to_gold, "1 BTC = Data unavailable");
                }
                views.setTextViewText(R.id.btc_to_sp500, "1 BTC = Data unavailable");
                views.setTextViewText(R.id.btc_to_house, "1 BTC = Data unavailable");
            }
            
            callback.onDataFetched(views);
        }
        
        private double fetchBitcoinPrice() {
            try {
                URL url = new URL(BTC_API_URL);
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("GET");
                
                BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                StringBuilder response = new StringBuilder();
                String line;
                
                while ((line = reader.readLine()) != null) {
                    response.append(line);
                }
                
                reader.close();
                connection.disconnect();
                
                JSONObject jsonObject = new JSONObject(response.toString());
                return jsonObject.getDouble("USD");
            } catch (Exception e) {
                e.printStackTrace();
                return -1;
            }
        }
        
        private double fetchGoldPrice() {
            // Check if metals.dev API key is provided
            if (BuildConfig.METALS_DEV_KEY != null && !BuildConfig.METALS_DEV_KEY.isEmpty()) {
                try {
                    URL url = new URL(GOLD_API_URL);
                    HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                    connection.setRequestMethod("GET");
                    
                    BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                    StringBuilder response = new StringBuilder();
                    String line;
                    
                    while ((line = reader.readLine()) != null) {
                        response.append(line);
                    }
                    
                    reader.close();
                    connection.disconnect();
                    
                    JSONObject jsonObject = new JSONObject(response.toString());
                    JSONObject metals = jsonObject.getJSONObject("metals");
                    
                    return metals.getDouble("gold");
                } catch (Exception e) {
                    e.printStackTrace();
                    // Return a default value if API fails
                    return GOLD_PRICE_PER_OZ;
                }
            } else {
                // Return static gold price if no API key is provided
                return GOLD_PRICE_PER_OZ;
            }
        }
        
        private double fetchSP500Price() {
            try {
                URL url = new URL(SP500_API_URL);
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("GET");
                
                BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                StringBuilder response = new StringBuilder();
                String line;
                
                while ((line = reader.readLine()) != null) {
                    response.append(line);
                }
                
                reader.close();
                connection.disconnect();
                
                // Parse the JSON array response
                String responseStr = response.toString();
                if (responseStr.startsWith("[")) {
                    responseStr = responseStr.substring(1, responseStr.length() - 1);
                }
                
                JSONObject jsonObject = new JSONObject(responseStr);
                return jsonObject.getDouble("price");
            } catch (Exception e) {
                e.printStackTrace();
                // Return a default value if API fails
                return 5000.0; // Approximate S&P 500 price
            }
        }
        
        /**
         * Attempts to fetch the latest median house price (MSPUS series) from FRED.
         * We deliberately call the FRED endpoint with an empty api_key parameter (as requested).
         * If the call fails or no numeric observation is found, this returns the fallback MEDIAN_HOUSE_PRICE.
         */
        private double fetchMedianHousePrice() {
            try {
                URL url = new URL(FRED_API_URL);
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("GET");
                
                BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                StringBuilder response = new StringBuilder();
                String line;
                
                while ((line = reader.readLine()) != null) {
                    response.append(line);
                }
                
                reader.close();
                connection.disconnect();
                
                JSONObject jsonObject = new JSONObject(response.toString());
                // observations is an array; search from the end for the most recent numeric value
                org.json.JSONArray observations = jsonObject.getJSONArray("observations");
                for (int i = observations.length() - 1; i >= 0; i--) {
                    JSONObject obs = observations.getJSONObject(i);
                    String valueStr = obs.optString("value");
                    if (valueStr != null && !valueStr.equals(".") && !valueStr.isEmpty()) {
                        try {
                            return Double.parseDouble(valueStr);
                        } catch (NumberFormatException nfe) {
                            // ignore and continue searching earlier entries
                        }
                    }
                }
                
                // No numeric observation found — return fallback
                return MEDIAN_HOUSE_PRICE;
            } catch (Exception e) {
                e.printStackTrace();
                // On any error (including 403/401 from missing API key), fall back to static value
                return MEDIAN_HOUSE_PRICE;
            }
        }
    }
    
    public static class FinancialData {
        public double btcPrice = 0;
        public double goldPrice = 2000; // Default gold price per oz
        public double sp500Price = 5000; // Default S&P 500 price
        public double btcToGold = 0;
        public double btcToSP500 = 0;
        public double btcToHouse = 0;
        // The median house price used for the btc-to-house comparison (may be fetched from FRED)
        public double medianHousePrice = MEDIAN_HOUSE_PRICE;
    }
    
    public interface DataFetchCallback {
        void onDataFetched(RemoteViews views);
    }
}
