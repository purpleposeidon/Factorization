package factorization.common.servo;

import java.io.IOException;
import java.io.InputStream;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.model.ModelBiped;
import net.minecraft.client.model.ModelZombie;
import net.minecraft.client.renderer.GLAllocation;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.entity.RenderBiped;
import net.minecraft.client.renderer.entity.RenderEntity;
import net.minecraft.client.renderer.entity.RenderItem;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.monster.EntityEnderman;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemDye;
import net.minecraft.item.ItemStack;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.Icon;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.Vec3;
import net.minecraftforge.client.model.obj.WavefrontObject;
import net.minecraftforge.common.ForgeDirection;

import org.lwjgl.opengl.GL11;

import factorization.api.FzOrientation;
import factorization.api.Quaternion;
import factorization.common.BlockIcons;
import factorization.common.Core;
import factorization.common.FactorizationUtil;
import factorization.common.FactorizationUtil.FzInv;
import factorization.common.TileEntitySocketBase;

public class RenderServoMotor extends RenderEntity {
    static int both_lists = -1, sprocket_display_list = -1, chasis_display_list = -1;
    boolean loaded_models = false;
    
    private static Icon subsetIcon;
    private static Tessellator subsetTessellator = new Tessellator() {
        @Override
        public void setTextureUV(double u, double v) {
            super.setTextureUV(subsetIcon.getInterpolatedU(u*16), subsetIcon.getInterpolatedV(v*16));
        }
    };
    
    static void loadModel(int displayList, String modelName, Icon icon) {
        try {
            WavefrontObject sprocket = null;
            InputStream input = null;
            try {
                ResourceLocation rl = Core.getResource(modelName);
                input = Minecraft.getMinecraft().getResourceManager().getResource(rl).getInputStream();
                if (input == null) {
                    Core.logWarning("Missing servo sprocket model: " + rl);
                    return;
                }
                sprocket = new WavefrontObject(rl.toString(), input);
                input.close();
                input = null;
            } finally {
                if (input != null) {
                    input.close();
                }
            }
            
            GL11.glNewList(displayList, GL11.GL_COMPILE);
            double modelScale = 1.0/16.0;
            GL11.glScaled(modelScale, modelScale, modelScale);
            subsetIcon = icon;
            subsetTessellator.startDrawingQuads();
            sprocket.tessellateAll(subsetTessellator);
            subsetTessellator.draw();
            modelScale = 1/modelScale;
            GL11.glScaled(modelScale, modelScale, modelScale);
            GL11.glEndList();
        } catch (IOException e) {
            Core.logWarning("Failed to load model %s", modelName);
            e.printStackTrace();
        }
    }
    
    void loadModels() {
        if (both_lists != -1) {
            GLAllocation.deleteDisplayLists(both_lists);
        }
        both_lists = GLAllocation.generateDisplayLists(3);
        sprocket_display_list = both_lists + 0;
        chasis_display_list = both_lists + 1;
        loadModel(sprocket_display_list, "models/servo/socket.obj", BlockIcons.servo$model$gear);
        loadModel(chasis_display_list, "models/servo/chasis.obj", BlockIcons.servo$model$chasis);
    }

    void renderSprocket() {
        GL11.glCallList(sprocket_display_list);
    }
    
    void renderChasis() {
        GL11.glCallList(chasis_display_list);
    }

    ForgeDirection getPerpendicular(ForgeDirection a, ForgeDirection b) {
        return a.getRotation(b);
    }

    static Vec3 quat_vector = Vec3.createVectorHelper(0, 0, 0);
    static Quaternion start = new Quaternion(), end = new Quaternion();

    float interp(double a, double b, double part) {
        double d = a - b;
        float r = (float) (b + d * part);
        double v;
        // h(x,k) = (sin(x∙pi∙4.5)^2)∙x
        // v = Math.pow(Math.sin(r*Math.PI*4.5), 2)*r;
        
        v = Math.min(1, r * r * 4);
        return (float) v;
    }

    private Quaternion q0 = new Quaternion(), q1 = new Quaternion();
    private static boolean debug_servo_orientation = false;

