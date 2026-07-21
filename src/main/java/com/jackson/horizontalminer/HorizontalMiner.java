package com.jackson.horizontalminer;

import com.jackson.horizontalminer.blockentity.ModBlockEntities;
import com.mojang.logging.LogUtils;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.slf4j.Logger;
import com.jackson.horizontalminer.block.ModBlocks;
import com.jackson.horizontalminer.item.ModItems;
import com.jackson.horizontalminer.inventory.ModMenuTypes;

@Mod(HorizontalMiner.MOD_ID)
public class HorizontalMiner {

    public static final String MOD_ID = "horizontalminer";
    private static final Logger LOGGER = LogUtils.getLogger();

    public HorizontalMiner() {
        var modEventBus = FMLJavaModLoadingContext.get().getModEventBus();

        ModBlocks.BLOCKS.register(modEventBus);
        ModItems.ITEMS.register(modEventBus);
        ModBlockEntities.BLOCK_ENTITIES.register(modEventBus);
        ModMenuTypes.MENUS.register(modEventBus);

        LOGGER.info("Horizontal Miner is loading!");
    }
}