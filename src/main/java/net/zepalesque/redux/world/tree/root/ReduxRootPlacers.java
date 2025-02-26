package net.zepalesque.redux.world.tree.root;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.levelgen.feature.rootplacers.RootPlacerType;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;
import net.zepalesque.redux.Redux;

public class ReduxRootPlacers {

    public static final DeferredRegister<RootPlacerType<?>> ROOT_PLACERS = DeferredRegister.create(Registries.ROOT_PLACER_TYPE, Redux.MODID);
    public static final RegistryObject<RootPlacerType<BlightwillowRootPlacer>> BLIGHTWILLOW_ROOT_PLACER = ROOT_PLACERS.register("blightwillow_root_placer", () -> new RootPlacerType<>(BlightwillowRootPlacer.CODEC));

}
