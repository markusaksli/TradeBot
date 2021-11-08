TradeBot is a cryptocurrency trading bot that uses the Binance API, and a strategy based on a couple of 5 minute chart
indicators

- (RSI, MACD, Bollinger Bands)

Download the latest release at https://github.com/markusaksli/TradeBot/releases/latest

### How?

- The bot uses 5 different indicators: DBB, EMA, MACD, RSI, SMA. The three main indicators will fire off a buy signal
  when a certain state has been achieved.
- When the bot has collected enough signals, an order will be placed on the market.
- Vice versa, if enough sell signals are signals are fired, a sell order will be placed.

The config for the bot can be changed using the config.txt file

# Modes

### Live

- **This is not a financial service or investment advice!**

  - **The default config and strategy implemented in the source code of the project serve as an example and are
    open-source.**

  - **While we intend to contribute to make the bot work well out-of-the-box, we make no specific claims about the
    profitability of it in the current market climate!**

- **Backtest your config and make sure you are ready to use this mode at your own risk!**

- This mode trades with real money on the Binance platform

- API key and Secret key required

- You can choose to add your credentials to the credentials.txt file for easier use

- Currently only supports market orders, this will cause a slight efficiency loss.

### Simulation

- Real-time trading simulation based on actual market data

- Trades are only simulated based on market prices

- No actual orders are made

### Backtesting

- Simulation based on historical data

- Allows for quick testing of the behavior and profitability of the bot

- Data needs to be loaded from a .dat file created with the Collection mode

### Collection

- Collects raw market price data (aggregated trades) from a specified time period

- Collected data is saved in a file in the backtesting directory

- Collected data can be exported to a .csv format

- Never run more than one TradeBot with this mode at the same time, you will likely hit the API request limit.

# Config

- MACD change indicator - Change of MACD line to count as a buy signal (decimal)
- RSI positive side minimum - Strong buy signal (2) if RSI is below this (integer)
- RSI positive side maximum - Buy signal if RSI is below this (integer)
- RSI negative side minimum - Sell signal if RSI is above this (integer)
- RSI negative side maximum - Strong sell signal (2) if RSI is above this (integer)
- Simulation mode starting value - Amount of FIAT to start with in Simulation (integer)
- Percentage of money per trade - How much of available fiat to put into each trade (decimal)
- Trailing SL - Trailing Stop Loss (decimal)
- Take profit - Profit to close trade at (decimal)
- Confluence - How many indicators have to give a buy signal to buy (integer)
- Close confluence - How many indicators have to give a sell signal to sell (integer)
- Use confluence to close - Whether to use sell signals to close or not (true/false)
- Currencies to track - What currencies to track in simulation and live (ex BTC, ETH, ADA...)
- FIAT - What currency to trade against (ex USDT)

See the included config file for a ready to use example

Setup the credentials.txt file to automatically log into live mode without having to enter your credentials