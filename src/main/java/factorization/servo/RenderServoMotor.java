package factorization.servo;

import factorization.api.FzColor;
import factorization.api.FzOrientation;
import factorization.api.Quaternion;
import factorization.fzds.DeltaChunk;
import factorization.fzds.Hammer;
import factorization.fzds.HammerEnabled;
import factorization.shared.Core;
import factorization.shared.FzModel;
import factorization.sockets.TileEntitySocketBase;
import factorization.util.NumUtil;
import net.minecraft.block.Block;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.renderer.RenderBlocks;
import net.minecraft.client.renderer.RenderGlobal;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.entity.RenderEntity;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.monster.EntityEnderman;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.util.ResourceLocation;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL12;

import java.util.Iterator;

public class RenderServoMotor extends RenderEntity {
    FzModel sprocket = new FzModel("servo/sprocket");
    FzModel chasis = new FzModel("servo/chasis.obj");

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
        //Ugh, there's some state that changes when mousing over an item in the inventory...
        MovingObjectPosition mop = Minecraft.getMinecraft().objectMouseOver;
        if (HammerEnabled.ENABLED && DeltaChunk.getClientShadowWorld() == ent.worldObj) {
            mop = Hammer.proxy.getShadowHit();
        }
        boolean highlighted = mop != null && mop.entityHit == ent;
        ServoMotor motor = (ServoMotor) ent;

        GL11.glPushMatrix();
        GL11.glEnable(GL12.GL_RESCALE_NORMAL);
        GL11.glTranslated(x + 0.5, y + 0.5, z + 0.5);
        GL11.glPushMatrix();

        motor.motionHandler.interpolatePosition((float) Math.pow(motor.motionHandler.pos_progress, 2));
        float reorientInterpolation = interp(motor.motionHandler.servo_reorient, motor.motionHandler.prev_servo_reorient, partial);
        orientMotor(motor, partial, reorientInterpolation);

        renderMainModel(motor, partial, reorientInterpolation, false);
        renderSocketAttachment(motor, motor.socket, partial);
        
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
            float d = 1F/2F, h = 0.25F;
            AxisAlignedBB ab = new AxisAlignedBB(-d, -h, -d, d, h, d);
            drawOutlinedBoundingBox(ab);
            ab.offset(ab.minX, ab.minY, ab.minZ);
            GL11.glPopAttrib();
            GL11.glEnable(GL11.GL_TEXTURE_2D);
            
