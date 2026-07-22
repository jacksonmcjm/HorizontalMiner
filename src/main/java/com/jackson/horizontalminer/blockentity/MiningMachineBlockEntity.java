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
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Block;
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
    private int maximumBurnTime;
    private int currentTunnelDepth;
    private int miningProgress;
    private List<BlockPos> currentSlice = List.of();
    private List<PendingTarget> pendingTargets = List.of();
    private List<ItemStack> pendingDrops = List.of();
    private boolean outputBlocked;
    private final ContainerData burnTimeData = new ContainerData() {
        @Override
        public int get(int index) {
            return switch (index) {
                case 0 -> remainingBurnTime;
                case 1 -> maximumBurnTime;
                case 2 -> miningProgress;
                case 3 -> TICKS_PER_MINING_CYCLE;
                case 4 -> currentTunnelDepth;
                case 5 -> getMachineStatus().ordinal();
                default -> 0;
            };
        }

        @Override
        public void set(int index, int value) {
            switch (index) {
                case 0 -> remainingBurnTime = value;
                case 1 -> maximumBurnTime = value;
                case 2 -> miningProgress = value;
                case 4 -> currentTunnelDepth = value;
                default -> {
                }
            }
        }

        @Override
        public int getCount() {
            return 6;
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

    public int getMaximumBurnTime() {
        return maximumBurnTime;
    }

    public int getCurrentTunnelDepth() {
        return currentTunnelDepth;
    }

    public int getMiningProgress() {
        return miningProgress;
    }

    public boolean isOutputBlocked() {
        return outputBlocked;
    }

    public MachineStatus getMachineStatus() {
        if (outputBlocked) {
            return MachineStatus.OUTPUT_FULL;
        }
        if (remainingBurnTime > 0 || isValidFuel(inventory.getStackInSlot(MiningMachineInventory.FUEL_SLOT))) {
            return MachineStatus.MINING;
        }
        if (currentTunnelDepth > 0 || miningProgress > 0) {
            return MachineStatus.OUT_OF_FUEL;
        }
        return MachineStatus.IDLE;
    }

    /**
     * Builds the next 5-wide, 3-high vertical tunnel slice directly ahead of the machine.
     * The floor is at Y - 1; the full-width bottom row is at the machine's Y level.
     */
    public List<BlockPos> calculateCurrentSlice() {
        Direction facing = getBlockState().getValue(MiningMachineBlock.FACING);
        Direction acrossTunnel = facing.getClockWise();
        BlockPos sliceOrigin = worldPosition.relative(facing, currentTunnelDepth + 1);
        List<BlockPos> slice = new ArrayList<>(13);

        for (int sideways = -2; sideways <= 2; sideways++) {
            int highestY = Math.abs(sideways) <= 1 ? 2 : 1;
            for (int yOffset = 0; yOffset <= highestY; yOffset++) {
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

    /** Returns whether the machine has fuel available after output capacity is satisfied. */
    public boolean canOperate() {
        return remainingBurnTime > 0 || isValidFuel(inventory.getStackInSlot(MiningMachineInventory.FUEL_SLOT));
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, MiningMachineBlockEntity machine) {
        if (!(level instanceof ServerLevel serverLevel)) {
            return;
        }

        if (!machine.ensurePendingSlice(serverLevel)) {
            return;
        }

        if (!machine.canOperate()) {
            return;
        }

        if (machine.remainingBurnTime <= 0) {
            machine.consumeFuel();
        }

        if (machine.remainingBurnTime > 0) {
            machine.remainingBurnTime--;
            machine.tickMiningEngine(serverLevel);
            machine.setChanged();
        }
    }

    private void tickMiningEngine(ServerLevel level) {
        if (currentSlice.isEmpty()) {
            currentSlice = calculateCurrentSlice();
        }

        miningProgress++;
        if (miningProgress < TICKS_PER_MINING_CYCLE) {
            return;
        }

        completePendingSlice(level);
    }

    private static boolean isMineable(Level level, BlockPos target) {
        BlockState targetState = level.getBlockState(target);
        return !targetState.isAir()
                && targetState.getFluidState().isEmpty()
                && !targetState.is(Blocks.BEDROCK)
                && targetState.getDestroySpeed(level, target) >= 0.0F;
    }

    /**
     * Rolls loot once for the current slice and retains those exact stacks until the
     * cycle completes. An output-capacity failure pauses before any further fuel burns.
     */
    private boolean ensurePendingSlice(ServerLevel level) {
        if (!pendingTargets.isEmpty() || !pendingDrops.isEmpty()) {
            if (canInsertAll(pendingDrops)) {
                outputBlocked = false;
                return true;
            }

            outputBlocked = true;
            return false;
        }

        if (currentSlice.isEmpty()) {
            currentSlice = calculateCurrentSlice();
        }

        List<PendingTarget> targets = new ArrayList<>();
        List<ItemStack> drops = new ArrayList<>();
        for (BlockPos target : currentSlice) {
            if (!isMineable(level, target)) {
                continue;
            }

            BlockState targetState = level.getBlockState(target);
            targets.add(new PendingTarget(target.immutable(), Block.getId(targetState)));
            drops.addAll(Block.getDrops(targetState, level, target, level.getBlockEntity(target), null,
                    ItemStack.EMPTY));
        }

        pendingTargets = List.copyOf(targets);
        pendingDrops = copyStacks(drops);
        outputBlocked = !canInsertAll(pendingDrops);
        return !outputBlocked;
    }

    private void completePendingSlice(ServerLevel level) {
        if (!pendingTargetsAreUnchanged(level)) {
            clearPendingSlice();
            miningProgress = 0;
            return;
        }

        if (!canInsertAll(pendingDrops)) {
            outputBlocked = true;
            return;
        }

        if (!insertAllDrops(pendingDrops)) {
            outputBlocked = true;
            return;
        }

        for (PendingTarget target : pendingTargets) {
            level.setBlock(target.pos(), Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
        }

        LOGGER.info("Mining Machine at {} mined slice depth {}: removed {} blocks and inserted {} drop stacks",
                worldPosition, currentTunnelDepth, pendingTargets.size(), pendingDrops.size());
        currentTunnelDepth++;
        miningProgress = 0;
        currentSlice = calculateCurrentSlice();
        clearPendingSlice();
    }

    private boolean pendingTargetsAreUnchanged(ServerLevel level) {
        return pendingTargets.stream().allMatch(target -> {
            BlockState state = level.getBlockState(target.pos());
            return isMineable(level, target.pos()) && Block.getId(state) == target.stateId();
        });
    }

    private boolean canInsertAll(List<ItemStack> drops) {
        List<ItemStack> simulatedOutputs = new ArrayList<>();
        for (int slot = MiningMachineInventory.OUTPUT_START; slot <= MiningMachineInventory.OUTPUT_END; slot++) {
            simulatedOutputs.add(inventory.getStackInSlot(slot).copy());
        }

        for (ItemStack drop : drops) {
            if (!simulateInsert(simulatedOutputs, drop.copy()).isEmpty()) {
                return false;
            }
        }

        return true;
    }

    private static ItemStack simulateInsert(List<ItemStack> outputs, ItemStack stack) {
        for (ItemStack existing : outputs) {
            if (ItemStack.isSameItemSameTags(existing, stack)) {
                int moved = Math.min(stack.getCount(), existing.getMaxStackSize() - existing.getCount());
                if (moved > 0) {
                    existing.grow(moved);
                    stack.shrink(moved);
                }
            }
        }

        for (int index = 0; index < outputs.size() && !stack.isEmpty(); index++) {
            if (outputs.get(index).isEmpty()) {
                int moved = Math.min(stack.getCount(), stack.getMaxStackSize());
                ItemStack inserted = stack.copy();
                inserted.setCount(moved);
                outputs.set(index, inserted);
                stack.shrink(moved);
            }
        }

        return stack;
    }

    private boolean insertAllDrops(List<ItemStack> drops) {
        List<ItemStack> originalOutputs = new ArrayList<>();
        for (int slot = MiningMachineInventory.OUTPUT_START; slot <= MiningMachineInventory.OUTPUT_END; slot++) {
            originalOutputs.add(inventory.getStackInSlot(slot).copy());
        }

        for (ItemStack drop : drops) {
            if (!insertOutput(drop.copy()).isEmpty()) {
                restoreOutputSlots(originalOutputs);
                return false;
            }
        }

        return true;
    }

    private ItemStack insertOutput(ItemStack stack) {
        for (int slot = MiningMachineInventory.OUTPUT_START;
             slot <= MiningMachineInventory.OUTPUT_END && !stack.isEmpty(); slot++) {
            ItemStack existing = inventory.getStackInSlot(slot);
            if (ItemStack.isSameItemSameTags(existing, stack)) {
                int moved = Math.min(stack.getCount(), existing.getMaxStackSize() - existing.getCount());
                if (moved > 0) {
                    existing.grow(moved);
                    stack.shrink(moved);
                    inventory.setStackInSlot(slot, existing);
                }
            }
        }

        for (int slot = MiningMachineInventory.OUTPUT_START;
             slot <= MiningMachineInventory.OUTPUT_END && !stack.isEmpty(); slot++) {
            if (inventory.getStackInSlot(slot).isEmpty()) {
                int moved = Math.min(stack.getCount(), stack.getMaxStackSize());
                ItemStack inserted = stack.copy();
                inserted.setCount(moved);
                inventory.setStackInSlot(slot, inserted);
                stack.shrink(moved);
            }
        }

        return stack;
    }

    private void restoreOutputSlots(List<ItemStack> originalOutputs) {
        for (int index = 0; index < originalOutputs.size(); index++) {
            inventory.setStackInSlot(MiningMachineInventory.OUTPUT_START + index, originalOutputs.get(index));
        }
    }

    private void clearPendingSlice() {
        pendingTargets = List.of();
        pendingDrops = List.of();
        outputBlocked = false;
    }

    private static List<ItemStack> copyStacks(List<ItemStack> stacks) {
        return stacks.stream().filter(stack -> !stack.isEmpty()).map(ItemStack::copy).toList();
    }

    private record PendingTarget(BlockPos pos, int stateId) {
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
        maximumBurnTime = burnDuration;
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
        tag.putInt("MaximumBurnTime", maximumBurnTime);
        tag.putInt("CurrentTunnelDepth", currentTunnelDepth);
        tag.putInt("MiningProgress", miningProgress);
        tag.putBoolean("OutputBlocked", outputBlocked);

        ListTag sliceTag = new ListTag();
        for (BlockPos slicePos : currentSlice) {
            CompoundTag positionTag = new CompoundTag();
            positionTag.putInt("X", slicePos.getX());
            positionTag.putInt("Y", slicePos.getY());
            positionTag.putInt("Z", slicePos.getZ());
            sliceTag.add(positionTag);
        }
        tag.put("CurrentSlice", sliceTag);

        ListTag pendingTargetTag = new ListTag();
        for (PendingTarget target : pendingTargets) {
            CompoundTag targetTag = new CompoundTag();
            targetTag.putInt("X", target.pos().getX());
            targetTag.putInt("Y", target.pos().getY());
            targetTag.putInt("Z", target.pos().getZ());
            targetTag.putInt("StateId", target.stateId());
            pendingTargetTag.add(targetTag);
        }
        tag.put("PendingTargets", pendingTargetTag);

        ListTag pendingDropTag = new ListTag();
        for (ItemStack drop : pendingDrops) {
            pendingDropTag.add(drop.save(new CompoundTag()));
        }
        tag.put("PendingDrops", pendingDropTag);
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        inventory.deserializeNBT(tag.getCompound("Inventory"));
        remainingBurnTime = tag.getInt("RemainingBurnTime");
        maximumBurnTime = Math.max(tag.getInt("MaximumBurnTime"), remainingBurnTime);
        currentTunnelDepth = tag.getInt("CurrentTunnelDepth");
        miningProgress = tag.getInt("MiningProgress");
        outputBlocked = tag.getBoolean("OutputBlocked");

        List<BlockPos> loadedSlice = new ArrayList<>();
        ListTag sliceTag = tag.getList("CurrentSlice", Tag.TAG_COMPOUND);
        for (int index = 0; index < sliceTag.size(); index++) {
            CompoundTag positionTag = sliceTag.getCompound(index);
            loadedSlice.add(new BlockPos(positionTag.getInt("X"), positionTag.getInt("Y"),
                    positionTag.getInt("Z")));
        }
        currentSlice = List.copyOf(loadedSlice);

        List<PendingTarget> loadedTargets = new ArrayList<>();
        ListTag pendingTargetTag = tag.getList("PendingTargets", Tag.TAG_COMPOUND);
        for (int index = 0; index < pendingTargetTag.size(); index++) {
            CompoundTag targetTag = pendingTargetTag.getCompound(index);
            loadedTargets.add(new PendingTarget(new BlockPos(targetTag.getInt("X"), targetTag.getInt("Y"),
                    targetTag.getInt("Z")), targetTag.getInt("StateId")));
        }
        pendingTargets = List.copyOf(loadedTargets);

        List<ItemStack> loadedDrops = new ArrayList<>();
        ListTag pendingDropTag = tag.getList("PendingDrops", Tag.TAG_COMPOUND);
        for (int index = 0; index < pendingDropTag.size(); index++) {
            ItemStack drop = ItemStack.of(pendingDropTag.getCompound(index));
            if (!drop.isEmpty()) {
                loadedDrops.add(drop);
            }
        }
        pendingDrops = List.copyOf(loadedDrops);
    }

    public enum MachineStatus {
        MINING,
        OUT_OF_FUEL,
        OUTPUT_FULL,
        IDLE;

        public static MachineStatus byId(int id) {
            MachineStatus[] values = values();
            return id >= 0 && id < values.length ? values[id] : IDLE;
        }
    }
}
