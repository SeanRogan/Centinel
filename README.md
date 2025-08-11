# Centinel
Centinel is an automated financial analysis and trading system. 

The project is created with Java/SpringBoot and uses a microservices architecture pattern.

### The Monitor Service:
#### This service connects to crypto/fin exchange APIs and streams current asset data to a timescale DB
### The Analysis Service:
#### This service analyzed the data loaded by the monitor service into the timescale DB and looks for trade opportunities.
### The Execution Service:
#### This service recieves trade signals from the Analysis service and executes the trades on various exchanges.