package com.jackson.horizontalminer.blockentity;

import com.jackson.horizontalminer.inventory.MiningMachineInventory;
import com.jackson.horizontalminer.inventory.MiningMachineMenu;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

public class MiningMachineBlockEntity extends BlockEntity implements MenuProvider {

    private final MiningMachineInventory inventory =
            new MiningMachineInventory(this::setChanged);

    public MiningMachineBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.MINING_MACHINE.get(), pos, state);
    }

    public MiningMachineInventory getInventory() {
        return inventory;
    }

    @Override
    public Component getDisplayName() {
        return Component.literal("Mining Machine");
    }

    @Override
    public AbstractContainerMenu createMenu(int containerId,
                                            Inventory playerInventory,
                                            net.minecraft.world.entity.player.Player player) {

        return new MiningMachineMenu(containerId, playerInventory, inventory, worldPosition);
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        tag.put("Inventory", inventory.serializeNBT());
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        inventory.deserializeNBT(tag.getCompound("Inventory"));
    }
}
