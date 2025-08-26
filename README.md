# Bitcoin Price Comparison Widget

An Android widget that displays the current Bitcoin price compared to gold, S&P 500, and US median house price.

## Features

- Displays current Bitcoin price in USD
- Shows Bitcoin value in terms of gold ounces
- Shows Bitcoin value in terms of S&P 500 shares
- Shows Bitcoin value in terms of median US home prices
- Refresh button to update data manually
- Automatic updates every 30 minutes

## Installation

1. Clone or download this repository
2. Open the project in Android Studio
3. Generate the Gradle wrapper files:
   - In Android Studio, go to File > Sync Project with Gradle Files
   - This will automatically download and generate the gradle-wrapper.jar file
   - Alternatively, you can run in terminal: `gradle wrapper` (if you have Gradle installed)
4. Add API keys for the financial data services:
   - Create an account at [Metals Dev API](https://metals.dev/) and get an API key
   - Create an account at [Financial Modeling Prep](https://financialmodelingprep.com/) and get an API key
5. Replace `YOUR_API_KEY` in `DataFetcher.java` with your actual API keys
6. Build and install the app on your Android device:
   - In Android Studio: Build > Build Bundle(s) / APK(s) > Build APK(s)
   - Command line: `./gradlew assembleDebug` or `./gradlew installDebug`

## Project Structure

The project follows the standard Android project structure:

- `app/src/main/java/com/example/btcwidget/` - Java source files
- `app/src/main/res/` - Resource files
- `app/build.gradle` - App module build configuration
- `build.gradle` - Root project build configuration
- `settings.gradle` - Project settings
- `gradle.properties` - Project-wide Gradle settings

## Usage

1. After installing the app, long-press on your home screen
2. Select "Widgets" from the menu
3. Find "BTC Comparison Widget" and drag it to your home screen
4. The widget will automatically update every 30 minutes
5. Tap the "Refresh" button to manually update the data

## Data Sources

- Bitcoin price: [CoinDesk API](https://api.coindesk.com/v1/bpi/currentprice.json)
- Gold price: [Metals Dev API](https://api.metals.dev/)
- S&P 500 price: [Financial Modeling Prep API](https://financialmodelingprep.com/)
- Median US home price: Static value based on recent data (~$400,000)

## Example Display

```
Bitcoin Price Comparisons
BTC: $67,890.00
1 BTC = 0.26 Gold oz
1 BTC = 1.85 S&P 500 shares
1 BTC = 0.72 Median homes
[Refresh] Last updated: 14:30
```

## Requirements

- Android 5.0 (API level 21) or higher
- Internet connection for data fetching

## Troubleshooting Gradle Issues

If you encounter Gradle build errors like:

```
Unable to find method ''org.gradle.api.artifacts.Dependency org.gradle.api.artifacts.dsl.DependencyHandler.module(java.lang.Object)''
```

Try these solutions:

1. **Clear Gradle cache**:

   - Close Android Studio
   - Delete the .gradle folder in your project directory
   - Run `./gradlew clean` and then `./gradlew build` in the terminal

2. **Stop Gradle daemons**:

   - Run `./gradlew --stop` in the terminal
   - Restart Android Studio

3. **Check Android Studio and Gradle versions**:

   - In Android Studio, go to Help > About to check your Android Studio version
   - This project is configured for Android Studio Narwhal (2025.1.1) with AGP 8.5.0 and Gradle 8.5
   - For other Android Studio versions, you might need to adjust versions accordingly

4. **Compatible versions**:

   - For Android Studio Narwhal (2025.1.1), use AGP 8.5.0 with Gradle 8.5
   - For Android Studio Flamingo (2022.2.1) or newer, use AGP 8.0.0+ with Gradle 8.0+
   - For older Android Studio versions, you might need to adjust versions accordingly

5. **Invalidate caches**:
   - In Android Studio, go to File > Invalidate Caches and Restart

## Manual Gradle Wrapper Generation

If Android Studio fails to generate the gradle-wrapper.jar file:

1. **Download it manually**:

   - Download Gradle 8.5 distribution from: https://services.gradle.org/distributions/gradle-8.5-bin.zip
   - Extract the zip file
   - Copy gradle/wrapper/gradle-wrapper.jar from the extracted folder to your project's gradle/wrapper/ directory

2. **Alternative approach using Android Studio**:

   - In Android Studio, go to Build > Build Bundle(s) / APK(s) > Build APK(s)
   - This might trigger the Gradle wrapper generation without needing command line tools

3. **Check Gradle installation**:
   - If you have Gradle installed on your system, you can run:
   - `gradle wrapper` in your project directory to generate the wrapper files

## License

This project is licensed under the MIT License.
