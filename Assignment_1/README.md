# Assignment 1

Implementation of logic to process transactions and produce the ledger for ScroogeCoin. All transactions are validated before being published to as a final list. For the validation, the transaction handler selects a subset of transactions that are valid together and ensures that no output is spend twice (double-spending). Moreover, the signature, output values and input availability is checked.



## File Structure

- src
  - `Crypto.java` verify signatures
  - `Transaction.java` transaction object with nested input and output class
  - `TxHandler.java` main transaction processing and validation
  - `UTXO.java` Unspent transaction output
  - `UTXOPool.java` Collection of UTXOs
- test
  - `TxHandlerTest.java` Tests for transaction handling



## Tests

- `testValidTx()`
  - Two valid subsequent transactions with transaction handler
- `testInvalidSignature()`
  - Invalid signature from Bob for coins from Alice
- `testInsufficientInput()`
  - Sum of outputs higher than sum of inputs
- `testDoubleSpendingMultipleTransactions()`
  - Spending specific coins in two independent transactions
- `testDoubleSpendingSingleTransaction()`
  - Spending specific coins twice in single transaction
- `testUnknownClaimedOutput()`
  - Input coins from unknown output
- `testNegativeOutput()`
  - Transaction with negative output value