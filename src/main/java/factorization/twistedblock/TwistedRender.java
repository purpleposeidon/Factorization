package factorization.twistedblock;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderBlocks;
import net.minecraft.client.renderer.entity.RenderItem;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.item.ItemStack;
import net.minecraftforge.client.IItemRenderer;

import org.lwjgl.opengl.GL11;

import factorization.common.BlockIcons;
import factorization.shared.BlockRenderHelper;
import factorization.shared.Core;

public class TwistedRender implements IItemRenderer {
    
    @Override
    public boolean handleRenderType(ItemStack item, ItemRenderType type) {
        return type == ItemRenderType.EQUIPPED || type == ItemRenderType.EQUIPPED_FIRST_PERSON || type == ItemRenderType.INVENTORY || type == ItemRenderType.ENTITY;
    }

    @Override
    public boolean shouldUseRenderHelper(ItemRenderType type, ItemStack item, ItemRendererHelper helper) {
        if (type == ItemRenderType.INVENTORY) return false;
        if (type == ItemRenderType.ENTITY) return false;
        return true;
    }
    final ItemStack obs = Core.registry.dark_iron_block_item;
    final Minecraft mc = Minecraft.getMinecraft();
    final RenderItem ri = new RenderItem();
    
    @Override
    public void renderItem(ItemRenderType type, ItemStack item, Object... data) {
        GL11.glPushMatrix();
        double r = 45;
        if (mc.theWorld != null) {
            r = mc.theWorld.getTotalWorldTime() * 5;
        }
        if (type == ItemRenderType.EQUIPPED_FIRST_PERSON || type == ItemRenderType.EQUIPPED) {
            GL11.glRotated(r, 1, 1, 1);
            //RenderBlocks rb = (RenderBlocks) data[0];
            EntityLivingBase player = (EntityLivingBase) data[1];
            mc.entityRenderer.itemRenderer.renderItem(player, obs, 0, type);
        } else if (type == ItemRenderType.INVENTORY) {
            GL11.glRotated(r, 1, 1, 1);
            //RenderBlocks rb = (RenderBlocks) data[0];
            ri.renderItemIntoGUI(null, mc.renderEngine, obs, 0, 0);
        } else if (type == ItemRenderType.ENTITY) {
            GL11.glRotated(r, 0, 1, 0);
            GL11.glRotated(r, 1, 0, 0);
            mc.renderEngine.bindTexture(Core.blockAtlas);
            BlockRenderHelper block = BlockRenderHelper.instance;
            block.useTexture(BlockIcons.dark_iron_block);
            block.setBlockBounds(0, 0, 0, 1, 1, 1);
            RenderBlocks rb = (RenderBlocks) data[0];
            block.renderForInventory(rb);
        }
        GL11.glPopMatrix();
    }

}
