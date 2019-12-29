import java.util.ArrayList;

public class TxHandler {

    private UTXOPool pool;

    /**
     * Creates a public ledger whose current UTXOPool (collection of unspent transaction outputs) is
     * {@code utxoPool}. This should make a copy of utxoPool by using the UTXOPool(UTXOPool uPool)
     * constructor.
     */
    public TxHandler(UTXOPool utxoPool) {
        this.pool = new UTXOPool(utxoPool);
    }

    /**
     * @return true if:
     * (1) all outputs claimed by {@code tx} are in the current UTXO pool, 
     * (2) the signatures on each input of {@code tx} are valid, 
     * (3) no UTXO is claimed multiple times by {@code tx},
     * (4) all of {@code tx}s output values are non-negative, and
     * (5) the sum of {@code tx}s input values is greater than or equal to the sum of its output
     *     values; and false otherwise.
     */
    public boolean isValidTx(Transaction tx) {

        UTXOPool currentTransactionPool = new UTXOPool();
        double transactionFee = 0.0;

        // Check inputs
        for (int i = 0; i < tx.numInputs(); i++) {
            Transaction.Input currentTxInput = tx.getInput(i);
            assert currentTxInput != null;

            // (1) all outputs claimed by {@code tx} are in the current UTXO pool
            UTXO currentUTXO = new UTXO(currentTxInput.prevTxHash, currentTxInput.outputIndex);
            if (!this.pool.contains(currentUTXO)) {
                System.out.println("Claimed output by transaction is not in current UTXO pool");
                return false;
            }

            // (2) the signatures on each input of {@code tx} are valid
            Transaction.Output origin = this.pool.getTxOutput(currentUTXO);
            assert origin != null;

            if (!Crypto.verifySignature(origin.address, tx.getRawDataToSign(i), currentTxInput.signature)) {
                System.out.println("Signature is invalid");
                return false;
            }

            // (3) no UTXO is claimed multiple times by {@code tx}
            if (currentTransactionPool.contains(currentUTXO)) {
                System.out.println("UTXO is claimed multiple times");
                return false;
            } else {
                currentTransactionPool.addUTXO(currentUTXO, null);
            }

            transactionFee += origin.value;

        }

        // Check outputs
        for (int j = 0; j < tx.numOutputs(); j++) {

            // (4) all of {@code tx}s output values are non-negative
            Transaction.Output currentTxInput = tx.getOutput(j);
            if (currentTxInput.value < 0) {
                System.out.println("Negative output value");
                return false;
            }

            transactionFee -= tx.getOutput(j).value;

        }

        // (5) the sum of {@code tx}s input values is greater than or equal to the sum of its output
        // values; and false otherwise.
        if (transactionFee < 0) {
            System.out.println("Outputs higher than inputs");
            return false;
        }

        return true;
    }

    /**
     * Handles each epoch by receiving an unordered array of proposed transactions, checking each
     * transaction for correctness, returning a mutually valid array of accepted transactions, and
     * updating the current UTXO pool as appropriate.
     */
    public Transaction[] handleTxs(Transaction[] possibleTxs) {
        assert possibleTxs != null;
        ArrayList<Transaction> validTxs = new ArrayList<>();

        for (int x = 0; x < possibleTxs.length; x++) {
            Transaction currentTx = possibleTxs[x];

            // Check each transaction
            if (!isValidTx(currentTx)) {
                System.out.println("Transaction not valid");
                continue;
            }

            // Add to accepted transaction
            validTxs.add(currentTx);

            // Update UTXO pool: Remove used UTXOs
            for (int i = 0; i < currentTx.getInputs().size(); i++) {
                Transaction.Input currentTxInput = currentTx.getInput(i);
                UTXO oldUTXO = new UTXO(currentTxInput.prevTxHash, currentTxInput.outputIndex);

                this.pool.removeUTXO(oldUTXO);
            }

            // Update UTXO pool: Add new UTXOs
            for(int o = 0; o < currentTx.getOutputs().size(); o++) {
                Transaction.Output currentTxOutput = currentTx.getOutput(o);
                UTXO newUTXO = new UTXO(currentTx.getHash(), o);

                this.pool.addUTXO(newUTXO, currentTxOutput);
            }

        }

        // Return array of accepted transactions
        Transaction[] finalTxs = new Transaction[validTxs.size()];
        return validTxs.toArray(finalTxs);

    }

    public UTXOPool getUTXOPool() {
        return pool;
    }

}
