import org.junit.jupiter.api.Test;

import java.security.*;
import java.util.ArrayList;

import static org.junit.jupiter.api.Assertions.*;

class BlockChainTest {

    private ArrayList<KeyPair> users;
    private Block genesis;
    private BlockChain chain;
    private BlockHandler handler;

    private byte[] sign(KeyPair keys, byte[] message)
            throws NoSuchAlgorithmException, SignatureException, InvalidKeyException {

        Signature signature = Signature.getInstance("SHA256withRSA");
        signature.initSign(keys.getPrivate());
        signature.update(message);

        return signature.sign();
    }

    protected void setUp() throws Exception {

        // Generate key pairs
        KeyPairGenerator keyGen = KeyPairGenerator.getInstance("RSA");

        // Create keys for 50 users
        users = new ArrayList<KeyPair>();
        for (int i = 0; i < 50; i++) {
            KeyPair pair = keyGen.genKeyPair();
            users.add(pair);
        }

    }

    protected void genesisSetUp() throws Exception {
        this.setUp();

        // Create genesis block and init chain and handler
        genesis = new Block(null, users.get(0).getPublic());
        genesis.finalize();
        chain = new BlockChain(genesis);
        handler = new BlockHandler(chain);

        System.out.println("Genesis setup completed");

    }

    @Test
    void testGenesisSetUp() throws Exception {
        this.genesisSetUp();

        // Check previous hash and coinbase
        Block currentMaxHeightBlock = chain.getMaxHeightBlock();
        Transaction coinbase = new Transaction(25, users.get(0).getPublic());

        assertEquals(null, currentMaxHeightBlock.getPrevBlockHash());
        assertEquals(coinbase, currentMaxHeightBlock.getCoinbase());

        // Check UTXO outputs and whether it includes coinbase
        UTXOPool currentMaxHeightUTXOPool = this.chain.getMaxHeightUTXOPool();

        assertEquals(1, currentMaxHeightUTXOPool.getAllUTXO().size());
        assertEquals(new UTXO(coinbase.getHash(), 0), chain.getMaxHeightUTXOPool().getAllUTXO().get(0));

    }

    @Test
    public void testSecondBlockWithValidTransaction() throws Exception {
        this.genesisSetUp();

        // Create second block
        Block second = new Block(genesis.getHash(), users.get(1).getPublic());

        // Initiate transaction with two outputs
        Transaction coinbase = new Transaction();
        coinbase.addInput(genesis.getCoinbase().getHash(), 0);
        coinbase.addOutput(2, users.get(3).getPublic());
        coinbase.addOutput(5, users.get(4).getPublic());
        coinbase.addSignature(sign(users.get(0), coinbase.getRawDataToSign(0)), 0);
        coinbase.finalize();

        // Include transactions in block
        second.addTransaction(coinbase);
        second.finalize();

        assertTrue(chain.addBlock(second));

        // Process the block
        handler.processBlock(second);

        // Check block of main chain: hash and coinbase
        assertEquals(second.getHash(), chain.getMaxHeightBlock().getHash());
        assertEquals(new Transaction(25, users.get(1).getPublic()), chain.getMaxHeightBlock().getCoinbase());

    }

    @Test
    public void testSecondBlockWithInvalidTransaction() throws Exception {
        this.genesisSetUp();

        // Create second block
        Block second = new Block(genesis.getHash(), users.get(1).getPublic());

        // Initiate invalid transaction with false signature
        Transaction coinbase = new Transaction();
        coinbase.addInput(genesis.getCoinbase().getHash(), 0);
        coinbase.addOutput(2, users.get(5).getPublic());
        coinbase.addSignature(sign(users.get(5), coinbase.getRawDataToSign(0)), 0);
        coinbase.finalize();

        // Include transactions in block
        second.addTransaction(coinbase);
        second.finalize();

        // Should return false due to invalid transaction
        assertFalse(chain.addBlock(second));

    }

