// Block Chain should maintain only limited block nodes to satisfy the functions
// You should not have all the blocks added to the block chain in memory 
// as it would cause a memory overflow.

import java.util.ArrayList;
import java.util.HashMap;

public class BlockChain {
    public static int CUT_OFF_AGE = 10;

    private HashMap<byte[], BlockWrapper> chain;
    private BlockWrapper main;
    private TransactionPool transactionPool;

    /**
     * create an empty block chain with just a genesis block. Assume {@code genesisBlock} is a valid
     * block
     */
    public BlockChain(Block genesisBlock) {

        UTXOPool pool = new UTXOPool();
        chain = new HashMap<>();

        // Include coinbase in UTXO pool
        Transaction coinbase = genesisBlock.getCoinbase();
        for (int output = 0; output < coinbase.numOutputs(); output++) {
            Transaction.Output transactionOutput = coinbase.getOutput(output);
            pool.addUTXO( new UTXO(coinbase.getHash(), output), transactionOutput);
        }

        // Wrap genesis block, then add to chain
        BlockWrapper genesisBlockWrapped = new BlockWrapper(genesisBlock, null, pool);
        chain.put(genesisBlock.getHash(), genesisBlockWrapped);

        // Genesis block is end of main chain; empty transaction pool
        main = genesisBlockWrapped;
        transactionPool = new TransactionPool();

    }

    /** Get the maximum height block */
    public Block getMaxHeightBlock() {
        return main.getRawBlock();
    }

    /** Get the UTXOPool for mining a new block on top of max height block */
    public UTXOPool getMaxHeightUTXOPool() {
        return main.getPool();
    }

    /** Get the transaction pool to mine a new block */
    public TransactionPool getTransactionPool() {
        return transactionPool;
    }

    /**
     * Add {@code block} to the block chain if it is valid. For validity, all transactions should be
     * valid and block should be at {@code height > (maxHeight - CUT_OFF_AGE)}.
     * 
     * <p>
     * For example, you can try creating a new block over the genesis block (block height 2) if the
     * block chain height is {@code <=
     * CUT_OFF_AGE + 1}. As soon as {@code height > CUT_OFF_AGE + 1}, you cannot create a new block
     * at height 2.
     * 
     * @return true if block is successfully added
     */
    public boolean addBlock(Block block) {

        // Return false if genesis block
        if (block.getPrevBlockHash() == null) {
            System.out.println("Is genesis block");
            return false;
        }

        // Return false if no previous block found
        BlockWrapper previousBlockWrapped = chain.get(block.getPrevBlockHash());
        if (previousBlockWrapped == null) {
            System.out.println("No previous block");
            return false;
        }

        // Check if all contained transactions are valid
        TxHandler handler = new TxHandler(previousBlockWrapped.getPool());
        Transaction[] transactions = block.getTransactions().toArray(new Transaction[block.getTransactions().size()]);
        Transaction[] validTransactions = handler.handleTxs(transactions);

        // Return false if any transaction is invalid
        if (validTransactions.length != transactions.length) {
            System.out.println("Invalid transaction");
            return false;
        }

        // Check height; extend from longest
        if (!(previousBlockWrapped.getHeight() + 1 > main.getHeight() - CUT_OFF_AGE)) {
            System.out.println("Height");
            return false;
        }

        UTXOPool pool = handler.getUTXOPool();

        // Include coinbase in UTXO pool
        Transaction coinbase = block.getCoinbase();
        for (int output = 0; output < coinbase.numOutputs(); output++) {
            Transaction.Output transactionOutput = coinbase.getOutput(output);
            pool.addUTXO( new UTXO(coinbase.getHash(), output), transactionOutput);
        }

        // Update transactions
        for (int i = 0; i < transactions.length; i++) {

            Transaction tx = transactions[i];
            ArrayList<Transaction.Input> inputs = tx.getInputs();

            // Check which UTXOs have been spent, remove from UTXO pool
            for (int j = 0; j < tx.numInputs(); j++) {
                Transaction.Input input = inputs.get(j);
                pool.removeUTXO(new UTXO(input.prevTxHash, input.outputIndex));
            }

            // Add new outputs to UTXO pool
            for (int k = 0; k < tx.numOutputs(); k++) {
                pool.addUTXO(new UTXO(tx.getHash(), k), tx.getOutput(k));
            }

            // Check which Transactions have been processed, remove from transaction pool
            for (int m = 0; m < block.getTransactions().size(); m++) {
                transactionPool.removeTransaction(tx.getHash());
            }

        }

        // Create new block wrapper and add to chain
        BlockWrapper newBlockWrapped =  new BlockWrapper(block, previousBlockWrapped, pool);
        chain.put(block.getHash(), newBlockWrapped);

        // Update end of branch and return success
        if(newBlockWrapped.getHeight() > main.getHeight()) {
            main = newBlockWrapped;
        }

        return true;

    }

    /** Add a transaction to the transaction pool */
    public void addTransaction(Transaction tx) {
        transactionPool.addTransaction(tx);
    }

}