            EntityPlayer player = Core.proxy.getClientPlayer();
            if (player != null) {
                for (int i = 0; i < 9; i++) {
                    ItemStack is = player.inventory.getStackInSlot(i);
                    if (is == null) continue;
                    if (is.getItem() == Core.registry.logicMatrixProgrammer) {
                        render_details = true;
                        break;
                    }
                }
            }
        }
        
        renderInventory(motor, partial);
        GL11.glPopMatrix();
        if (render_details) {
            GL11.glDisable(GL11.GL_DEPTH_TEST);
            RenderManager rm = Minecraft.getMinecraft().getRenderManager();
            GL11.glRotatef(-rm.playerViewY, 0.0F, 1.0F, 0.0F);
            GL11.glRotatef(rm.playerViewX, 1.0F, 0.0F, 0.0F);
            renderStacks(motor);
            GL11.glEnable(GL11.GL_DEPTH_TEST);
        }
        GL11.glPopMatrix();
        motor.motionHandler.interpolatePosition(motor.motionHandler.pos_progress);
        GL11.glDisable(GL12.GL_RESCALE_NORMAL);
        Core.profileEndRender();
    }
    
    void drawOutlinedBoundingBox(AxisAlignedBB box) {
        RenderGlobal.drawBox(box);
    }
    
    void orientMotor(ServoMotor motor, float partial, float reorientInterpolation) {
        final FzOrientation orientation = motor.motionHandler.orientation;
        FzOrientation prevOrientation = motor.motionHandler.prevOrientation;
        if (prevOrientation == null) {
            prevOrientation = orientation;
        }
        
        if (debug_servo_orientation) {
            GL11.glDisable(GL11.GL_LIGHTING);
            GL11.glDisable(GL11.GL_TEXTURE_2D);
            GL11.glLineWidth(4);
            GL11.glBegin(GL11.GL_LINE_STRIP);
            FzOrientation o = orientation;
            GL11.glColor3f(1, 0, 0);
            GL11.glVertex3d(0, 0, 0);
            GL11.glVertex3d(o.facing.getDirectionVec().getX(), o.facing.getDirectionVec().getY(), o.facing.getDirectionVec().getZ());
            GL11.glVertex3d(o.facing.getDirectionVec().getX() + o.top.getDirectionVec().getX(), o.facing.getDirectionVec().getY() + o.top.getDirectionVec().getY(), o.facing.getDirectionVec().getZ() + o.top.getDirectionVec().getZ());
            GL11.glEnd();
            GL11.glLineWidth(2);
            GL11.glBegin(GL11.GL_LINE_STRIP);
            o = prevOrientation;
            GL11.glColor3f(0, 0, 1);
            GL11.glVertex3d(0, 0, 0);
            GL11.glVertex3d(o.facing.getDirectionVec().getX(), o.facing.getDirectionVec().getY(), o.facing.getDirectionVec().getZ());
            GL11.glVertex3d(o.facing.getDirectionVec().getX() + o.top.getDirectionVec().getX(), o.facing.getDirectionVec().getY() + o.top.getDirectionVec().getY(), o.facing.getDirectionVec().getZ() + o.top.getDirectionVec().getZ());
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
    
    void renderSocketAttachment(ServoMotor motor, TileEntitySocketBase socket, float partial) {
        socket.getPos().getX() = socket.getPos().getY() = socket.getPos().getZ() = 0;
        socket.facing = EnumFacing.UP;
        socket.renderInServo(motor, partial);
    }

    void renderInventory(ServoMotor motor, float partial) {
        if (motor.inv.length == 0) return;
        ItemStack is = motor.inv[0];
        if (is == null) {
            return;
        }
        dummy_entity.worldObj = motor.worldObj;
        motor.socket.renderItemOnServo(this, motor, is, partial);
        dummy_entity.worldObj = null;
    }
    
    
    @Override
    protected ResourceLocation getEntityTexture(Entity ent) {
        return Core.blockAtlas;
    }
    
    void renderMainModel(ServoMotor motor, float partial, double ro, boolean hilighting) {
        GL11.glPushMatrix();
        bindTexture(Core.blockAtlas);
        chasis.draw();
        
        FzColor c = motor.motionHandler.color;
        renderServoColor(c);
        GL11.glColor3f(c.getRed(), c.getGreen(), c.getBlue());

        // Determine the sprocket location & rotation
        double rail_width = TileEntityServoRail.width;
        double radius = 0.56 /* from sprocket center to the outer edge of the ring (excluding the teeth) */
                    + 0.06305 /* half the width of the teeth */;
        double constant = Math.PI * 2 * (radius);
        double partial_rotation = NumUtil.interp((float) motor.motionHandler.prev_sprocket_rotation, (float) motor.motionHandler.sprocket_rotation, partial);
        final double angle = constant * partial_rotation;

        radius = 0.25 - 1.0 / 48.0;
        radius = -4.0/16.0;

        float rd = (float) (radius + rail_width);
        if (motor.motionHandler.orientation != motor.motionHandler.prevOrientation && motor.motionHandler.prevOrientation != null) {
            // This could use some work: only stretch if the new direction is parallel to the old gear direction.
            double stretch_interp = ro * 2;
            if (stretch_interp < 1) {
                if (stretch_interp > 0.5) {
                    stretch_interp = 1 - stretch_interp;
                }
                rd += stretch_interp / 8;
            }
        }
        // Render them
        float height_d = 2F/16F;
        GL11.glRotatef(180, 1, 0, 0);
        {
            GL11.glPushMatrix();
            GL11.glTranslatef(0, height_d, rd);
            GL11.glRotatef((float) Math.toDegrees(angle), 0, 1, 0);
            sprocket.draw();
            GL11.glPopMatrix();
        }
        {
            GL11.glPushMatrix();
            GL11.glTranslatef(0, height_d, -rd);
            GL11.glRotatef((float) Math.toDegrees(-angle) + 360F / 9F, 0, 1, 0);
            sprocket.draw();
            GL11.glPopMatrix();
        }
        
        GL11.glColor3f(1, 1, 1);
        GL11.glPopMatrix();
    }
    
    static EntityLiving dummy_entity = new EntityEnderman(null);

    public void renderItem(ItemStack is) {
        // Copied from RenderBiped.renderEquippedItems
        GL11.glPushMatrix();
        //float s = 0.75F;
        //GL11.glScalef(s, s, s);
        float s = 1 / 4F;
        //s *= 0.75F;
        GL11.glScalef(s, s, s);
        
        // Pre-emptively undo transformations that the item renderer does so
        // that we don't get a stupid angle. Minecraft render code is terrible.
        boolean needRotationFix = true;
        if (is.getItem() instanceof ItemBlock) {
            Block block = Block.getBlockFromItem(is.getItem());
            if (block != null && RenderBlocks.renderItemIn3d(block.getRenderType())) {
                needRotationFix = false;
            }
        }
        if (needRotationFix) {
            GL11.glTranslatef(0.9375F, 0.0625F, -0.0F);
            GL11.glRotatef(-335.0F, 0.0F, 0.0F, 1.0F);
            GL11.glRotatef(-50.0F, 0.0F, 1.0F, 0.0F);
        }
        
        float scale = 1.5F;
        GL11.glScalef(scale, scale, scale);
        
        int itemColor = is.getItem().getColorFromItemStack(is, 0);
        float cr = (float)(itemColor >> 16 & 255) / 255.0F;
        float cg = (float)(itemColor >> 8 & 255) / 255.0F;
        float cb = (float)(itemColor & 255) / 255.0F;
        GL11.glColor4f(cr, cg, cb, 1.0F);
        
        this.renderManager.itemRenderer.renderItem(dummy_entity, is, 0);

        if (is.getItem().requiresMultipleRenderPasses()) {
            for (int x = 1; x < is.getItem().getRenderPasses(is.getItemDamage()); x++) {
                this.renderManager.itemRenderer.renderItem(dummy_entity, is, x);
            }
        }
        GL11.glPopMatrix();
    }
    
    void renderStacks(ServoMotor motor) {
        GL11.glDisable(GL11.GL_LIGHTING);
        GL11.glPushMatrix();
        
        float scale = 4F/128F;
        GL11.glScalef(scale, scale, scale);
        renderStack(motor.getArgStack(), scale, 0);
        renderStack(motor.getInstructionsStack(), scale, 1);
        renderStack(motor.getEntryInstructionStack(), scale, 2);
        
        FzColor color = motor.motionHandler.color;
        if (color != FzColor.NO_COLOR) {
            FontRenderer fr = getFontRendererFromRenderManager();
            String text = "" + color;
            int width = fr.getStringWidth(text);
            GL11.glRotatef(180, 0, 0, 1);
            GL11.glTranslatef(-width/4F, 10, 0);
            float s = 0.5F;
            GL11.glScalef(s, s, s);
            fr.drawString(text, 0, 0, 0xDAC9D0, true);
        }
        
        GL11.glPopMatrix();
        GL11.glEnable(GL11.GL_LIGHTING);
    }
    
    void renderStack(ServoStack ss, float scale, int i) {
        if (i != 0 && ss.getSize() == 0) return;
        GL11.glPushMatrix();
        GL11.glRotatef(180, 0, 0, 1);
        GL11.glTranslatef(0, -(0.9F)/scale, 0);
        int color = 0xFFFFCF;
        if (i == 0) {
            GL11.glTranslatef(0, 0, 0);
        } else if (i == 1) {
            GL11.glTranslatef(-32, 8*ss.getSize(), 0);
            color = 0xCFFFCF;
        } else if (i == 2) {
            GL11.glTranslatef(32, 8*ss.getSize(), 0);
            color = 0xEFEFEF;
        }
        renderStackWithColor(ss, color);
        GL11.glPopMatrix();
    }
    
    boolean renderStackWithColor(ServoStack stack, int color) {
        FontRenderer fr = getFontRendererFromRenderManager();
        int count = stack.getSize();
        if (count == 0) {
            fr.drawString("_", 0, 0, color, true);
            return false;
        }
        GL11.glPushMatrix();
        float s = 7.0F/count;
        if (s > 1) {
            s = 1;
        }
        GL11.glScalef(s, s, s);
        GL11.glTranslatef(0, count*7.5F, 0);
        fr.drawString("_", 0, 0, color, true);
        Iterator<Object> it = stack.descendingIterator();
        while (it.hasNext()) {
            Object o = it.next();
            GL11.glTranslatef(0, -10, 0);;
            fr.drawString(o != null ? o.toString() : "null", 0, 0, color, true);
        }
        GL11.glPopMatrix();
        return true;
    }
    
    void renderServoColor(FzColor color) {
        if (color == FzColor.NO_COLOR) return;
        BlockRenderHelper block = BlockRenderHelper.instance;
        block.setBlockBoundsOffset(0, 0, 0);
        block.useTexture(null);
        block.setTexture(1 /* up */, colorIcon);
        block.beginWithMirroredUVs();
        GL11.glPushMatrix();
        float d = -0.5F;
        GL11.glTranslatef(d, d - 3F/8F + 0.0001F, d);
        {
            // We need to get 14/16ths transformed for 10/16ths.
            float b = 14F/16F;
            GL11.glScalef(1/b, 1, 1/b);
            float s = 10F/16F;
            GL11.glScalef(s, 1, s);
            float t = 3.2F/16F;
            GL11.glTranslatef(t, 0, t);
        }
        Tessellator.instance.startDrawingQuads();
        block.renderForTileEntity();
        //GL11.glDisable(GL11.GL_LIGHTING);
        Tessellator.instance.draw();
        //GL11.glEnable(GL11.GL_LIGHTING);
        GL11.glPopMatrix();
    }
}
