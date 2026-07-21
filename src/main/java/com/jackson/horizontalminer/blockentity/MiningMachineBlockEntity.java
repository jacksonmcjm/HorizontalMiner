package com.jackson.horizontalminer.blockentity;

import com.jackson.horizontalminer.block.MiningMachineBlock;
import com.jackson.horizontalminer.inventory.MiningMachineInventory;
import com.jackson.horizontalminer.inventory.MiningMachineMenu;
import com.mojang.logging.LogUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.Level;
import net.minecraftforge.common.ForgeHooks;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.List;

public class MiningMachineBlockEntity extends BlockEntity implements MenuProvider {

    private static final Logger LOGGER = LogUtils.getLogger();
    private static final int TICKS_PER_MINING_CYCLE = 100;

    private final MiningMachineInventory inventory =
            new MiningMachineInventory(this::setChanged);
    private int remainingBurnTime;
    private int currentTunnelDepth;
    private int miningProgress;
    private List<BlockPos> currentSlice = List.of();
    private final ContainerData burnTimeData = new ContainerData() {
        @Override
        public int get(int index) {
            return index == 0 ? remainingBurnTime : 0;
        }

        @Override
        public void set(int index, int value) {
            if (index == 0) {
                remainingBurnTime = value;
            }
        }

        @Override
        public int getCount() {
            return 1;
        }
    };

    public MiningMachineBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.MINING_MACHINE.get(), pos, state);
    }

    public MiningMachineInventory getInventory() {
        return inventory;
    }

    public ContainerData getBurnTimeData() {
        return burnTimeData;
    }

    public int getRemainingBurnTime() {
        return remainingBurnTime;
    }

    public int getCurrentTunnelDepth() {
        return currentTunnelDepth;
    }

    public int getMiningProgress() {
        return miningProgress;
    }

    /**
     * Builds the next 5-wide, 3-high vertical tunnel slice directly ahead of the machine.
     * The floor (Y + 0) is omitted; the outer columns omit Y + 1.
     */
    public List<BlockPos> calculateCurrentSlice() {
        Direction facing = getBlockState().getValue(MiningMachineBlock.FACING);
        Direction acrossTunnel = facing.getClockWise();
        BlockPos sliceOrigin = worldPosition.relative(facing, currentTunnelDepth + 1);
        List<BlockPos> slice = new ArrayList<>(13);

        for (int sideways = -2; sideways <= 2; sideways++) {
            int lowestY = Math.abs(sideways) <= 1 ? 1 : 2;
            for (int yOffset = lowestY; yOffset <= 3; yOffset++) {
                slice.add(sliceOrigin.relative(acrossTunnel, sideways).above(yOffset));
            }
        }

        return List.copyOf(slice);
    }

    public List<BlockPos> getCurrentSlice() {
        return currentSlice.isEmpty() ? calculateCurrentSlice() : List.copyOf(currentSlice);
    }

    public static boolean isValidFuel(ItemStack stack) {
        return !stack.isEmpty() && ForgeHooks.getBurnTime(stack, RecipeType.SMELTING) > 0;
    }

    /**
     * This debug-stage engine can always evaluate and advance a slice, even when that
     * slice contains no mineable blocks. Real mining work checks will replace this gate.
     */
    public boolean canOperate() {
        return remainingBurnTime > 0 || isValidFuel(inventory.getStackInSlot(MiningMachineInventory.FUEL_SLOT));
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, MiningMachineBlockEntity machine) {
        if (!machine.canOperate()) {
            return;
        }

        if (machine.remainingBurnTime <= 0) {
            machine.consumeFuel();
        }

        if (machine.remainingBurnTime > 0) {
            machine.remainingBurnTime--;
            machine.tickMiningEngine(level);
            machine.setChanged();
        }
    }

    private void tickMiningEngine(Level level) {
        if (currentSlice.isEmpty()) {
            currentSlice = calculateCurrentSlice();
        }

        miningProgress++;
        if (miningProgress < TICKS_PER_MINING_CYCLE) {
            return;
        }

        List<BlockPos> completedSlice = currentSlice;
        List<BlockPos> mineableBlocks = completedSlice.stream()
                .filter(target -> isMineable(level, target))
                .toList();

        LOGGER.info("Mining Machine at {} completed slice depth {} facing {}: planned targets {}; "
                        + "{} mineable targets {}",
                worldPosition, currentTunnelDepth,
                getBlockState().getValue(MiningMachineBlock.FACING), completedSlice,
                mineableBlocks.size(), mineableBlocks);

        currentTunnelDepth++;
        miningProgress = 0;
        currentSlice = calculateCurrentSlice();
    }

    private static boolean isMineable(Level level, BlockPos target) {
        BlockState targetState = level.getBlockState(target);
        return !targetState.isAir()
                && targetState.getFluidState().isEmpty()
                && !targetState.is(Blocks.BEDROCK)
                && targetState.getDestroySpeed(level, target) >= 0.0F;
    }

    private boolean consumeFuel() {
        ItemStack fuel = inventory.getStackInSlot(MiningMachineInventory.FUEL_SLOT);
        int burnDuration = ForgeHooks.getBurnTime(fuel, RecipeType.SMELTING);
        if (burnDuration <= 0) {
            return false;
        }

        ItemStack remainder = fuel.getCraftingRemainingItem();
        fuel.shrink(1);
        if (fuel.isEmpty()) {
            inventory.setStackInSlot(MiningMachineInventory.FUEL_SLOT, remainder);
        }

        remainingBurnTime = burnDuration;
        setChanged();
        return true;
    }

    @Override
    public Component getDisplayName() {
        return Component.literal("Mining Machine");
    }

    @Override
    public AbstractContainerMenu createMenu(int containerId,
                                            Inventory playerInventory,
                                            net.minecraft.world.entity.player.Player player) {

        return new MiningMachineMenu(containerId, playerInventory, inventory, worldPosition, burnTimeData);
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        tag.put("Inventory", inventory.serializeNBT());
        tag.putInt("RemainingBurnTime", remainingBurnTime);
        tag.putInt("CurrentTunnelDepth", currentTunnelDepth);
        tag.putInt("MiningProgress", miningProgress);

        ListTag sliceTag = new ListTag();
        for (BlockPos slicePos : currentSlice) {
            CompoundTag positionTag = new CompoundTag();
            positionTag.putInt("X", slicePos.getX());
            positionTag.putInt("Y", slicePos.getY());
            positionTag.putInt("Z", slicePos.getZ());
            sliceTag.add(positionTag);
        }
        tag.put("CurrentSlice", sliceTag);
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        inventory.deserializeNBT(tag.getCompound("Inventory"));
        remainingBurnTime = tag.getInt("RemainingBurnTime");
        currentTunnelDepth = tag.getInt("CurrentTunnelDepth");
        miningProgress = tag.getInt("MiningProgress");

        List<BlockPos> loadedSlice = new ArrayList<>();
        ListTag sliceTag = tag.getList("CurrentSlice", Tag.TAG_COMPOUND);
        for (int index = 0; index < sliceTag.size(); index++) {
            CompoundTag positionTag = sliceTag.getCompound(index);
            loadedSlice.add(new BlockPos(positionTag.getInt("X"), positionTag.getInt("Y"),
                    positionTag.getInt("Z")));
        }
        currentSlice = List.copyOf(loadedSlice);
    }
}
