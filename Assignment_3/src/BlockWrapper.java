public class BlockWrapper {

    private Block block;
    private BlockWrapper previousBlock;
    private int height;
    private UTXOPool pool;


    public BlockWrapper(Block block, BlockWrapper previousBlock, UTXOPool pool) {
        this.block = block;
        this.pool = pool;

        // Check if first block on chain
        if (previousBlock != null) {
            this.previousBlock = previousBlock;
            this.height = previousBlock.height + 1;
        } else {
            height = 1;
        }
    }

    public Block getRawBlock() {
        // Return the unwrapped block
        return this.block;
    }

    public UTXOPool getPool() {
        // Return UTXO pool corresponding to block
        return new UTXOPool(this.pool);
    }

    public int getHeight() {
        return this.height;
    }

}
