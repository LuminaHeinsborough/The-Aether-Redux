package net.zepalesque.redux.mixin.client.block;

import com.aetherteam.aether.block.AetherBlocks;
import com.aetherteam.aether.block.natural.AetherDoubleDropsLeaves;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.util.ParticleUtils;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.zepalesque.redux.Redux;
import net.zepalesque.redux.block.ReduxBlocks;
import net.zepalesque.redux.client.particle.ReduxParticleTypes;
import net.zepalesque.redux.config.ReduxConfig;
import net.zepalesque.redux.misc.ReduxTags;
import net.zepalesque.redux.util.compat.DeepAetherParticleUtil;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.function.Supplier;

@Mixin(AetherDoubleDropsLeaves.class)
public class AetherLeavesClientMixin extends LeafBlockClientMixin {

    @Override
    protected void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random, CallbackInfo ci) {
        super.animateTick(state, level, pos, random, ci);

        if (level.isClientSide()) {
            @Nullable Supplier<? extends ParticleOptions> supplier = getParticle((AetherDoubleDropsLeaves) (Object) this);
            int chance = level.getBiome(pos).is(ReduxTags.Biomes.DENSE_LEAF_FALL) ? 20 : 30;
            if (supplier != null && random.nextInt(chance) == 0) {
                BlockPos blockpos = pos.below();
                BlockState blockstate = level.getBlockState(blockpos);
                if (!blockstate.canOcclude() || !blockstate.isFaceSturdy(level, blockpos, Direction.UP)) {
                    ParticleUtils.spawnParticleBelow(level, pos, random, supplier.get());
                }
            }
        }
    }

    private static @Nullable Supplier<? extends ParticleOptions> getParticle(Block self)
    {
        if (ReduxConfig.CLIENT.better_leaf_particles.get()) {
            @Nullable Supplier<? extends ParticleOptions> particle = null;
            if (Redux.deepAetherCompat()) {
                particle = DeepAetherParticleUtil.getParticle(self);
            }
            if (self == AetherBlocks.SKYROOT_LEAVES.get()) { particle = ReduxParticleTypes.FALLING_SKYROOT_LEAVES; }
            if (self == ReduxBlocks.BLIGHTWILLOW_LEAVES.get()) { particle = ReduxParticleTypes.FALLING_BLIGHTWILLOW_LEAVES; }
            if (self == ReduxBlocks.GLACIA_LEAVES.get()) { particle = ReduxParticleTypes.FALLING_GLACIA_NEEDLES; }
            if (self == ReduxBlocks.PURPLE_GLACIA_LEAVES.get()) { particle = ReduxParticleTypes.FALLING_PURPLE_GLACIA_NEEDLES; }
            if (self == ReduxBlocks.BLIGHTED_SKYROOT_LEAVES.get()) { particle = ReduxParticleTypes.FALLING_BLIGHTED_SKYROOT_LEAVES; }
            return particle;
        } else {
            return null;
        }

    }
    
}
