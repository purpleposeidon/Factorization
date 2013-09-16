package factorization.client.render;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderBlocks;
import net.minecraft.client.renderer.tileentity.TileEntityRenderer;
import net.minecraft.client.renderer.tileentity.TileEntitySpecialRenderer;
import net.minecraft.item.ItemStack;
import net.minecraftforge.client.IItemRenderer;

import org.lwjgl.opengl.GL11;

import factorization.common.FactoryType;
import factorization.common.TileEntityCommon;
import factorization.common.TileEntityDayBarrel;

public class DayBarrelItemRenderer implements IItemRenderer {
    BlockRenderDayBarrel render_barrel;
    TileEntitySpecialRenderer tesr = null;
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
        RenderBlocks rb = Minecraft.getMinecraft().renderGlobal.globalRenderBlocks;
        render_barrel.render(rb);
        ItemStack silk = TileEntityDayBarrel.getSilkedItem(is);
        if (silk != null) {
            if (tesr == null) {
                tesr = TileEntityRenderer.instance.getSpecialRendererForClass(TileEntityDayBarrel.class);
            }
            TileEntityCommon tec = FactoryType.DAYBARREL.getRepresentative();
            GL11.glRotatef(90, 0, 0, -1);
            GL11.glRotatef(90, 0, 1, 0);
            GL11.glTranslated(-0.5, -0.5, -0.5);
            tesr.renderTileEntityAt(tec, 0, 0, 0, 0);
        }
        GL11.glPopMatrix();
    }

}