    @Test
    public void testTransactionPoolAddingAndRemoving() throws Exception {
        this.genesisSetUp();

        // Create second block
        Block second = new Block(genesis.getHash(), users.get(1).getPublic());

        // Initiate transaction a
        Transaction a = new Transaction();
        a.addInput(genesis.getCoinbase().getHash(), 0);
        a.addOutput(10, users.get(10).getPublic());
        a.addOutput(5, users.get(11).getPublic());
        a.addSignature(sign(users.get(0), a.getRawDataToSign(0)), 0);
        a.finalize();

        // Initiate transaction b
        Transaction b = new Transaction();
        b.addInput(genesis.getCoinbase().getHash(), 0);
        b.addOutput(1, users.get(12).getPublic());
        b.addSignature(sign(users.get(10), b.getRawDataToSign(0)), 0);
        b.finalize();

        // Add transactions a and b to chain
        chain.addTransaction(a);
        chain.addTransaction(b);

        ArrayList<Transaction> expTransactionsAll = new ArrayList<>();
        expTransactionsAll.add(a);
        expTransactionsAll.add(b);

        // Ensure that transactions a and b are in transaction pool
        assertEquals(expTransactionsAll.size(), chain.getTransactionPool().getTransactions().size());

        // Add transaction a to block
        second.addTransaction(a);
        second.finalize();
        assertTrue(chain.addBlock(second));

        ArrayList<Transaction> expTransactions = new ArrayList<>();
        expTransactions.add(b);

        // Ensure that transaction b remains in transaction pool
        // Remark: transaction b depends on a
        assertEquals(expTransactions, chain.getTransactionPool().getTransactions());

    }

    @Test
    void testMultipleBlocksWithMultipleValidTransactions() throws Exception {
        this.genesisSetUp();

        // Create second block
        Block second = new Block(genesis.getHash(), users.get(1).getPublic());

        // Initiate first transaction and include in block
        Transaction a = new Transaction();
        a.addInput(genesis.getCoinbase().getHash(), 0);
        a.addOutput(2, users.get(6).getPublic());
        a.addOutput(4, users.get(7).getPublic());
        a.addSignature(sign(users.get(0), a.getRawDataToSign(0)), 0);
        a.finalize();
        second.addTransaction(a);

        // Initiate second transaction and include in block
        Transaction b = new Transaction();
        b.addInput(a.getHash(), 0);
        b.addOutput(0.5, users.get(8).getPublic());
        b.addSignature(sign(users.get(6), b.getRawDataToSign(0)), 0);
        b.finalize();
        second.addTransaction(b);

        // Finalize second block and process
        second.finalize();
        assertTrue(chain.addBlock(second));
        handler.processBlock(second);

        // Create third block
        Block third = new Block(second.getHash(), users.get(2).getPublic());

        // Initiate fourth transaction and include in block
        Transaction c = new Transaction();
        c.addInput(b.getHash(), 0);
        c.addOutput(0.1, users.get(9).getPublic());
        c.addSignature(sign(users.get(8), c.getRawDataToSign(0)), 0);
        c.finalize();
        third.addTransaction(c);

        // Finalize third block and process
        third.finalize();
        assertTrue(chain.addBlock(third));
        handler.processBlock(third);

        // Check block of main chain: hash and coinbase
        assertEquals(third.getHash(), chain.getMaxHeightBlock().getHash());
        assertEquals(new Transaction(25, users.get(2).getPublic()), chain.getMaxHeightBlock().getCoinbase());

        // Check if UTXO pool still contains unspent outputs from transaction a
        assertTrue(chain.getMaxHeightUTXOPool().contains(new UTXO(a.getHash(), 1)));

        // Check if UTXO pool does not contains spent outputs from transaction a
        assertFalse(chain.getMaxHeightUTXOPool().contains(new UTXO(a.getHash(), 0)));

        // Check if UTXO pool contains unspent outputs from transaction c
        assertTrue(chain.getMaxHeightUTXOPool().contains(new UTXO(c.getHash(), 0)));

    }

