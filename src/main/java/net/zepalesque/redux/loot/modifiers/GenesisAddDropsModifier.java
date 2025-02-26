package net.zepalesque.redux.loot.modifiers;

import com.google.gson.JsonElement;
import com.google.gson.JsonSyntaxException;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.Dynamic;
import com.mojang.serialization.JsonOps;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.functions.LootItemFunction;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import net.minecraftforge.common.loot.IGlobalLootModifier;
import net.minecraftforge.common.loot.LootModifier;
import net.minecraftforge.common.loot.LootModifierManager;

public class GenesisAddDropsModifier extends LootModifier {
    private static final Codec<LootItemFunction[]> LOOT_FUNCTIONS_CODEC = Codec.PASSTHROUGH.flatXmap(
            d -> {
                try {
                    LootItemFunction[] functions = LootModifierManager.GSON_INSTANCE.fromJson(getJson(d), LootItemFunction[].class);
                    return DataResult.success(functions);
                } catch (JsonSyntaxException e) {
                    LootModifierManager.LOGGER.warn("Unable to decode loot functions", e);
                    return DataResult.error(e::getMessage);
                }
            }, functions -> {
                try {
                    JsonElement element = LootModifierManager.GSON_INSTANCE.toJsonTree(functions);
                    return DataResult.success(new Dynamic<>(JsonOps.INSTANCE, element));
                } catch (JsonSyntaxException e) {
                    LootModifierManager.LOGGER.warn("Unable to encode loot functions", e);
                    return DataResult.error(e::getMessage);
                }
            }
    );
    public static final Codec<GenesisAddDropsModifier> CODEC = RecordCodecBuilder.create((instance) -> instance.group(
            ItemStack.CODEC.fieldOf("item").forGetter(modifier -> modifier.itemStack),
            GenesisAddDropsModifier.LOOT_FUNCTIONS_CODEC.fieldOf("functions").forGetter(modifier -> modifier.functions),
            LootModifier.LOOT_CONDITIONS_CODEC.fieldOf("conditions").forGetter(modifier -> modifier.conditions)
    ).apply(instance, GenesisAddDropsModifier::new));
    private final LootItemFunction[] functions;
    private final ItemStack itemStack;

    public GenesisAddDropsModifier(ItemStack itemStack, LootItemFunction[] functions, LootItemCondition[] conditions) {
        super(conditions);
        this.functions = functions;
        this.itemStack = itemStack.copy();
    }

    @Override
    public ObjectArrayList<ItemStack> doApply(ObjectArrayList<ItemStack> lootStacks, LootContext context) {
        ItemStack i = this.itemStack;
        for (LootItemFunction function : this.functions) {
            i = function.apply(this.itemStack, context);
        }
        lootStacks.add(i);
        return lootStacks;
    }

    @Override
    public Codec<? extends IGlobalLootModifier> codec() {
        return GenesisAddDropsModifier.CODEC;
    }

    @SuppressWarnings("unchecked")
    static <U> JsonElement getJson(Dynamic<?> dynamic) {
        Dynamic<U> typed = (Dynamic<U>) dynamic;
        return typed.getValue() instanceof JsonElement ? (JsonElement) typed.getValue() : typed.getOps().convertTo(JsonOps.INSTANCE, typed.getValue());
    }
}