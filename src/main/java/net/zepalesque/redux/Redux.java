package net.zepalesque.redux;

import com.aetherteam.aether.AetherConfig;
import com.aetherteam.aether.block.AetherBlocks;
import com.aetherteam.aether.block.dispenser.DispenseUsableItemBehavior;
import com.aetherteam.aether.entity.AetherEntityTypes;
import com.aetherteam.aether_genesis.entity.GenesisEntityTypes;
import com.mojang.logging.LogUtils;
import com.mojang.serialization.Codec;
import net.minecraft.SharedConstants;
import net.minecraft.client.Minecraft;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.Registry;
import net.minecraft.data.DataGenerator;
import net.minecraft.data.PackOutput;
import net.minecraft.data.metadata.PackMetadataGenerator;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.metadata.pack.PackMetadataSection;
import net.minecraft.server.packs.repository.Pack;
import net.minecraft.server.packs.repository.PackSource;
import net.minecraft.world.flag.FeatureFlag;
import net.minecraft.world.flag.FeatureFlagSet;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DispenserBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.material.MapColor;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.crafting.CraftingHelper;
import net.minecraftforge.common.data.ExistingFileHelper;
import net.minecraftforge.data.event.GatherDataEvent;
import net.minecraftforge.event.AddPackFindersEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegisterEvent;
import net.minecraftforge.resource.PathPackResources;
import net.zepalesque.redux.advancement.trigger.ReduxAdvancementTriggers;
import net.zepalesque.redux.api.blockhandler.WoodHandler;
import net.zepalesque.redux.api.condition.AbstractCondition;
import net.zepalesque.redux.api.condition.ConditionSerializers;
import net.zepalesque.redux.api.packconfig.PackConfigBootstrap;
import net.zepalesque.redux.block.ReduxBlocks;
import net.zepalesque.redux.block.util.ReduxSoundTypes;
import net.zepalesque.redux.blockentity.ReduxBlockEntityTypes;
import net.zepalesque.redux.builtin.BuiltinPackUtils;
import net.zepalesque.redux.client.ReduxClient;
import net.zepalesque.redux.client.resource.ReduxOverridesPackResources;
import net.zepalesque.redux.client.audio.ReduxSoundEvents;
import net.zepalesque.redux.client.particle.ReduxParticleTypes;
import net.zepalesque.redux.client.render.ReduxRenderers;
import net.zepalesque.redux.config.ReduxConfig;
import net.zepalesque.redux.config.pack.ReduxPackConfig;
import net.zepalesque.redux.data.*;
import net.zepalesque.redux.data.loot.ReduxLootData;
import net.zepalesque.redux.data.tags.*;
import net.zepalesque.redux.effect.ReduxEffects;
import net.zepalesque.redux.entity.ReduxEntityTypes;
import net.zepalesque.redux.event.listener.MobSoundListener;
import net.zepalesque.redux.item.ReduxItems;
import net.zepalesque.redux.loot.condition.ReduxLootConditions;
import net.zepalesque.redux.loot.modifiers.ReduxLootModifiers;
import net.zepalesque.redux.misc.ReduxPackSources;
import net.zepalesque.redux.network.ReduxPacketHandler;
import net.zepalesque.redux.recipe.ReduxRecipeTypes;
import net.zepalesque.redux.recipe.condition.DataRecipeCondition;
import net.zepalesque.redux.recipe.serializer.ReduxRecipeSerializers;
import net.zepalesque.redux.world.biome.ReduxRegion;
import net.zepalesque.redux.world.biome.ReduxSurfaceData;
import net.zepalesque.redux.world.biome.modifier.ReduxBiomeModifierSerializers;
import net.zepalesque.redux.world.biome.surfacerule.ReduxConditionSources;
import net.zepalesque.redux.world.carver.ReduxCarvers;
import net.zepalesque.redux.world.density.ReduxDensityFunctionTypes;
import net.zepalesque.redux.world.feature.ReduxFeatureRegistry;
import net.zepalesque.redux.world.placement.ReduxPlacementModifiers;
import net.zepalesque.redux.world.stateprov.ReduxStateProviders;
import net.zepalesque.redux.world.structure.ReduxStructureTypes;
import net.zepalesque.redux.world.tree.decorator.ReduxTreeDecorators;
import net.zepalesque.redux.world.tree.foliage.ReduxFoliagePlacers;
import net.zepalesque.redux.world.tree.root.ReduxRootPlacers;
import net.zepalesque.redux.world.tree.trunk.ReduxTrunkPlacers;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import teamrazor.aeroblender.aether.AetherRuleCategory;
import terrablender.api.Regions;
import terrablender.api.SurfaceRuleManager;

