package factorization.client.render;

import net.minecraft.item.ItemStack;
import net.minecraftforge.client.IItemRenderer;

public class ItemRenderCapture implements IItemRenderer {
    private static ItemStack rendering;
    
    public static ItemStack getRenderingItem() {
        ItemStack ret = rendering;
        rendering = null;
        return ret;
    }
    
    @Override
    public boolean handleRenderType(ItemStack item, ItemRenderType type) {
        rendering = item;
        return false;
    }

    @Override
    public boolean shouldUseRenderHelper(ItemRenderType type, ItemStack item, ItemRendererHelper helper) {
        rendering = item;
        return false;
    }

    @Override
    public void renderItem(ItemRenderType type, ItemStack item, Object... data) { }

}