    @Override
    public void doRender(Entity ent, double x, double y, double z, float yaw, float partial) {
        Core.profileStartRender("servo");
        MovingObjectPosition mop = Minecraft.getMinecraft().objectMouseOver;
        boolean highlighted = mop != null && mop.entityHit == ent;
        ServoMotor motor = (ServoMotor) ent;

        GL11.glPushMatrix();
        GL11.glTranslated(x + 0.5, y + 0.5, z + 0.5);
        GL11.glPushMatrix();

        motor.interpolatePosition((float) Math.pow(motor.pos_progress, 2));
        float reorientInterpolation = interp(motor.servo_reorient, motor.prev_servo_reorient, partial);
        orientMotor(motor, partial, reorientInterpolation);

        renderMainModel(motor, partial, reorientInterpolation, false);
        renderSocketAttachment(motor.socket, partial);
        
        boolean render_details = false;
        if (highlighted) {
            GL11.glDisable(GL11.GL_TEXTURE_2D);
            GL11.glPushAttrib(GL11.GL_COLOR_BUFFER_BIT | GL11.GL_LIGHTING_BIT);
            GL11.glDisable(GL11.GL_LIGHTING);
            GL11.glEnable(GL11.GL_BLEND);
            GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
            float gray = 0.65F;
            GL11.glColor4f(gray, gray, gray, 0.8F);
            GL11.glLineWidth(1.5F);
            Minecraft mc = Minecraft.getMinecraft();
            float d = 1F, h = 0.25F;
            AxisAlignedBB ab = AxisAlignedBB.getBoundingBox(-d, -h, -d, d, h, d);
            mc.renderGlobal.drawOutlinedBoundingBox(ab);
            ab.offset(ab.minX, ab.minY, ab.minZ);
            GL11.glPopAttrib();
            GL11.glEnable(GL11.GL_TEXTURE_2D);
            
            EntityPlayer player = Core.proxy.getClientPlayer();
            if (player != null) {
                ItemStack is = player.getHeldItem();
                final ItemStack helmet = player.getCurrentArmor(3);
                if (is != null && is.getItem() == Core.registry.logicMatrixProgrammer ||
                        (helmet != null && FactorizationUtil.oreDictionarySimilar("visionInducingEyewear", helmet))) {
                    render_details = true;
                }
            }
        }
        
        if (render_details) {
            renderInventory(motor, partial);
        }
        GL11.glPopMatrix();
        if (render_details) {
            GL11.glRotatef(-RenderManager.instance.playerViewY, 0.0F, 1.0F, 0.0F);
            GL11.glRotatef(RenderManager.instance.playerViewX, 1.0F, 0.0F, 0.0F);
            renderStacks(motor);
        }
        GL11.glPopMatrix();
        motor.interpolatePosition(motor.pos_progress);
        Core.profileEndRender();
    }
    
    void orientMotor(ServoMotor motor, float partial, float reorientInterpolation) {
        final FzOrientation orientation = motor.orientation;
        final FzOrientation prevOrientation = motor.prevOrientation;

        if (debug_servo_orientation) {
            GL11.glDisable(GL11.GL_LIGHTING);
            GL11.glDisable(GL11.GL_TEXTURE_2D);
            GL11.glLineWidth(4);
            GL11.glBegin(GL11.GL_LINE_STRIP);
            FzOrientation o = orientation;
            GL11.glColor3f(1, 0, 0);
            GL11.glVertex3d(0, 0, 0);
            GL11.glVertex3d(o.facing.offsetX, o.facing.offsetY, o.facing.offsetZ);
            GL11.glVertex3d(o.facing.offsetX + o.top.offsetX, o.facing.offsetY + o.top.offsetY, o.facing.offsetZ + o.top.offsetZ);
            GL11.glEnd();
            GL11.glLineWidth(2);
            GL11.glBegin(GL11.GL_LINE_STRIP);
            o = prevOrientation;
            GL11.glColor3f(0, 0, 1);
            GL11.glVertex3d(0, 0, 0);
            GL11.glVertex3d(o.facing.offsetX, o.facing.offsetY, o.facing.offsetZ);
            GL11.glVertex3d(o.facing.offsetX + o.top.offsetX, o.facing.offsetY + o.top.offsetY, o.facing.offsetZ + o.top.offsetZ);
            GL11.glEnd();
        }

        // Servo facing
        Quaternion qt;
        if (prevOrientation == orientation) {
            qt = Quaternion.fromOrientation(orientation);
        } else {
            q0.update(Quaternion.fromOrientation(prevOrientation));
            q1.update(Quaternion.fromOrientation(orientation));
            if (q0.dotProduct(q1) < 0) {
                q0.incrScale(-1);
            }
            q0.incrLerp(q1, reorientInterpolation);
            qt = q0;
        }
        qt.glRotate();
        GL11.glRotatef(90, 0, 0, 1);

        if (debug_servo_orientation) {
            GL11.glColor3f(1, 0, 1);
            GL11.glBegin(GL11.GL_LINE_STRIP);
            GL11.glVertex3d(0, 0, 0);
            GL11.glVertex3d(1, 0, 0);
            GL11.glVertex3d(1, 1, 0);
            GL11.glVertex3d(0, 0, 0);
            GL11.glEnd();
            GL11.glColor3f(1, 1, 1);
            GL11.glEnable(GL11.GL_TEXTURE_2D);
            GL11.glEnable(GL11.GL_LIGHTING);
        }
    }
    
