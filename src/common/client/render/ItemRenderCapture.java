package factorization.client.render;

import net.minecraft.item.ItemStack;
import net.minecraftforge.client.IItemRenderer;

public class ItemRenderCapture implements IItemRenderer {
    private static ItemStack rendering;
    private static ItemRenderType renderType;
    
    public static ItemStack getRenderingItem() {
        ItemStack ret = rendering;
        rendering = null;
        return ret;
    }
    
    public static ItemRenderType getRenderType() {
        return renderType;
    }
    
    @Override
    public boolean handleRenderType(ItemStack item, ItemRenderType type) {
        rendering = item;
        renderType = type;
        return false;
    }

    @Override
    public boolean shouldUseRenderHelper(ItemRenderType type, ItemStack item, ItemRendererHelper helper) {
        rendering = item;
        renderType = type;
        return false;
    }

    @Override
    public void renderItem(ItemRenderType type, ItemStack item, Object... data) { }

}
