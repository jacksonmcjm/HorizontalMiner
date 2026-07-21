package com.jackson.horizontalminer.inventory;

import com.jackson.horizontalminer.block.ModBlocks;
import com.jackson.horizontalminer.blockentity.MiningMachineBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.items.SlotItemHandler;

public class MiningMachineMenu extends AbstractContainerMenu {

    private static final int MACHINE_SLOT_COUNT = MiningMachineInventory.TOTAL_SLOTS;
    private static final int PLAYER_INVENTORY_START = MACHINE_SLOT_COUNT;
    private static final int PLAYER_INVENTORY_END = PLAYER_INVENTORY_START + 27;
    private static final int HOTBAR_START = PLAYER_INVENTORY_END;
    private static final int HOTBAR_END = HOTBAR_START + 9;

    private final ContainerLevelAccess access;

    public MiningMachineMenu(int containerId,
                             Inventory playerInventory,
                             FriendlyByteBuf extraData) {
        this(containerId, playerInventory, readMenuData(playerInventory, extraData));
    }

    private MiningMachineMenu(int containerId, Inventory playerInventory, MenuData menuData) {
        this(containerId, playerInventory, menuData.inventory(), menuData.blockPos());
    }

    public MiningMachineMenu(int containerId,
                             Inventory playerInventory,
                             MiningMachineInventory machineInventory,
                             BlockPos blockPos) {

        super(ModMenuTypes.MINING_MACHINE.get(), containerId);

        this.access = ContainerLevelAccess.create(playerInventory.player.level(), blockPos);

        // Fuel is the only machine slot players can insert into. Mining logic
        // will populate the nine extraction-only output slots.
        addSlot(new SlotItemHandler(machineInventory, MiningMachineInventory.FUEL_SLOT, 26, 18));
        for (int index = MiningMachineInventory.OUTPUT_START; index <= MiningMachineInventory.OUTPUT_END; index++) {
            int outputIndex = index - MiningMachineInventory.OUTPUT_START;
            addSlot(new OutputSlot(machineInventory, index, 62 + (outputIndex % 3) * 18,
                    18 + (outputIndex / 3) * 18));
        }

        for (int row = 0; row < 3; row++) {
            for (int column = 0; column < 9; column++) {
                addSlot(new Slot(playerInventory, column + row * 9 + 9,
                        8 + column * 18, 140 + row * 18));
            }
        }

        for (int column = 0; column < 9; column++) {
            addSlot(new Slot(playerInventory, column, 8 + column * 18, 198));
        }
    }

    @Override
    public boolean stillValid(Player player) {
        return stillValid(access, player, ModBlocks.MINING_MACHINE.get());
    }

    @Override
    public ItemStack quickMoveStack(Player player, int slot) {
        if (slot < 0 || slot >= slots.size()) {
            return ItemStack.EMPTY;
        }

        Slot sourceSlot = slots.get(slot);
        if (!sourceSlot.hasItem()) {
            return ItemStack.EMPTY;
        }

        ItemStack stackInSlot = sourceSlot.getItem();
        ItemStack originalStack = stackInSlot.copy();

        if (slot < MACHINE_SLOT_COUNT) {
            if (!moveItemStackTo(stackInSlot, PLAYER_INVENTORY_START, HOTBAR_END, true)) {
                return ItemStack.EMPTY;
            }
        } else if (!moveItemStackTo(stackInSlot, MiningMachineInventory.FUEL_SLOT,
                MiningMachineInventory.FUEL_SLOT + 1, false)) {
            return ItemStack.EMPTY;
        }

        if (stackInSlot.isEmpty()) {
            sourceSlot.set(ItemStack.EMPTY);
        } else {
            sourceSlot.setChanged();
        }

        sourceSlot.onTake(player, stackInSlot);
        return originalStack;
    }

    private static MenuData readMenuData(Inventory playerInventory, FriendlyByteBuf extraData) {
        BlockPos blockPos = extraData.readBlockPos();
        if (playerInventory.player.level().getBlockEntity(blockPos) instanceof MiningMachineBlockEntity machine) {
            return new MenuData(machine.getInventory(), blockPos);
        }

        throw new IllegalStateException("Mining machine block entity is missing at " + blockPos);
    }

    private record MenuData(MiningMachineInventory inventory, BlockPos blockPos) {
    }

    private static class OutputSlot extends SlotItemHandler {

        private OutputSlot(MiningMachineInventory inventory, int slot, int x, int y) {
            super(inventory, slot, x, y);
        }

        @Override
        public boolean mayPlace(ItemStack stack) {
            return false;
        }
    }
}
