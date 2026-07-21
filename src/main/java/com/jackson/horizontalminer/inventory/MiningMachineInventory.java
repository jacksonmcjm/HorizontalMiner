package com.jackson.horizontalminer.inventory;

import com.jackson.horizontalminer.blockentity.MiningMachineBlockEntity;
import net.minecraft.world.item.ItemStack;
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
    public boolean isItemValid(int slot, ItemStack stack) {
        return slot == FUEL_SLOT && MiningMachineBlockEntity.isValidFuel(stack);
    }

    @Override
    protected void onContentsChanged(int slot) {
        super.onContentsChanged(slot);
        onContentsChanged.run();
    }
}
