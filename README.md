# TradeBot
This is a cryptocurrency trading bot that uses the Binance API,
and a strategy based on a couple of 5 minute chart indicators
(RSI, MACD, Bollinger Bands)
(The bot only trades USDT fiat pairs)


Download [TradeBot.zip](https://github.com/markusaksli/TradeBot/raw/master/TradeBot.zip) if you just want to use the jar from command line. The zip comes included with a simple .bat script to run the bot. Keep in mind that this project was compiled with [JDK 11](https://www.oracle.com/java/technologies/javase-jdk11-downloads.html). This means you will need [JDK 11](https://www.oracle.com/java/technologies/javase-jdk11-downloads.html) or higher in your PATH to run it.
The config for the bot can be changed using the config.txt file


The bot has the following modes of operation:

## Live (CURRENTLY UNTESTED)

- This mode trades with real money on the Binance platform

- API key and Secret key required

## Simulation

- Real-time trading simulation based on actual market data

- Trades are only simulated based on market prices 

- No actual orders are made

## Backtesting

- Simulation based on historical data

- Allows for quick testing of the behavior and profitability of the bot

- Data needs to be loaded from a .dat file created with the COLLECTION mode

## Collection

- Collects raw market price data (aggregated trades) from a specified time period

- Collected data is saved in a file in the /backtesting directory

- Never run more than one TradeBot with this mode at the same time

# Issues, suggestions and contributing
If you run into any issues while using the bot or if you want to request any changes or new features, open a new issue to let us know.

If you would like to contribute to the development and profitability of the bot, simply open a PR or let us know.
