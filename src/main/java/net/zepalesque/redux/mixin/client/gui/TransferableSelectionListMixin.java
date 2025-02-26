package net.zepalesque.redux.mixin.client.gui;

import net.minecraft.client.gui.screens.packs.PackSelectionScreen;
import net.minecraft.client.gui.screens.packs.TransferableSelectionList;
import net.zepalesque.redux.api.MixinMenuStorage;
import net.zepalesque.redux.client.gui.screen.PackConfigMenu;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;

@Mixin(TransferableSelectionList.class)
public class TransferableSelectionListMixin implements MixinMenuStorage {

    @Shadow @Final
    PackSelectionScreen screen;
    @Unique
    @Nullable
    public PackConfigMenu menu;


    @Override
    public @Nullable PackConfigMenu getMenu() {
        return this.menu;
    }

    @Override
    public void setMenu(PackConfigMenu menu) {
        this.menu = menu;
        ((MixinMenuStorage) this.screen).setMenu(this.menu);
    }
}
