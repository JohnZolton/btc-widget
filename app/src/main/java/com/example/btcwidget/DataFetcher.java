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
import java.util.Iterator;

public class DataFetcher {
    // Bitcoin price API - No API key required
    private static final String BTC_API_URL = "https://min-api.cryptocompare.com/data/price?fsym=BTC&tsyms=USD";
    
    // Gold price API - Requires API key from https://metals.dev/
    private static final String GOLD_API_URL = "https://api.metals.dev/v1/latest?api_key=" + BuildConfig.METALS_DEV_KEY + "&currency=USD&unit_to_oz=true";
    
    // Gold price - Using static value since there's no reliable API or key is not provided
    private static final double GOLD_PRICE_PER_OZ = 2000.0; // Approximate gold price per oz
    
    // S&P 500 price API - Requires API key from https://www.alphavantage.co/
    private static final String SP500_API_URL = "https://www.alphavantage.co/query?function=TIME_SERIES_DAILY&symbol=SPY&apikey=" + BuildConfig.ALPHA_VANTAGE_KEY;
    
    // FRED API for median single-family home sale price (MSPUS series)
    // Using the FRED API key from BuildConfig
    private static final String FRED_API_URL = "https://api.stlouisfed.org/fred/series/observations?series_id=MSPUS&api_key=" + BuildConfig.FRED_KEY + "&file_type=json";
    
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
                    // Calculate gold comparison only if API key is provided and we got valid data
                    if (BuildConfig.METALS_DEV_KEY != null && !BuildConfig.METALS_DEV_KEY.isEmpty() && data.goldPrice > 0) {
                        data.btcToGold = data.btcPrice / data.goldPrice;
                    }
                    // Calculate S&P 500 comparison only if we got valid data
                    if (data.sp500Price > 0) {
                        data.btcToSP500 = data.btcPrice / data.sp500Price;
                    }
                    // Try to fetch a current median house price from FRED
                    double medianHouse = fetchMedianHousePrice();
                    // Only calculate btcToHouse if we got a valid median house price
                    if (medianHouse > 0) {
                        data.btcToHouse = data.btcPrice / medianHouse;
                    }
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
                DecimalFormat df = new DecimalFormat("#,###");
                DecimalFormat dfCompact = new DecimalFormat("0.00");
                
                // Update Bitcoin price
                views.setTextViewText(R.id.btc_price, "$" + df.format(data.btcPrice));
                
                // Update comparison values
                // Only show gold comparison if metals.dev API key is provided and we have valid data
                if (BuildConfig.METALS_DEV_KEY != null && !BuildConfig.METALS_DEV_KEY.isEmpty() && data.btcToGold > 0) {
                    views.setViewVisibility(R.id.btc_to_gold, View.VISIBLE);
                    views.setTextViewText(R.id.btc_to_gold, dfCompact.format(data.btcToGold) + " Gold oz");
                } else if (BuildConfig.METALS_DEV_KEY != null && !BuildConfig.METALS_DEV_KEY.isEmpty()) {
                    // Show error message for gold comparison if API key is provided but data is unavailable
                    views.setViewVisibility(R.id.btc_to_gold, View.VISIBLE);
                    views.setTextViewText(R.id.btc_to_gold, "Gold data unavailable");
                }
                
                // Only show S&P 500 comparison if we have valid data
                if (data.btcToSP500 > 0) {
                    views.setTextViewText(R.id.btc_to_sp500, dfCompact.format(data.btcToSP500) + " S&P 500 shares");
                } else {
                    views.setTextViewText(R.id.btc_to_sp500, "S&P 500 data unavailable");
                }
                // Only display btcToHouse if we have valid data
                if (data.btcToHouse > 0) {
                    views.setTextViewText(R.id.btc_to_house, dfCompact.format(data.btcToHouse) + " Median homes");
                } else {
                    views.setTextViewText(R.id.btc_to_house, "Median home data unavailable");
                }
            } else {
                // Handle error case
                views.setTextViewText(R.id.btc_price, "Data unavailable");
                // Only show gold comparison error if metas.dev API key is provided
                if (BuildConfig.METALS_DEV_KEY != null && !BuildConfig.METALS_DEV_KEY.isEmpty()) {
                    views.setViewVisibility(R.id.btc_to_gold, View.VISIBLE);
                    views.setTextViewText(R.id.btc_to_gold, "Gold data unavailable");
                }
                views.setTextViewText(R.id.btc_to_sp500, "S&P 500 data unavailable");
                views.setTextViewText(R.id.btc_to_house, "Median home data unavailable");
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
                    // Return -1 to indicate failure
                    return -1;
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
                
                // Parse the Alpha Vantage JSON response
                JSONObject jsonObject = new JSONObject(response.toString());
                JSONObject timeSeries = jsonObject.getJSONObject("Time Series (Daily)");
                
                // Get the most recent date (first key in the time series)
                Iterator<String> keys = timeSeries.keys();
                if (keys.hasNext()) {
                    String latestDate = keys.next();
                    JSONObject latestData = timeSeries.getJSONObject(latestDate);
                    String closePriceStr = latestData.getString("4. close");
                    return Double.parseDouble(closePriceStr);
                }
                
                return -1;
            } catch (Exception e) {
                e.printStackTrace();
                // Return -1 to indicate failure
                return -1;
            }
        }
        
        /**
         * Attempts to fetch the latest median house price (MSPUS series) from FRED.
         * Using the FRED API key from BuildConfig.
         * If the call fails or no numeric observation is found, this returns -1 to indicate failure.
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
                
                // No numeric observation found
                return -1;
            } catch (Exception e) {
                e.printStackTrace();
                // On any error (including 403/401 from missing API key), return -1
                return -1;
            }
        }
    }
    
    public static class FinancialData {
        public double btcPrice = 0;
        public double goldPrice = -1; // Initialize to -1 to indicate no data fetched yet
        public double sp500Price = -1; // Initialize to -1 to indicate no data fetched yet
        public double btcToGold = 0;
        public double btcToSP500 = 0;
        public double btcToHouse = 0;
        // The median house price used for the btc-to-house comparison (may be fetched from FRED)
        // Initialize to -1 to indicate no data fetched yet
        public double medianHousePrice = -1;
    }
    
    public interface DataFetchCallback {
        void onDataFetched(RemoteViews views);
    }
}
