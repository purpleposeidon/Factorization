package factorization.client.render;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderBlocks;
import net.minecraft.client.renderer.RenderEngine;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.util.Icon;
import net.minecraftforge.client.IItemRenderer.ItemRenderType;

import org.lwjgl.opengl.GL11;

import factorization.api.Coord;
import factorization.common.BlockIcons;
import factorization.common.BlockRenderHelper;
import factorization.common.Core;
import factorization.common.FactoryType;
import factorization.common.TileEntityBattery;


public class BlockRenderBattery extends FactorizationBlockRender {
    float item_fullness = 0;
    @Override
    void render(RenderBlocks rb) {
        if (world_mode) {
            TileEntityBattery bat = new Coord(Minecraft.getMinecraft().theWorld, x, y, z).getTE(TileEntityBattery.class);
            if (bat == null) {
                return;
            }
            renderBatteryDisplay(rb, bat.getFullness());
            renderNormalBlock(rb, FactoryType.BATTERY.md);
        }
    }
    
    void renderInventoryMode(RenderBlocks rb, ItemRenderType type) {
        renderNormalBlock(rb, FactoryType.BATTERY.md);
        GL11.glPushMatrix();
        Tessellator.instance.startDrawingQuads();
        //if (type == EQUIPPED || type == ENTITY) {
        if (true) {
            GL11.glTranslatef(-0.5F, -0.5F, -0.5F);
        }
        RenderEngine re = Minecraft.getMinecraft().renderEngine;
        re.bindTextureFile(Core.texture_file_block);
        renderBatteryDisplay(rb, item_fullness);
        Tessellator.instance.draw();
        GL11.glPopMatrix();
    }

    
    void renderBatteryDisplay(RenderBlocks rb, float fullness) {
        fullness = Math.min(fullness, 1);
        Tessellator tes = Tessellator.instance;

        float pixels = Math.round(fullness * 11);
        if (pixels < 1) {
            return;
        }
        float h = 1F / 16F + pixels / 16F;
        final double d = 1.0 / 128.0;
        int brightness;
        if (world_mode) {
            brightness = Core.registry.factory_block.getMixedBrightnessForBlock(getCoord().w, x, y, z);
        } else {
            brightness = 0xc000c0;
        }
        tes.setBrightness(brightness);
        float color = Math.min(1, fullness * .8F + 0.2F);
        tes.setColorOpaque_F(color, fullness, fullness);
        
        BlockRenderHelper block = BlockRenderHelper.instance;
        Icon meter = BlockIcons.battery_meter;
        block.useTextures(null, null, meter, meter, meter, meter);
        block.setBlockBounds(0, 0, 0, 1, h, 1);
        if (world_mode) {
            block.render(x, y, z);
        } else {
            block.renderForTileEntity();
        }
    }


    @Override
    FactoryType getFactoryType() {
        return FactoryType.BATTERY;
    }
}
