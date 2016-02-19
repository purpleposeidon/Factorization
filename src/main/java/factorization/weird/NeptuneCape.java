package factorization.weird;

import factorization.beauty.TileEntitySteamShaft;
import factorization.beauty.TileEntitySteamShaftRenderer;
import factorization.shared.Core;
import factorization.shared.FzModel;
import factorization.shared.PatreonRewards;
import factorization.util.NORELEASE;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.AbstractClientPlayer;
import net.minecraft.client.model.ModelBase;
import net.minecraft.client.model.ModelRenderer;
import net.minecraft.client.renderer.entity.Render;
import net.minecraft.client.renderer.entity.RenderPlayer;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraftforge.client.event.RenderPlayerEvent;
import net.minecraftforge.client.event.TextureStitchEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import org.lwjgl.opengl.GL11;

import java.util.Iterator;
import java.util.List;

public class NeptuneCape {
    {
        Core.loadBus(this);
        LmpMaskRenderer.init();
    }
    
    static boolean hideMask(EntityPlayer player) {
        final ItemStack helmet = player.getCurrentArmor(3);
        return helmet == null || helmet.getItem() != Core.registry.logicMatrixProgrammer;
    }
    
    static boolean should_render_mask = false;
    private static int total_hash = 0;
    private static final int[] mask_wearers = build_list(PatreonRewards.getMasqueraders());
    private static int[] build_list(List<String> names) {
        int[] ret = new int[names.size()];
        int hash_or = 0;
        for (int i = 0; i < ret.length; i++) {
            hash_or |= ret[i] = names.get(i).hashCode();
        }
        total_hash = ~hash_or;
        return ret;
    }

    private static boolean isNameWhitelisted(String name) {
        int name_hash = name.hashCode();
        if ((name_hash & total_hash) != 0) return false;
        for (int hash : mask_wearers) {
            if (hash == name_hash) return true;
        }
        return false;
    }
    
    @SubscribeEvent
    public void renderLmp(RenderPlayerEvent.Pre event) {
        if (invalid) return;
        EntityPlayer player = event.entityPlayer;
        if (dirty) {
            dirty = false;
            injectModel(player);
        }
        should_render_mask = !hideMask(player) && isNameWhitelisted(player.getName());
        if (should_render_mask) {
            LmpMaskRenderer.rendering_player = (AbstractClientPlayer) player;
        }
    }

    boolean invalid = false;
    void injectModel(EntityPlayer player) {
        Minecraft mc = Minecraft.getMinecraft();
        Render<EntityPlayer> pr = mc.getRenderManager().getEntityRenderObject(player);
        if (!((Render) pr instanceof RenderPlayer)) {
            if (!invalid) {
                Core.logWarning("Unable to patch Player model!");
            }
            invalid = true;
            return;
        }
        RenderPlayer playerRenderer = (RenderPlayer) (Render) pr;
        ModelRenderer head = playerRenderer.getMainModel().bipedHead;
        if (head.childModels != null) {
            for (Iterator<ModelRenderer> it = head.childModels.iterator(); it.hasNext(); ) {
                ModelRenderer child = it.next();
                if (child instanceof LmpMaskRenderer) {
                    it.remove();
                }

            }
        }
        head.addChild(new LmpMaskRenderer(playerRenderer.getMainModel()));
    }
    
    boolean dirty = true;
    @SubscribeEvent
    public void resourcePackChanged(TextureStitchEvent.Post event) {
        dirty = true;
    }
    
    static class LmpMaskRenderer extends ModelRenderer {
        static FzModel LMP = new FzModel("lmpMask");
        static AbstractClientPlayer rendering_player;
        static void init() { }

        public LmpMaskRenderer(ModelBase base) {
            super(base);
            base.boxList.remove(this); // Prevents arrows from rendering inside us; else rendering will crash
        }
        
        @Override
        public void render(float partial) {
            if (rendering_player == null) return; // Model may be rendered twice for the hurt animation
            if (!should_render_mask) {
                rendering_player = null;
                return;
            }
            GL11.glPushMatrix();
            
            GL11.glTranslatef(0, -7F/16F, -4.5F/16F);
            GL11.glRotatef(-90, 0, 0, 1);
            GL11.glRotatef(-90, 1, 0, 0);

            LMP.draw();
            //TileEntitySteamShaftRenderer.whirligig.draw();
            GL11.glPopMatrix();
            Minecraft.getMinecraft().renderEngine.bindTexture(rendering_player.getLocationSkin());
            rendering_player = null;
        }
        
    }
}
