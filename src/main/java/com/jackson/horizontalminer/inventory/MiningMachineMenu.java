package com.jackson.horizontalminer.inventory;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;

public class MiningMachineMenu extends AbstractContainerMenu {

    private final Player player;

    public MiningMachineMenu(int containerId,
                             Inventory playerInventory,
                             FriendlyByteBuf extraData) {

        super(ModMenuTypes.MINING_MACHINE.get(), containerId);

        this.player = playerInventory.player;
    }

    @Override
    public boolean stillValid(Player player) {
        return true;
    }

    @Override
    public ItemStack quickMoveStack(Player player, int slot) {
        return ItemStack.EMPTY;
    }

    public Player getPlayer() {
        return player;
    }
}