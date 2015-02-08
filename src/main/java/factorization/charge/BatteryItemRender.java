package factorization.charge;

import factorization.util.ItemUtil;
import net.minecraft.client.renderer.RenderBlocks;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraftforge.client.IItemRenderer;

import org.lwjgl.opengl.GL11;

import factorization.shared.Core;

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
        NBTTagCompound tag = ItemUtil.getTag(is);
        render_battery.item_fullness = TileEntityBattery.getFullness(Core.registry.battery.getStorage(is));
        render_battery.renderInInventory();
        render_battery.renderInventoryMode((RenderBlocks)data[0], type);
        GL11.glPopMatrix();
    }

}
