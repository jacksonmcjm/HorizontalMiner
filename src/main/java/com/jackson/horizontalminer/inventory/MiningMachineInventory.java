package com.jackson.horizontalminer.inventory;

import net.minecraftforge.items.ItemStackHandler;
import net.minecraftforge.items.ItemStackHandler;

public class MiningMachineInventory extends ItemStackHandler {

    public static final int FUEL_SLOT = 0;
    public static final int OUTPUT_START = 1;
    public static final int OUTPUT_END = 9;
    public static final int TOTAL_SLOTS = 10;

    private final Runnable onContentsChanged;

    public MiningMachineInventory(Runnable onContentsChanged) {
        super(TOTAL_SLOTS);
        this.onContentsChanged = onContentsChanged;
    }

    @Override
    protected void onContentsChanged(int slot) {
        super.onContentsChanged(slot);
        onContentsChanged.run();
    }
}