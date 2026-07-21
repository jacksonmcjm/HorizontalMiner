package com.jackson.horizontalminer.blockentity;

import com.jackson.horizontalminer.HorizontalMiner;
import com.jackson.horizontalminer.block.ModBlocks;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ModBlockEntities {

    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
            DeferredRegister.create(ForgeRegistries.BLOCK_ENTITY_TYPES, HorizontalMiner.MOD_ID);

    public static final RegistryObject<BlockEntityType<MiningMachineBlockEntity>> MINING_MACHINE =
            BLOCK_ENTITIES.register("mining_machine",
                    () -> BlockEntityType.Builder.of(
                            MiningMachineBlockEntity::new,
                            ModBlocks.MINING_MACHINE.get()
                    ).build(null));
}