    void renderSocketAttachment(TileEntitySocketBase socket, float partial) {
        float d = -0.5F;
        float y = -2F/16F;
        GL11.glTranslatef(d, y, d);
        socket.xCoord = socket.yCoord = socket.zCoord = 0;
        socket.facing = ForgeDirection.UP;
        
        GL11.glPushMatrix();
        socket.renderTesr(partial);
        GL11.glPopMatrix();
        
        bindTexture(Core.blockAtlas);
        Tessellator tess = Tessellator.instance;
        tess.startDrawingQuads();
        socket.renderStatic(tess);
        tess.draw();
        GL11.glTranslatef(-d, -y, -d);
    }

    RenderItem renderItem = new RenderItem();

    void renderInventory(ServoMotor motor, float partial) {
        ItemStack held = motor.getHeldItem();
        if (held != null) {
            //if held.getItem().isFull3D()...
            GL11.glPushMatrix();
            GL11.glTranslatef(0, 3.5F/16F, 0);
            Item item = held.getItem();
            if (item instanceof ItemBlock) {
                GL11.glTranslatef(1F/16F, 2.75F/16F, 2.5F/16F);
                GL11.glRotatef(45, 0, 1, 0);
                GL11.glRotatef(23.5F, 0, 0, -1);
            }
            renderItem(motor, held, partial);
            GL11.glPopMatrix();
        }
        
        
        GL11.glPushMatrix();
        GL11.glRotatef(90, 1, 0, 0);
        float s = 1/4F;
        FzInv inv = motor.getInv();
        final Minecraft mc = Minecraft.getMinecraft();
        long range = 9*20;
        double d = motor.worldObj.getTotalWorldTime() + partial;
        float now = (float) ((d % range)/(double)range);
        for (int i = 0; i < inv.size(); i++) {
            ItemStack is = inv.get(i);
            if (is == null || is == held) {
                continue;
            }
            GL11.glPushMatrix();
            GL11.glTranslatef(0.65F, 0.25F, -0.125F);
            GL11.glScalef(s, s, s);
            GL11.glRotatef(-90, 0, 0, 1);
            float offset = i;
            if (i > 2) {
                offset += 1;
            }
            GL11.glTranslatef((offset - 2)*0.8F, -1.5F, 0);
            try {
                renderItem(motor, is, partial);
            } catch (Exception e) {
                System.err.println("Error rendering item: " + is);
                e.printStackTrace();
                inv.set(i, null);
            }
            GL11.glPopMatrix();
        }
        GL11.glPopMatrix();
    }
    
    
    @Override
    protected ResourceLocation getEntityTexture(Entity par1Entity) {
        return Core.blockAtlas;
    }
    
    void renderMainModel(ServoMotor motor, float partial, double ro, boolean hilighting) {
        GL11.glPushMatrix();
        //0.1 blender units = 1/16 m
        float scale = 0.1F*16F;
        GL11.glScalef(scale, scale, scale);
        bindTexture(Core.blockAtlas);
        if (!loaded_models) {
            try {
                loadModels();
            } catch (Throwable e) {
                Core.logWarning("Failed to load servo sprocket model");
                e.printStackTrace();
            }
            loaded_models = true;
        }
        double modelScale = 1.0/16.0;
        renderChasis();

        // Sprocket rotation
        double rail_width = TileEntityServoRail.width;
        double radius = 0.56 /* from sprocket center to the outer edge of the ring (excluding the teeth) */
                    + 0.06305 /* half the width of the teeth */;
        double constant = Math.PI * 2 * (radius);
        double dr = motor.sprocket_rotation - motor.prev_sprocket_rotation;
        double partial_rotation = motor.prev_sprocket_rotation + dr * partial;
        final double angle = constant * partial_rotation;

        radius = 0.25 - 1.0 / 48.0;

        float rd = (float) (radius + rail_width);
        if (motor.orientation != motor.prevOrientation) {
            double stretch_interp = ro * 2;
            if (stretch_interp < 1) {
                if (stretch_interp > 0.5) {
                    stretch_interp = 1 - stretch_interp;
                }
                rd += stretch_interp / 8;
            }
        }
        // Sprocket rendering. (You wouldn't have been able to tell by reading the code.)
        float height_d = 2F/16F; // -1.5F/64F;
        GL11.glRotatef(180, 1, 0, 0);
        {
            GL11.glPushMatrix();
            GL11.glTranslatef(0, height_d, rd);
            GL11.glRotatef((float) Math.toDegrees(angle), 0, 1, 0);
            renderSprocket();
            GL11.glPopMatrix();
        }
        {
            GL11.glPushMatrix();
            GL11.glTranslatef(0, height_d, -rd);
            GL11.glRotatef((float) Math.toDegrees(-angle) + 360F / 9F, 0, 1, 0);
            renderSprocket();
            GL11.glPopMatrix();
        }
        
        GL11.glPopMatrix();
    }

