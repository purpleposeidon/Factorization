package factorization.client.render;

import org.lwjgl.opengl.GL11;

import net.minecraft.client.Minecraft;
import net.minecraft.src.ItemStack;
import net.minecraft.src.NBTTagCompound;
import net.minecraft.src.RenderBlocks;
import net.minecraft.src.RenderEngine;
import net.minecraft.src.Tessellator;
import net.minecraftforge.client.IItemRenderer;
import net.minecraftforge.client.MinecraftForgeClient;
import factorization.common.Core;
import factorization.common.FactorizationUtil;
import factorization.common.ItemBattery;
import factorization.common.TileEntityBattery;

public class BatteryItemRender implements IItemRenderer {
    BlockRenderBattery render_battery;
    public BatteryItemRender(BlockRenderBattery render_battery) {
        this.render_battery = render_battery;
    }
    @Override
    public boolean handleRenderType(ItemStack item, ItemRenderType type) {
        return true;
    }

    @Override
    public boolean shouldUseRenderHelper(ItemRenderType type, ItemStack item, ItemRendererHelper helper) {
        return item.getItem() instanceof ItemBattery;
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
        if (type == ItemRenderType.EQUIPPED) {
            GL11.glTranslatef(0, 1, 1);
            GL11.glRotatef(45, 1, 1, 0);
        }
        RenderEngine re = Minecraft.getMinecraft().renderEngine;
        re.bindTexture(re.getTexture(Core.texture_file_block));
        NBTTagCompound tag = FactorizationUtil.getTag(is);
        render_battery.item_fullness = TileEntityBattery.getFullness(Core.registry.battery.getStorage(is));
        render_battery.renderInInventory();
        render_battery.renderInventoryMode((RenderBlocks)data[0], type);
        GL11.glPopMatrix();
    }

}
