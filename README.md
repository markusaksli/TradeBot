# TradeBot

This is a cryptocurrency trading bot that uses the Binance API, and a strategy based on a couple of 5 minute chart
indicators

- (RSI, MACD, Bollinger Bands)
- (The bot currently only trades USDT fiat pairs)

[Download the latest release](https://github.com/markusaksli/TradeBot/releases/latest)

## How?

- The bot uses 5 different indicators: DBB, EMA, MACD, RSI, SMA. Each indicator will fire off a buy signal when a
  certain state has been achieved.
- When the bot has collected enough signals, an order will be placed on the market.
- Vice versa, if enough sell signals are signals are fired, a sell order will be placed.

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

## Config

- MACD change indicator - Change of MACD line to count as a buy signal (decimal)
- RSI positive side minimum - Strong buy signal (2) if RSI is below this (integer)
- RSI positive side maximum - Buy signal if RSI is below this (integer)
- RSI negative side minimum - Sell signal if RSI is above this (integer)
- RSI negative side maximum - Strong sell signal (2) if RSI is above this (integer)
- Simulation mode starting value - Amount of USDT to start with in Simulation (integer)
- Percentage of money per trade - How much of available fiat to put into each trade (decimal)
- Trailing SL - Trailing Stop Loss (decimal)
- Take profit - Profit to close trade at (decimal)
- Confluence - How many indicators have to give a buy signal to buy (integer)
- Close confluence - How many indicators have to give a sell signal to sell (integer)
- Use confluence to close - Whether to use sell signals to close or not (true/false)
- Currencies to track - What currencies to track in simulation and live (ex BTC, ETH, ADA...)

See the included config file for a ready to use example

# Issues, suggestions and contributing

If you run into any issues while using the bot or if you want to request any changes or new features, open a new issue
to let us know.

If you would like to contribute to the development and profitability of the bot, simply open a PR or let us know.