import java.nio.file.Path;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

@Mod(Redux.MODID)
public class Redux
{

    public static final String MODID = "aether_redux";
    public static final Logger LOGGER = LogUtils.getLogger();
    @Nullable
    public static ReduxPackConfig packConfig;

    public Redux()
    {
        IEventBus bus = FMLJavaModLoadingContext.get().getModEventBus();
        bus.addListener(this::commonSetup);
        bus.addListener(this::clientSetup);
        bus.addListener(ReduxClient::blockColors);
        bus.addListener(ReduxClient::itemColors);
        bus.addListener(this::applyResourcePack);
        bus.addListener(EventPriority.HIGH, this::packSetup);
        bus.addListener(this::dataSetup);
        bus.addListener(this::registerRecipeSerializers);
        packConfig = PackConfigBootstrap.register("aether_redux_pack_config", ReduxPackConfig::new);
        ReduxBlocks.BLOCKS.register(bus);
        ReduxItems.ITEMS.register(bus);
        ReduxSoundEvents.SOUNDS.register(bus);
        ReduxBlockEntityTypes.BLOCK_ENTITY_TYPES.register(bus);
        ReduxEntityTypes.ENTITY_TYPES.register(bus);
        ConditionSerializers.CONDITION_SERIALIZERS.register(bus);
        ReduxBiomeModifierSerializers.BIOME_MODIFIER_SERIALIZERS.register(bus);
        ReduxLootModifiers.GLOBAL_LOOT_MODIFIERS.register(bus);
        ReduxEffects.EFFECTS.register(bus);
        ReduxRecipeTypes.RECIPE_TYPES.register(bus);
        ReduxRecipeSerializers.RECIPE_SERIALIZERS.register(bus);
        ReduxCarvers.CARVERS.register(bus);
        ReduxParticleTypes.PARTICLES.register(bus);
        ReduxFoliagePlacers.FOLIAGE_PLACERS.register(bus);
        ReduxFeatureRegistry.FEATURES.register(bus);
        ReduxDensityFunctionTypes.DENSITY_FUNCTIONS.register(bus);
        ReduxTreeDecorators.TREE_DECORATORS.register(bus);
        ReduxTrunkPlacers.TRUNK_PLACERS.register(bus);
        ReduxRootPlacers.ROOT_PLACERS.register(bus);
        ReduxLootConditions.LOOT_CONDITION_TYPES.register(bus);
        ReduxStructureTypes.STRUCTURE_TYPES.register(bus);
        ReduxConditionSources.CONDITIONS.register(bus);
        ReduxStateProviders.PROVIDERS.register(bus);
        ReduxBlocks.registerWoodTypes();
        ReduxBlocks.registerPots();
        MinecraftForge.EVENT_BUS.register(this);
        MinecraftForge.EVENT_BUS.register(MobSoundListener.class);

        ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, ReduxConfig.COMMON_SPEC);
        ModLoadingContext.get().registerConfig(ModConfig.Type.CLIENT, ReduxConfig.CLIENT_SPEC);
    }

    public static class Keys {
        public static final ResourceKey<Registry<Codec<? extends AbstractCondition<?>>>> CONDITION_SERIALIZER = ResourceKey.createRegistryKey(new ResourceLocation(Redux.MODID, "condition_serializer"));
    }


    public static class Handlers {

        public static class Wood {
            public static final WoodHandler FIELDSPROUT = WoodHandler.handler("fieldsprout", null, true, WoodHandler.cherrySoundBlockSet(), "trees", "log", "wood", SoundType.CHERRY_WOOD, SoundType.CHERRY_WOOD, false, MapColor.NETHER, MapColor.COLOR_ORANGE, false);
            public static final WoodHandler BLIGHTWILLOW = WoodHandler.handler("blightwillow", null, true, WoodHandler.bambooSoundBlockSet(), "trees", "log", "wood", SoundType.BAMBOO_WOOD, SoundType.BAMBOO_WOOD, false, MapColor.TERRACOTTA_CYAN, MapColor.COLOR_ORANGE, true);
            public static final WoodHandler CLOUDCAP = WoodHandler.fungus("cloudcap", false, MapColor.WOOL, MapColor.TERRACOTTA_PURPLE);
            public static final WoodHandler SPRINGSHROOM = WoodHandler.noStrippingFungus("springshroom", false, MapColor.COLOR_GRAY, MapColor.COLOR_GRAY);
            public static final WoodHandler CRYSTAL = WoodHandler.tree("crystal", false, MapColor.TERRACOTTA_CYAN, MapColor.COLOR_LIGHT_BLUE);
            public static final WoodHandler GLACIA = WoodHandler.tree("glacia", false, MapColor.TERRACOTTA_BLACK, MapColor.TERRACOTTA_LIGHT_GRAY);
            public static final WoodHandler[] WOOD_HANDLERS = new WoodHandler[] { CRYSTAL, BLIGHTWILLOW, GLACIA, FIELDSPROUT, CLOUDCAP, SPRINGSHROOM };
        }
}

    private void replaceBlockSounds() {
        Blocks.GLOWSTONE.soundType = ReduxSoundTypes.GLOWSTONE;
    }

    private void commonSetup(final FMLCommonSetupEvent event)
    {
        ReduxAdvancementTriggers.init();

        ReduxPlacementModifiers.init();
        ReduxPacketHandler.register();
        event.enqueueWork(() -> {
            ReduxBlocks.registerFlammability();
            registerDispenserBehaviors();
            replaceBlockSounds();
            Regions.register(new ReduxRegion(new ResourceLocation(MODID, "aether_redux_region"), ReduxConfig.COMMON.region_size.get()));
            SurfaceRuleManager.addSurfaceRules(AetherRuleCategory.THE_AETHER, "aether_redux", ReduxSurfaceData.makeRules());
            if (ReduxConfig.COMMON.better_mimics.get()) {
                AetherEntityTypes.MIMIC.get().getDimensions().height = 1.25F;
                if (aetherGenesisCompat())
                {
                    GenesisEntityTypes.SKYROOT_MIMIC.get().getDimensions().height = 1.25F;
                }
            }
        });
    }

    private void clientSetup(final FMLClientSetupEvent event)
    {
        ReduxRenderers.registerCuriosRenderers();
        event.enqueueWork(
                () -> {
                    if (ReduxConfig.CLIENT.change_aether_configs.get()) {
                        AetherConfig.CLIENT.green_sunset.set(true);
                        AetherConfig.CLIENT.colder_lightmap.set(true);
                        ReduxConfig.CLIENT.change_aether_configs.set(false);
                    }
                    ReduxClient.registerItemModelProperties();

                });
    }

    private void registerDispenserBehaviors() {
        DispenserBlock.registerBehavior(ReduxItems.BLIGHTED_SPORES.get(), new DispenseUsableItemBehavior<>(ReduxRecipeTypes.SPORE_BLIGHTING.get()));
    }

    /**
     * semi-arbitrarily chosen event soon before the resource packs are first reloaded in order to apply the overrides pack before it is done
     */
    public void applyResourcePack(RegisterKeyMappingsEvent event)
    {
        if (packConfig != null && packConfig.auto_apply.get()) {
            for (Pack pack : Minecraft.getInstance().getResourcePackRepository().getAvailablePacks()) {
                if (pack.getPackSource() == ReduxPackSources.AUTO_APPLY_RESOURCE) {
                    Minecraft.getInstance().getResourcePackRepository().addPack(pack.getId());
                }
            }
        }
    }

    public void registerRecipeSerializers(RegisterEvent event)
    {
        if (event.getRegistryKey().equals(ForgeRegistries.Keys.RECIPE_SERIALIZERS))
        {
            CraftingHelper.register(DataRecipeCondition.Serializer.INSTANCE);
        }
    }

    public void dataSetup(GatherDataEvent event) {
        DataGenerator generator = event.getGenerator();
        ExistingFileHelper fileHelper = event.getExistingFileHelper();
        CompletableFuture<HolderLookup.Provider> lookupProvider = event.getLookupProvider();
        PackOutput packOutput = generator.getPackOutput();
        generator.addProvider(event.includeServer(), new ReduxRegistrySets(packOutput, lookupProvider));
        generator.addProvider(event.includeClient(), new ReduxBlockstateData(packOutput, fileHelper));
        generator.addProvider(event.includeClient(), new ReduxItemModelData(packOutput, fileHelper));
        generator.addProvider(event.includeClient(), new ReduxLanguageData(packOutput));
        generator.addProvider(event.includeClient(), new ReduxSoundData(packOutput, fileHelper));
        generator.addProvider(event.includeServer(), new ReduxRecipeData(packOutput));
        generator.addProvider(event.includeServer(), ReduxLootData.loot(packOutput));
        generator.addProvider(event.includeServer(), new ReduxDamageTypeTagData(packOutput, lookupProvider, fileHelper));
        ReduxBlockTagsData block = new ReduxBlockTagsData(packOutput, lookupProvider, fileHelper);
        generator.addProvider(event.includeServer(), block);
        generator.addProvider(event.includeServer(), new ReduxItemTagsData(packOutput, lookupProvider, block.contentsGetter(), fileHelper));
        generator.addProvider(event.includeServer(), new ReduxBiomeTagsData(packOutput, lookupProvider, fileHelper));
        generator.addProvider(event.includeServer(), new ReduxEntityTypeTagData(packOutput, lookupProvider, fileHelper));
        generator.addProvider(event.includeServer(), new ReduxAdvancementData(packOutput, lookupProvider, fileHelper));
        generator.addProvider(event.includeServer(), new ReduxLootModifierData(packOutput));
        PackMetadataGenerator packMeta = new PackMetadataGenerator(packOutput);
        Map<PackType, Integer> packTypes = Map.of(PackType.SERVER_DATA, SharedConstants.getCurrentVersion().getPackVersion(PackType.SERVER_DATA));
        packMeta.add(PackMetadataSection.TYPE, new PackMetadataSection(Component.literal("Aether: Redux data/resources"), SharedConstants.getCurrentVersion().getPackVersion(PackType.CLIENT_RESOURCES), packTypes));
        generator.addProvider(true, packMeta);
    }

    public  void packSetup(AddPackFindersEvent event) {


        PackConfigBootstrap.bootstrap();

        overridesPack(event);

        if (aetherGenesisCompat()) { this.setupMandatoryDataPack(event, "data/genesis_data", "Redux - Genesis Tweaks", "Small tweaks to the Aether: Genesis"); }
        if (deepAetherCompat()) { this.setupMandatoryDataPack(event, "data/deep_aether_data", "Redux - Deep Aether Tweaks", "Small tweaks to the Deep Aether Addon"); }
        if (ancientAetherCompat()) {
            this.setupMandatoryDataPack(event, "data/ancient_aether_data", "Redux - Ancient Aether Tweaks", "Small tweaks to the Ancient Aether Addon");
        }
        this.setupBuiltinDatapack(event, "data/cloudbed", "Redux - Cloudbed", "Highlands-like Cloudbed");

    }


    public static void overridesPack(AddPackFindersEvent event) {
        if (event.getPackType() == PackType.CLIENT_RESOURCES) {

            if (packConfig != null) {
                String id = "overrides_pack";
                String title = "pack.aether_redux.overrides.title";
                Component desc = Component.translatable("pack.aether_redux.overrides.description");
                ReduxOverridesPackResources combined = ReduxPackConfig.generate(id, title, desc);
                BuiltinPackUtils.autoApply(event, combined, id, title, desc);
            }

        }
    }

    private void setupMandatoryPack(AddPackFindersEvent event, String path, String displayName, String desc) {
        if (event.getPackType() == PackType.CLIENT_RESOURCES) {
            Path resourcePath = ModList.get().getModFileById(MODID).getFile().findResource("packs/" + path);
            PathPackResources pack = new PathPackResources(ModList.get().getModFileById(MODID).getFile().getFileName() + ":" + resourcePath, true, resourcePath);
            PackMetadataSection metadata = new PackMetadataSection(Component.translatable(desc)
                    , SharedConstants.getCurrentVersion().getPackVersion(PackType.CLIENT_RESOURCES));
            event.addRepositorySource((packConsumer) -> {
                packConsumer.accept(Pack.create("builtin/" + path, Component.literal("Redux - " + displayName),
                        true,
                        (string) -> pack,
                        new Pack.Info(metadata.getDescription(), metadata.getPackFormat(PackType.SERVER_DATA), metadata.getPackFormat(PackType.CLIENT_RESOURCES), FeatureFlagSet.of(), pack.isHidden()),
                        PackType.CLIENT_RESOURCES,
                        Pack.Position.TOP,
                        false,
                        PackSource.BUILT_IN));
            });
        }
    }

    private void setupOptionalPack(AddPackFindersEvent event, String path, String displayName, String desc) {
        if (event.getPackType() == PackType.CLIENT_RESOURCES) {
            Path resourcePath = ModList.get().getModFileById(MODID).getFile().findResource("packs/" + path);
            PathPackResources pack = new PathPackResources(ModList.get().getModFileById(MODID).getFile().getFileName() + ":" + resourcePath, true, resourcePath);
            PackMetadataSection metadata = new PackMetadataSection(Component.translatable(desc)
                    , SharedConstants.getCurrentVersion().getPackVersion(PackType.CLIENT_RESOURCES));
            event.addRepositorySource((packConsumer) -> {
                packConsumer.accept(Pack.create("builtin/" + path, Component.literal("Redux - " + displayName),
                        false,
                        (string) -> pack,
                        new Pack.Info(metadata.getDescription(), metadata.getPackFormat(PackType.SERVER_DATA), metadata.getPackFormat(PackType.CLIENT_RESOURCES), FeatureFlagSet.of(), pack.isHidden()),
                        PackType.CLIENT_RESOURCES,
                        Pack.Position.TOP,
                        false,
                        PackSource.BUILT_IN));
            });
        }
    }
    private void setupMandatoryDataPack(AddPackFindersEvent event, String path, String displayName, String desc) {
        if (event.getPackType() == PackType.SERVER_DATA) {
            Path resourcePath = ModList.get().getModFileById(MODID).getFile().findResource("packs/" + path);
            PathPackResources pack = new PathPackResources(ModList.get().getModFileById(MODID).getFile().getFileName() + ":" + resourcePath, true, resourcePath);
            PackMetadataSection metadata = new PackMetadataSection(Component.translatable(desc)
                    , SharedConstants.getCurrentVersion().getPackVersion(PackType.SERVER_DATA));
            event.addRepositorySource((packConsumer) -> {
                packConsumer.accept(Pack.create("builtin/" + path, Component.literal("Redux - " + displayName),
                        true,
                        (string) -> pack,
                        new Pack.Info(metadata.getDescription(), metadata.getPackFormat(PackType.SERVER_DATA), metadata.getPackFormat(PackType.CLIENT_RESOURCES), FeatureFlagSet.of(), pack.isHidden()),
                        PackType.SERVER_DATA,
                        Pack.Position.TOP,
                        false,
                        PackSource.BUILT_IN));
            });
        }

    }
    private void setupBuiltinDatapack(AddPackFindersEvent event, String path, String displayName, String desc) {
        setupDatapack(event, path, displayName, desc, PackSource.BUILT_IN);
    }
    private void setupFeatureDatapack(AddPackFindersEvent event, String path, String displayName, String desc) {
        setupDatapack(event, path, displayName, desc, PackSource.FEATURE);
    }
    private void setupOptionalDatapack(AddPackFindersEvent event, String path, String displayName, String desc) {
        setupDatapack(event, path, displayName, desc, ReduxPackSources.OPTIONAL_DATAPACK);
    }
    private void setupDatapack(AddPackFindersEvent event, String path, String displayName, String desc, PackSource source) {
        if (event.getPackType() == PackType.SERVER_DATA) {
            Path resourcePath = ModList.get().getModFileById(MODID).getFile().findResource("packs/" + path);
            PathPackResources pack = new PathPackResources(ModList.get().getModFileById(MODID).getFile().getFileName() + ":" + resourcePath, true, resourcePath);
            PackMetadataSection metadata = new PackMetadataSection(Component.translatable(desc)
                    , SharedConstants.getCurrentVersion().getPackVersion(PackType.SERVER_DATA));
            event.addRepositorySource((packConsumer) -> {
                packConsumer.accept(Pack.create("builtin/" + path, Component.literal("Redux - " + displayName),
                        false,
                        (string) -> pack,
                        new Pack.Info(metadata.getDescription(), metadata.getPackFormat(PackType.SERVER_DATA), metadata.getPackFormat(PackType.CLIENT_RESOURCES), FeatureFlagSet.of(), pack.isHidden()),
                        PackType.SERVER_DATA,
                        Pack.Position.TOP,
                        false,
                        source));
            });
        }
    }
    private void setupDatapackFeatureFlags(AddPackFindersEvent event, String path, String displayName, String desc, PackSource source, FeatureFlag flag, FeatureFlag... otherFlags) {
        if (event.getPackType() == PackType.SERVER_DATA) {
            Path resourcePath = ModList.get().getModFileById(MODID).getFile().findResource("packs/" + path);
            PathPackResources pack = new PathPackResources(ModList.get().getModFileById(MODID).getFile().getFileName() + ":" + resourcePath, true, resourcePath);
            PackMetadataSection metadata = new PackMetadataSection(Component.translatable(desc)
                    , SharedConstants.getCurrentVersion().getPackVersion(PackType.SERVER_DATA));
            event.addRepositorySource((packConsumer) -> {
                packConsumer.accept(Pack.create("builtin/" + path, Component.literal("Redux - " + displayName),
                        false,
                        (string) -> pack,
                        new Pack.Info(metadata.getDescription(), metadata.getPackFormat(PackType.SERVER_DATA), metadata.getPackFormat(PackType.CLIENT_RESOURCES), FeatureFlagSet.of(flag, otherFlags), pack.isHidden()),
                        PackType.SERVER_DATA,
                        Pack.Position.TOP,
                        false,
                        source));
            });
        }
    }
    public static ResourceLocation locate(String name) {
        return new ResourceLocation(MODID, name);
    }
    public static boolean deepAetherCompat() { return ModList.get().isLoaded("deep_aether"); }
    public static boolean lostAetherCompat() { return ModList.get().isLoaded("lost_aether_content"); }
    public static boolean aetherGenesisCompat() { return ModList.get().isLoaded("aether_genesis"); }
    public static boolean ancientAetherCompat() { return ModList.get().isLoaded("ancient_aether"); }


}
