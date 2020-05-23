# TradeBot
This is a cryptocurrency trading bot that uses the Binance API,
and a strategy based on a couple of 5 minute chart indicators
(RSI, MACD, Bollinger Bands)
(The bot only trades USDT fiat pairs)


The bot has the following modes of operation:

---LIVE (CURRENTLY UNTESTED)

-This mode trades with real money on the Binance platform

-API key and Secret key required

---SIMULATION

-Real-time trading simulation based on actual market data

-Trades are only simulated based on market prices 

-No actual orders are made

---BACKTESTING

-Simulation based on historical data

-Allows for quick testing of the behavior and profitability of the bot

-Data needs to be loaded from a .dat file created with the COLLECTION mode

---COLLECTION

-Collects raw market price data (aggregated trades) from a specified time period

-Collected data is saved in a file in the /backtesting directory

-Collection can be very RAM intensive


The config for the bot can be changed using the config.txt file


JAR:

https://drive.google.com/open?id=1Rnss483S2dZSLMFyP53jNEDxNRGuFvov

Backtesting data:

https://drive.google.com/open?id=1VkPEZ064EiDEvv9Da-Ba_xn48cCySceU
