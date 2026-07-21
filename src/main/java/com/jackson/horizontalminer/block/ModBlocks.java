package com.jackson.horizontalminer.block;

import com.jackson.horizontalminer.HorizontalMiner;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ModBlocks {

    public static final DeferredRegister<Block> BLOCKS =
            DeferredRegister.create(ForgeRegistries.BLOCKS, HorizontalMiner.MOD_ID);

    public static final RegistryObject<Block> MINING_MACHINE =
            BLOCKS.register("mining_machine",
                    () -> new MiningMachineBlock(
                            Block.Properties.copy(Blocks.IRON_BLOCK)
                    ));
}