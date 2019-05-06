package io.github.kvverti.bannerpp.mixin;

import io.github.kvverti.bannerpp.api.LoomPattern;

import net.minecraft.block.entity.BannerPattern;
import net.minecraft.container.Container;
import net.minecraft.container.ContainerType;
import net.minecraft.container.LoomContainer;
import net.minecraft.container.Property;
import net.minecraft.container.Slot;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.inventory.Inventory;
import net.minecraft.item.BannerPatternItem;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompoundTag;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

/**
 * Overwrites LoomContainer methods to correctly determine dye banner patterms.
 */
@Mixin(LoomContainer.class)
public abstract class MixinLoomContainer extends Container {

    // unused
    private MixinLoomContainer(ContainerType<?> ct, int i) {
        super(ct, i);
    }

    // shadow members

    @Shadow private Property selectedPattern;
    @Shadow private Slot bannerSlot;
    @Shadow private Slot dyeSlot;
    @Shadow private Slot patternSlot;
    @Shadow private Slot outputSlot;

    @Shadow private void updateOutputSlot() {}

    // mixin members

    private static final BannerPattern[] patterns = BannerPattern.values();

    /**
     * Sets the output slot based on which background index the client clicks.
     */
    @Overwrite
    public boolean onButtonClick(PlayerEntity player, int patternIdxPlus1) {
        int patternIdx = patternIdxPlus1 - 1;
        // replace conditional to check for dye pattern index instead of
        // pattern ID
        if(patternIdx >= 0 && patternIdx < LoomPattern.RECIPE_PATTERNS.size()) {
            this.selectedPattern.set(LoomPattern.RECIPE_PATTERNS.get(patternIdx).ordinal());
            this.updateOutputSlot();
            return true;
        } else {
            return false;
        }
    }

    /**
     * Determines the selected pattern and output slot on content change.
     */
    @Overwrite
    public void onContentChanged(Inventory inv) {
        ItemStack banner = this.bannerSlot.getStack();
        ItemStack dye = this.dyeSlot.getStack();
        ItemStack pattern = this.patternSlot.getStack();
        ItemStack output = this.outputSlot.getStack();
        // replace comparing pattern ID to a hardcoded limit with directly
        // querying the desired property (added via mixin)
        boolean selectedNeedsItem = ((LoomPattern)(Object)patterns[this.selectedPattern.get()]).requiresPatternItem();
        if (output.isEmpty() || !banner.isEmpty() && !dye.isEmpty() && this.selectedPattern.get() > 0 && (!selectedNeedsItem || !pattern.isEmpty())) {
            if (!pattern.isEmpty() && pattern.getItem() instanceof BannerPatternItem) {
                CompoundTag blockEntityTag = banner.getOrCreateSubCompoundTag("BlockEntityTag");
                boolean bannerIsFull = blockEntityTag.containsKey("Patterns", 9) && !banner.isEmpty() && blockEntityTag.getList("Patterns", 10).size() >= 6;
                if (bannerIsFull) {
                    this.selectedPattern.set(0);
                } else {
                    this.selectedPattern.set(((BannerPatternItem)pattern.getItem()).getPattern().ordinal());
                }
            }
        } else {
            this.outputSlot.setStack(ItemStack.EMPTY);
            this.selectedPattern.set(0);
        }

        this.updateOutputSlot();
        this.sendContentUpdates();
    }
}