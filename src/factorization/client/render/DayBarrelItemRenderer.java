package factorization.client.render;

import net.minecraft.client.renderer.RenderBlocks;
import net.minecraft.item.ItemStack;
import net.minecraftforge.client.IItemRenderer;

import org.lwjgl.opengl.GL11;

public class DayBarrelItemRenderer implements IItemRenderer {
    BlockRenderDayBarrel render_barrel;
    public DayBarrelItemRenderer(BlockRenderDayBarrel render) {
        this.render_barrel = render;
    }
    @Override
    public boolean handleRenderType(ItemStack item, ItemRenderType type) {
        return true;
    }

    @Override
    public boolean shouldUseRenderHelper(ItemRenderType type, ItemStack item, ItemRendererHelper helper) {
        return true;
    }

    @Override
    public void renderItem(ItemRenderType type, ItemStack is, Object... data) {
        if (type == ItemRenderType.FIRST_PERSON_MAP) {
            return;
        }
        GL11.glPushMatrix();
        if (type == ItemRenderType.ENTITY) {
            GL11.glScalef(0.5F, 0.5F, 0.5F);
        }
        if (type == ItemRenderType.EQUIPPED || type == ItemRenderType.EQUIPPED_FIRST_PERSON) {
            GL11.glTranslatef(0.5F, 0.5F, 0.5F);
        }
        render_barrel.renderInInventory();
        render_barrel.is = is;
        render_barrel.renderType = type;
        render_barrel.render((RenderBlocks)data[0]);
        GL11.glPopMatrix();
    }

}
