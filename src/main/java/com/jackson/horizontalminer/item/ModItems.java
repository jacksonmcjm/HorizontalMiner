package com.jackson.horizontalminer.item;

import com.jackson.horizontalminer.HorizontalMiner;
import com.jackson.horizontalminer.block.ModBlocks;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ModItems {

    public static final DeferredRegister<Item> ITEMS =
            DeferredRegister.create(ForgeRegistries.ITEMS, HorizontalMiner.MOD_ID);

    public static final RegistryObject<Item> MINING_MACHINE =
            ITEMS.register("mining_machine",
                    () -> new BlockItem(
                            ModBlocks.MINING_MACHINE.get(),
                            new Item.Properties()
                    ));
}