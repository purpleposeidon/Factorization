package factorization.charge;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderBlocks;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.util.IIcon;
import net.minecraftforge.client.IItemRenderer.ItemRenderType;

import org.lwjgl.opengl.GL11;

import factorization.api.Coord;
import factorization.common.BlockIcons;
import factorization.common.FactoryType;
import factorization.shared.BlockRenderHelper;
import factorization.shared.Core;
import factorization.shared.FactorizationBlockRender;


public class BlockRenderBattery extends FactorizationBlockRender {
    float item_fullness = 0;
    @Override
    public boolean render(RenderBlocks rb) {
        TileEntityBattery bat;
        if (world_mode) {
            bat = (TileEntityBattery) te;
        } else {
            bat = (TileEntityBattery) FactoryType.BATTERY.getRepresentative();
            if (is != null && is.hasTagCompound()) {
                bat.loadFromStack(is);
            }
        }
        if (bat == null) {
            return false;
        }
        renderBatteryDisplay(rb, bat.getFullness());
        renderNormalBlock(rb, FactoryType.BATTERY.md);
        return true;
    }
    
    void renderInventoryMode(RenderBlocks rb, ItemRenderType type) {
        Minecraft mc = Minecraft.getMinecraft();
        mc.renderEngine.bindTexture(Core.blockAtlas);
        renderNormalBlock(rb, FactoryType.BATTERY.md);
        GL11.glPushMatrix();
        renderBatteryDisplay(rb, item_fullness);
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
        final float d = 1.0F / 128.0F;
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
        //IIcon meter = BlockIcons.battery_meter;
        IIcon meter = BlockIcons.battery_meter;
        block.useTextures(null, null, meter, meter, meter, meter);
        block.setBlockBounds(0 - d, 0, 0 - d, 1 + d, h, 1 + d);
        if (world_mode) {
            block.render(rb, x, y, z);
        } else {
            block.renderForInventory(rb);
        }
    }


    @Override
    public FactoryType getFactoryType() {
        return FactoryType.BATTERY;
    }
}
