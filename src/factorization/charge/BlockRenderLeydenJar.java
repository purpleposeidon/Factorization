package factorization.charge;



import java.util.WeakHashMap;

import org.lwjgl.opengl.GL11;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderBlocks;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Icon;
import net.minecraftforge.client.IItemRenderer.ItemRenderType;
import factorization.common.BlockIcons;
import factorization.common.BlockRenderHelper;
import factorization.common.FactoryType;
import factorization.shared.FactorizationBlockRender;

public class BlockRenderLeydenJar extends FactorizationBlockRender {

    @Override
    public void render(RenderBlocks rb) {
        Icon glass = BlockIcons.leyden_glass_side;
        Icon knob = BlockIcons.leyden_knob;
        BlockRenderHelper block = BlockRenderHelper.instance;
        float inset = 1F/16F;
        float jarHeight = 16F/16F; //13
        float metal_height = 5F/16F;
        float knob_in = 5F/16F;
        float knob_height = 3F/16F;
        float post_in = 7F/16F;
        float dz = 1F/64F;
        
        block.setBlockBounds(inset, 0, inset, 1 - inset, jarHeight, 1 - inset);
        block.useTextures(null, BlockIcons.leyden_glass, glass, glass, glass, glass);
        renderBlock(rb, block);
        
        block.useTexture(knob);
        float d = 5F/16F;
        //block.setBlockBoundsOffset(d, 0, d);
        block.setBlockBounds(knob_in, 0 + 1F/64F, knob_in, 1 - knob_in, knob_height, 1 - knob_in);
        if (!world_mode) {
            GL11.glTranslatef(0, 1, 0);
            renderBlock(rb, block);
            GL11.glTranslatef(0, -1, 0);
        } else {
            y++;
            if (!w.isBlockOpaqueCube(x, y, z)) {
                renderBlock(rb, block);
            }
            y--;
        }
        
        
        Icon leyden_metal = BlockIcons.leyden_metal;
        renderCauldron(rb, BlockIcons.leyden_rim, leyden_metal, metal_height);
        block.useTextures(null, null, leyden_metal, leyden_metal, leyden_metal, leyden_metal);
        block.setBlockBounds(post_in, 1F/16F, post_in, 1 - post_in, jarHeight, 1 - post_in);
        renderBlock(rb, block);
        if (!world_mode && is != null && is.hasTagCompound()) {
            TileEntityLeydenJar jar = (TileEntityLeydenJar) FactoryType.LEYDENJAR.getRepresentative();
            jar.onPlacedBy(null, is, 0);
            ChargeSparks sparks;
            if (!sparkMap.containsKey(is)) {
                sparks = new ChargeSparks(10);
                sparkMap.put(is, sparks);
            } else {
                sparks = sparkMap.get(is);
            }
            Minecraft mc = Minecraft.getMinecraft();
            if (mc.theWorld == null) {
                jar.updateSparks(sparks);
            } else {
                long now = mc.theWorld.getTotalWorldTime();
                if (now != sparks.last_update) {
                    sparks.last_update = now;
                    jar.updateSparks(sparks);
                }
            }
            if (renderType == ItemRenderType.ENTITY || renderType == ItemRenderType.EQUIPPED || renderType == ItemRenderType.EQUIPPED_FIRST_PERSON) {
                float t = -0.5F;
                GL11.glTranslatef(t, t, t);
                sparks.render();
                t *= -1;
                GL11.glTranslatef(t, t, t);
            } else {
                sparks.render();
            }
        }
    }
    
    private WeakHashMap<ItemStack, ChargeSparks> sparkMap = new WeakHashMap<ItemStack, ChargeSparks>();

    @Override
    public FactoryType getFactoryType() {
        return FactoryType.LEYDENJAR;
    }

}
