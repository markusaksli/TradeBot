# TradeBot
Crypto trading bot using xChange and Binance API

The bot uses a Binance API key to get live charts and history for the top 5 crypto coins on https://coinmarketcap.com/.

The bot uses 200 MA, 50 MA, RSI and MACD indicators to generate and filter buy signals. The bot automatically places buy limit orders on Bitmex with a minimum bid, take-profit, and trailing stop-loss. The bot constantly monitors all of its positions for close signals (based on indicators, take-profit or stop-loss).

The aim of this projext is to create a profitable live trading bot (with paper trading functionality).
