package factorization.weird;

import org.lwjgl.opengl.GL11;

import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.AbstractClientPlayer;
import net.minecraft.client.model.ModelBase;
import net.minecraft.client.model.ModelRenderer;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.entity.monster.EntityEnderman;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraftforge.client.event.RenderPlayerEvent;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import factorization.shared.Core;

public class NeptuneCape {
    {
        Core.loadBus(this);
    }
    
    ItemStack LMP = new ItemStack(Core.registry.logicMatrixProgrammer);
    EntityEnderman dummy_entity = new EntityEnderman(null);
    
    class LmpMaskRenderer extends ModelRenderer {
        
        AbstractClientPlayer player;
        
        public LmpMaskRenderer(AbstractClientPlayer player, ModelBase base) {
            super(base);
            this.player = player;
        }
        
        @Override
        public void render(float partial) {
            if (player.getHideCape()) return;
            if (player.getCurrentArmor(3) != null) return;
            GL11.glPushMatrix();
            
            float s = 12F/16F; GL11.glScalef(s, s, s);
            
            GL11.glRotatef(-90, 0, 0, 1);
            
            GL11.glTranslatef(3.5F/16F, -7.5F/16F, -5.5F/16F);
            
            GL11.glTranslatef(0.9375F, 0.0625F, -0.0F);
            GL11.glRotatef(-335.0F, 0.0F, 0.0F, 1.0F);
            GL11.glRotatef(-50.0F, 0.0F, 1.0F, 0.0F);
            
            GL11.glPushAttrib(GL11.GL_ENABLE_BIT);
            GL11.glEnable(GL11.GL_CULL_FACE);
            RenderManager.instance.itemRenderer.renderItem(dummy_entity, LMP, 0);
            GL11.glPopAttrib();
            GL11.glPopMatrix();
            Minecraft.getMinecraft().renderEngine.bindTexture(player.getLocationSkin());
        }
        
    }
    
    @SubscribeEvent
    public void renderLmp(RenderPlayerEvent.Pre event) {
        EntityPlayer player = event.entityPlayer;
        if (player.getHideCape()) return;
        if (!"neptunepink".equals(player.getCommandSenderName())) return;
        ModelRenderer head = event.renderer.modelBipedMain.bipedHead;
        if (head.childModels == null) {
            head.addChild(new LmpMaskRenderer((AbstractClientPlayer) player, event.renderer.modelBipedMain));
        }
    }
}
