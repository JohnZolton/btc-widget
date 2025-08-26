package com.example.btcwidget;

import android.content.Context;
import android.os.AsyncTask;
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
    private static final String GOLD_API_URL = "https://api.metals.dev/v1/latest?api_key=YOUR_API_KEY&currency=USD&unit_to_oz=true";
    
    // S&P 500 price API - Requires API key from https://financialmodelingprep.com/
    private static final String SP500_API_URL = "https://financialmodelingprep.com/api/v3/quote/%5EGSPC?apikey=YOUR_API_KEY";
    
    // For house price data, we'll use a static value as there's no reliable real-time API
    // Current median house price in the US is approximately $400,000
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
                
                // Fetch gold price
                data.goldPrice = fetchGoldPrice();
                
                // Fetch S&P 500 price
                data.sp500Price = fetchSP500Price();
                
                // Calculate comparisons
                if (data.btcPrice > 0) {
                    data.btcToGold = data.btcPrice / data.goldPrice;
                    data.btcToSP500 = data.btcPrice / data.sp500Price;
                    data.btcToHouse = data.btcPrice / MEDIAN_HOUSE_PRICE;
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
                views.setTextViewText(R.id.btc_to_gold, "1 BTC = " + dfCompact.format(data.btcToGold) + " Gold oz");
                views.setTextViewText(R.id.btc_to_sp500, "1 BTC = " + dfCompact.format(data.btcToSP500) + " S&P 500 shares");
                views.setTextViewText(R.id.btc_to_house, "1 BTC = " + dfCompact.format(data.btcToHouse) + " Median homes");
            } else {
                // Handle error case
                views.setTextViewText(R.id.btc_price, "BTC: Data unavailable");
                views.setTextViewText(R.id.btc_to_gold, "1 BTC = Data unavailable");
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
                return 2000.0; // Approximate gold price per oz
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
    }
    
    public static class FinancialData {
        public double btcPrice = 0;
        public double goldPrice = 2000; // Default gold price per oz
        public double sp500Price = 5000; // Default S&P 500 price
        public double btcToGold = 0;
        public double btcToSP500 = 0;
        public double btcToHouse = 0;
    }
    
    public interface DataFetchCallback {
        void onDataFetched(RemoteViews views);
    }
}
