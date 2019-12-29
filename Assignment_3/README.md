# Assignment 3

Implementation of a single node that is part of a blockchain-based distributed consensus protocol. The node can receive incoming transactions and blocks and will maintain an updated blockchain itself. Further, the node does not record all blocks added to branches in memory to avoid overflow.

A block wrapper class helps to store additional information about a block: previous block, height, and UTXO pool. The main chain has a max height which returns the block of the current longest chain.

All transactions waiting to be included in the blockchain are recorded in a transaction pool.



## File Structure

- src
  - `Block.java` stores a block with its data structure
  - `BlockChain.java`
  - `BlockHandler.java` processes newly received blocks, creates new block, or processes newly received transaction
  - `Branch.java`
  - `ByteArrayWrapper.java` utility which serves as wrapper for byte arrays to be used as key in hash functions
  - `Transaction.java` transaction object with nested input and output class
  - `TransactionPool.java`
  - `UTXO.java` unspent transaction output
  - `UTXOPool.java` collection of UTXOs
  - `TxHandler.java` main transaction processing and validation
- test
  - `BlockChainTest.java` Tests for transaction handling



## Blockchain Implementation

- `public BlockChain(Block genesisBlock)`
  - Constructor
  - Initiates chain as hash map with block identified by hash
  - Adds coinbase output to UTXO pool
  - Ignores coinbase maturity period
  - Wraps genesis block 
  - Initiates transaction pool
- `public Block getMaxHeightBlock()`
  - Returns unwrapped block with max height
- `public UTXOPool getMaxHeightUTXOPool()`
  - Returns UTXO pool of max height block
- `public TransactionPool getTransactionPool()`
  - Returns pool of waiting transactions
- `public boolean addBlock(Block block)`
  - Includes checks
    - Another genesis block
    - Block without matching previous block
    - Block with invalid transaction
    - Branch height with cut off age
  - Add coinbase to UTXO pool
  - Updates UTXO pool by removing spent outputs and adding new outputs
  - Updates transaction pool; removes included transactions
  - Creates block wrapper and adds to chain
  - Updates main branch
- `public void addTransaction(Transaction tx) `
  - Adds new transaction to transaction pool



## Tests

Setup method creates an array list of keypairs that will be used throughout the tests as users to send and receive transactions. A genesis setup method creates the genesis block and initiates the blockchain.

- `testGenesisSetUp()`
- `testSecondBlockWithValidTransaction()`
- `testSecondBlockWithInvalidTransaction()`
  
- `testTransactionPoolAddingAndRemoving()`
- `testMultipleBlocksWithMultipleValidTransactions()` 
  
- `testBlockFalseHeightConstraint()`
- `testBlockWithUTXOInTransactionAlreadyClaimed()`
- `testAddingNewGenesisBlock()`
  
- `testBlockWithInvalidPreviousHash()`

