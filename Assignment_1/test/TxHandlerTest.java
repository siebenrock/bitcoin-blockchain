import org.junit.jupiter.api.Test;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.spec.ECGenParameterSpec;
import java.security.Signature;
import java.security.SignatureException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;

import junit.framework.TestCase;

class TxHandlerTest extends TestCase {
    private TxHandler handler;
    private ArrayList<Transaction> testTxs = new ArrayList<>();
    private Transaction initTx = new Transaction();
    private KeyPair alice;
    private KeyPair bob;
    private KeyPair carol;
    private KeyPair eve;

    @Override
    protected void setUp() throws Exception {
        super.setUp();

        // Generate key pairs
        KeyPairGenerator keyGen = KeyPairGenerator.getInstance("RSA");
        // ECGenParameterSpec ecSpec = new ECGenParameterSpec("secp256k1");
        // keyGen.initialize(ecSpec);

        alice = keyGen.generateKeyPair();
        bob = keyGen.generateKeyPair();
        carol = keyGen.generateKeyPair();
        eve = keyGen.generateKeyPair();

        // Init transaction, finalize to set hash
        initTx.addOutput(1000, alice.getPublic());
        initTx.finalize();

        // Init pool
        UTXOPool pool = new UTXOPool();
        UTXO initUTXO = new UTXO(initTx.getHash(), 0);
        pool.addUTXO(initUTXO, initTx.getOutput(0));

        handler = new TxHandler(pool);
    }


    private byte[] sign(KeyPair keys, byte[] message)
            throws NoSuchAlgorithmException, SignatureException, InvalidKeyException {
        Signature signature = Signature.getInstance("SHA256withRSA");
        signature.initSign(keys.getPrivate());
        signature.update(message);
        return signature.sign();
    }

    @Test
    void testValidTx() throws Exception {
        this.setUp();

        Transaction testTxToBob = new Transaction();
        testTxToBob.addInput(initTx.getHash(), 0);
        testTxToBob.addOutput(100, bob.getPublic());
        testTxToBob.addOutput(890, alice.getPublic());
        byte[] signatureAlice = sign(alice, testTxToBob.getRawDataToSign(0));
        testTxToBob.addSignature(signatureAlice, 0);
        testTxToBob.finalize();
        // assertTrue(handler.isValidTx(testTxToBob));
        testTxs.add(testTxToBob);

        Transaction testTxToCarolAndEve = new Transaction();
        testTxToCarolAndEve.addInput(testTxToBob.getHash(), 0);
        testTxToCarolAndEve.addOutput(4, carol.getPublic());
        testTxToCarolAndEve.addOutput(5, eve.getPublic());
        testTxToCarolAndEve.addOutput(90, bob.getPublic());
        byte[] signatureBob = sign(bob, testTxToCarolAndEve.getRawDataToSign(0));
        testTxToCarolAndEve.addSignature(signatureBob, 0);
        testTxToCarolAndEve.finalize();
        testTxs.add(testTxToCarolAndEve);

        Transaction[] testTxsFixed = new Transaction[testTxs.size()];
        Transaction[] handledTx = handler.handleTxs(testTxs.toArray(testTxsFixed));
        assertEquals(2, handledTx.length);
    }

    @Test
    void testInvalidSignature() throws Exception {
        this.setUp();

        Transaction testTx = new Transaction();
        testTx.addInput(initTx.getHash(), 0);
        testTx.addOutput(100, bob.getPublic());
        testTx.addOutput(895, alice.getPublic());
        byte[] signature = sign(bob, testTx.getRawDataToSign(0));
        testTx.addSignature(signature, 0);
        testTx.finalize();
        // assertFalse(handler.isValidTx(testTx));
        testTxs.add(testTx);

        Transaction[] testTxsFixed = new Transaction[testTxs.size()];
        Transaction[] handledTx = handler.handleTxs(testTxs.toArray(testTxsFixed));
        assertEquals(0, handledTx.length);
    }

    @Test
    void testInsufficientInput() throws Exception {
        this.setUp();

        Transaction testTx = new Transaction();
        testTx.addInput(initTx.getHash(), 0);
        testTx.addOutput(100, bob.getPublic());
        testTx.addOutput(950, alice.getPublic());
        byte[] signature = sign(alice, testTx.getRawDataToSign(0));
        testTx.addSignature(signature, 0);
        testTx.finalize();
        assertFalse(handler.isValidTx(testTx));
    }

    @Test
    void testDoubleSpendingMultipleTransactions() throws Exception {
        this.setUp();

        Transaction testTxToBob = new Transaction();
        testTxToBob.addInput(initTx.getHash(), 0);
        testTxToBob.addOutput(950, bob.getPublic());
        byte[] signatureAliceToBob = sign(alice, testTxToBob.getRawDataToSign(0));
        testTxToBob.addSignature(signatureAliceToBob, 0);
        testTxToBob.finalize();
        testTxs.add(testTxToBob);

        Transaction testTxToCarol = new Transaction();
        testTxToCarol.addInput(initTx.getHash(), 0);
        testTxToCarol.addOutput(900, bob.getPublic());
        byte[] signatureAliceToCarol = sign(alice, testTxToCarol.getRawDataToSign(0));
        testTxToCarol.addSignature(signatureAliceToCarol, 0);
        testTxToCarol.finalize();
        testTxs.add(testTxToCarol);

        Transaction[] testTxsFixed = new Transaction[testTxs.size()];
        Transaction[] handledTx = handler.handleTxs(testTxs.toArray(testTxsFixed));
        // Expected to accept only one transaction
        assertEquals(1, handledTx.length);
    }

    @Test
    void testDoubleSpendingSingleTransaction() throws Exception {
        this.setUp();

        Transaction testTx = new Transaction();
        testTx.addInput(initTx.getHash(), 0);
        testTx.addInput(initTx.getHash(), 0);
        testTx.addOutput(100, bob.getPublic());
        testTx.addOutput(950, alice.getPublic());
        byte[] signature = sign(alice, testTx.getRawDataToSign(0));
        testTx.addSignature(signature, 0);
        byte[] signatureDoubleSpend = sign(alice, testTx.getRawDataToSign(1));
        testTx.addSignature(signatureDoubleSpend, 1);
        testTx.finalize();
        assertFalse(handler.isValidTx(testTx));
    }

    @Test
    void testUnknownClaimedOutput() throws Exception {
        this.setUp();

        Transaction testTxUnkown = new Transaction();
        testTxUnkown.addInput(initTx.getHash(), 0);
        testTxUnkown.addOutput(950, bob.getPublic());
        byte[] signatureAlice = sign(alice, testTxUnkown.getRawDataToSign(0));
        testTxUnkown.addSignature(signatureAlice, 0);
        testTxUnkown.finalize();

        Transaction testTx = new Transaction();
        testTx.addInput(testTxUnkown.getHash(), 0);
        testTx.addOutput(100, carol.getPublic());
        testTx.addOutput(850, bob.getPublic());
        byte[] signatureBob = sign(bob, testTx.getRawDataToSign(0));
        testTx.addSignature(signatureBob, 0);
        testTx.finalize();
        assertFalse(handler.isValidTx(testTx));
    }

    @Test
    void testNegativeOutput() throws Exception {
        this.setUp();

        Transaction testTx = new Transaction();
        testTx.addInput(initTx.getHash(), 0);
        testTx.addOutput(-100, bob.getPublic());
        byte[] signature = sign(alice, testTx.getRawDataToSign(0));
        testTx.addSignature(signature, 0);
        testTx.finalize();
        assertFalse(handler.isValidTx(testTx));
    }

}