    @Test
    public void testBlockFalseHeightConstraint() throws Exception {
        this.genesisSetUp();

        // Removed final attribute from BlockChain class to dynamically set CUT_OFF_AGE
        BlockChain.CUT_OFF_AGE = 10;

        Block first = new Block(genesis.getHash(), users.get(1).getPublic());
        first.finalize();
        assertTrue(chain.addBlock(first));

        Block previous = first;

        // Add multiple block, all on main branch
        for (int i = 0; i < BlockChain.CUT_OFF_AGE; i++) {

            Block current = new Block(previous.getHash(), users.get(i + 2).getPublic());
            current.finalize();
            assertTrue(chain.addBlock(current));

            previous = current;

        }

        // Add blocks which will fork main branch at end
        Block forkA = new Block(previous.getHash(), users.get(2).getPublic());
        forkA.finalize();
        assertTrue(chain.addBlock(forkA));

        Block forkB = new Block(previous.getHash(), users.get(3).getPublic());
        forkB.finalize();
        assertTrue(chain.addBlock(forkB));

        // Add block which will fork main branch close to origin
        Block forkCAtOrigin = new Block(first.getHash(), users.get(4).getPublic());
        forkCAtOrigin.finalize();

        // Height check invalid
        assertFalse(chain.addBlock(forkCAtOrigin));

    }

    @Test
    public void testBlockWithUTXOInTransactionAlreadyClaimed() throws Exception {
        this.genesisSetUp();

        // Create second block
        Block second = new Block(genesis.getHash(), users.get(1).getPublic());

        // Initiate first transaction and include in block
        Transaction a = new Transaction();
        a.addInput(genesis.getCoinbase().getHash(), 0);
        a.addOutput(15, users.get(1).getPublic());
        a.addSignature(sign(users.get(0), a.getRawDataToSign(0)), 0);
        a.finalize();
        second.addTransaction(a);

        // Finalize second block and process
        second.finalize();
        assertTrue(chain.addBlock(second));
        handler.processBlock(second);

        // Create third block
        Block third = new Block(second.getHash(), users.get(2).getPublic());

        // Initiate second transaction and include in block
        Transaction b = new Transaction();
        b.addInput(a.getHash(), 0);
        b.addOutput(10, users.get(2).getPublic());
        b.addSignature(sign(users.get(1), b.getRawDataToSign(0)), 0);
        b.finalize();
        third.addTransaction(b);

        // Finalize third block and process
        third.finalize();
        assertTrue(chain.addBlock(third));
        handler.processBlock(third);

        // Create fork block
        Block fork = new Block(genesis.getHash(), users.get(1).getPublic());

        // Initiate third transaction with spent genesis coinbase
        Transaction c = new Transaction();
        c.addInput(a.getHash(), 0);
        c.addOutput(10, users.get(10).getPublic());
        c.addSignature(sign(users.get(1), c.getRawDataToSign(0)), 0);
        c.finalize();

        // Include transactions in block
        fork.addTransaction(c);
        fork.finalize();

        // Ensure that second and fork block extend from same origin
        assertEquals(genesis.getHash(), fork.getPrevBlockHash());
        assertEquals(genesis.getHash(), second.getPrevBlockHash());
        assertEquals(second.getHash(), third.getPrevBlockHash());

        // Should return false due to invalid transaction
        assertFalse(chain.addBlock(fork));

    }

    @Test
    public void testAddingNewGenesisBlock() throws Exception {
        this.genesisSetUp();

        // Initiate another genesis block
        Block newGenesis = new Block(null, users.get(5).getPublic());
        newGenesis.finalize();

        // Try to process new genesis block
        assertFalse(handler.processBlock(newGenesis));

    }

    @Test
    public void testBlockWithInvalidPreviousHash() throws Exception {
        this.genesisSetUp();

        // Initiate block with invalid hash
        String str = "I am invalid";
        Block invalidPreviousHash = new Block(str.getBytes(), users.get(5).getPublic());
        invalidPreviousHash.finalize();

        // Try to process block with invalid previous hash
        assertFalse(handler.processBlock(invalidPreviousHash));

    }

}