    static ItemStack equiped_item = null;

    static EntityLiving item_holder = new EntityLiving(null) {
        @Override
        public ItemStack getHeldItem() {
            return equiped_item;
        }
    };

    private static class HolderRenderer extends RenderBiped {

        public HolderRenderer(ModelBiped model, float someFloat) {
            super(model, someFloat);
        }

        public void renderItem(float partial) {
            renderEquippedItems(item_holder, partial);
            // renderModel(item_holder, 0, 0, 0, 0, 0, 0);
        }
    }

    static HolderRenderer holder_render = new HolderRenderer(new ModelZombie(), 1);
    static EntityLiving dummy_entity = new EntityEnderman(null);

    void renderItem(ServoMotor motor, ItemStack is, float partial) {
        equiped_item = is;
        if (equiped_item == null) {
            return;
        }
        dummy_entity.worldObj = motor.worldObj;
        holder_render.setRenderManager(renderManager);
        do_renderItem(equiped_item);
    }

    public void do_renderItem(ItemStack itemstack) {
        // Yoinked from RenderBiped.renderEquippedItems
        GL11.glPushMatrix();
        float s = 1F / 4F;
        GL11.glScalef(s, s, s);
        
        
        //Pre-emptively undo transformations that the item renderer does so that we don't get a stupid angle
        //Minecraft render code is terrible.
        GL11.glTranslatef(0.9375F, 0.0625F, -0.0F);
        GL11.glRotatef(-335.0F, 0.0F, 0.0F, 1.0F);
        GL11.glRotatef(-50.0F, 0.0F, 1.0F, 0.0F);
        float f6 = 1.5F;
        GL11.glScalef(f6, f6, f6);
        
        this.renderManager.itemRenderer.renderItem(dummy_entity, itemstack, 0);

        if (itemstack.getItem().requiresMultipleRenderPasses()) {
            for (int x = 1; x < itemstack.getItem().getRenderPasses(itemstack.getItemDamage()); x++) {
                this.renderManager.itemRenderer.renderItem(dummy_entity, itemstack, x);
            }
        }

        GL11.glPopMatrix();
    }

    protected void func_82422_c() {
        GL11.glTranslatef(0.0F, 0.1875F, 0.0F);
    }
    
    void renderStacks(ServoMotor motor) {
        GL11.glDisable(GL11.GL_LIGHTING);
        GL11.glPushMatrix();
        
        float s = 1F/128F;
        GL11.glScalef(s, s, s);
        for (int i = 0; i < ServoMotor.STACKS; i++) {
            GL11.glPushMatrix();
            ServoStack ss = motor.getServoStack(i);
            GL11.glRotatef(180/16*(8.5F + i), 0, 0, 1);
            GL11.glTranslatef(0, -(0.9F)/s, 0);
            if (renderStack(ss, ItemDye.dyeColors[15 - i])) {
            }
            GL11.glPopMatrix();
        }
        
        GL11.glPopMatrix();
        GL11.glEnable(GL11.GL_LIGHTING);
    }
    
    boolean renderStack(ServoStack stack, int color) {
        int i = 0;
        Minecraft mc = Minecraft.getMinecraft();
        FontRenderer fr = mc.fontRenderer;
        fr.drawString("䷼", 0, 0, color, false); // All the cool kids use Yijing.
        for (Object o : stack) {
            if (i == 0) {
                GL11.glPushMatrix();
            }
            String text = o.toString();
            GL11.glTranslatef(0, -10, 0);;
            fr.drawString(text, 0, 0, color, false);
            i++;
        }
        if (i > 0) {
            GL11.glPopMatrix();
        }
        return i > 0;
    }
}
