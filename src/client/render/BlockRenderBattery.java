package factorization.client.render;

import org.lwjgl.opengl.GL11;

import net.minecraft.client.Minecraft;
import net.minecraft.src.ModLoader;
import net.minecraft.src.RenderBlocks;
import net.minecraft.src.RenderEngine;
import net.minecraft.src.Tessellator;
import net.minecraftforge.client.IItemRenderer.ItemRenderType;
import static net.minecraftforge.client.IItemRenderer.ItemRenderType.*;
import factorization.api.Coord;
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
        re.bindTexture(re.getTexture(Core.texture_file_block));
        renderBatteryDisplay(rb, item_fullness);
        Tessellator.instance.draw();
        GL11.glPopMatrix();
    }

    
    void renderBatteryDisplay(RenderBlocks rb, float fullness) {
        fullness = Math.min(fullness, 1);
        int icon = (6) + 16 * 1;
        Tessellator tes = Tessellator.instance;
        double u = ((icon & 0xf) << 4) / 256.0;
        double v = (icon & 0xf0) / 256.0;

        double uw = u + 16F / 256F; // u + uvd;
        double vw = v + 16F / 256F; //v + uvd;

        float pixels = Math.round(fullness * 11);
        float h = 1F / 16F + pixels / 16F;
        v += (4 + 11 - pixels) / 256F;
        final double d = 1.0 / 128.0;
        int brightness;
        if (world_mode) {
            brightness = Core.registry.factory_block.getMixedBrightnessForBlock(ModLoader.getMinecraftInstance().theWorld, x, y, z);
        } else {
            brightness = 12583104;
        }
        tes.setBrightness(brightness);
        float color = Math.min(1, fullness * .8F + 0.2F);
        tes.setColorOpaque_F(color, fullness, fullness);
        tes.addVertexWithUV(x, y, z - d, uw, vw);
        tes.addVertexWithUV(x, y + h, z - d, uw, v);
        tes.addVertexWithUV(x + 1, y + h, z - d, u, v);
        tes.addVertexWithUV(x + 1, y, z - d, u, vw);

        tes.addVertexWithUV(x + 1, y, z + 1 + d, uw, vw);
        tes.addVertexWithUV(x + 1, y + h, z + 1 + d, uw, v);
        tes.addVertexWithUV(x, y + h, z + 1 + d, u, v);
        tes.addVertexWithUV(x, y, z + 1 + d, u, vw);

        tes.addVertexWithUV(x - d, y, z + 1, u, vw);
        tes.addVertexWithUV(x - d, y + h, z + 1, u, v);
        tes.addVertexWithUV(x - d, y + h, z, uw, v);
        tes.addVertexWithUV(x - d, y, z, uw, vw);

        tes.addVertexWithUV(x + 1 + d, y, z, uw, vw);
        tes.addVertexWithUV(x + 1 + d, y + h, z, uw, v);
        tes.addVertexWithUV(x + 1 + d, y + h, z + 1, u, v);
        tes.addVertexWithUV(x + 1 + d, y, z + 1, u, vw);

    }


    @Override
    FactoryType getFactoryType() {
        return FactoryType.BATTERY;
    }
}
