package factorization.sockets.fanturpeller;

import java.io.IOException;
import java.util.ArrayList;

import factorization.aabbdebug.AabbDebugger;
import factorization.mechanics.MechanicsController;
import factorization.fzds.DeltaChunk;
import factorization.fzds.interfaces.IDeltaChunk;
import factorization.shared.Core;
import factorization.shared.NORELEASE;
import factorization.util.InvUtil;
import factorization.util.NumUtil;
import factorization.util.SpaceUtil;
import net.minecraft.command.IEntitySelector;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.IProjectile;
import net.minecraft.entity.item.EntityFallingBlock;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.item.EntityTNTPrimed;
import net.minecraft.entity.monster.EntityGhast;
import net.minecraft.entity.passive.EntityBat;
import net.minecraft.entity.passive.EntityChicken;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Items;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.DamageSource;
import net.minecraft.util.Vec3;
import net.minecraft.world.World;
import net.minecraftforge.common.util.ForgeDirection;

import org.lwjgl.opengl.GL11;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import factorization.api.Coord;
import factorization.api.datahelpers.DataHelper;
import factorization.api.datahelpers.IDataSerializable;
import factorization.api.datahelpers.Share;
import factorization.common.FactoryType;
import factorization.common.FzConfig;
import factorization.notify.Notice;
import factorization.servo.RenderServoMotor;
import factorization.servo.ServoMotor;
import factorization.util.InvUtil.FzInv;
import factorization.sockets.ISocketHolder;

public class BlowEntities extends SocketFanturpeller implements IEntitySelector {
    short dropDelay = 0;
    ArrayList<ItemStack> buffer = new ArrayList<ItemStack>(1);
    
    @Override
    public String getInfo() {
        String msg = (isSucking ? "Suck" : "Blow") + " entities";
        if (!buffer.isEmpty()) {
            msg += "\nBuffered output";
        }
        return msg;
    }
    
    @Override
    public IDataSerializable serialize(String prefix, DataHelper data) throws IOException {
        super.serialize(prefix, data);
        dropDelay = data.as(Share.PRIVATE, "dropDelay").putShort(dropDelay);
        buffer = data.as(Share.PRIVATE, "murderBuff").putItemArray(buffer);
        return this;
    }
    
    @Override
    public FactoryType getFactoryType() {
        return FactoryType.SOCKET_BLOWER;
    }
    
    @Override
    protected boolean shouldFeedJuice() {
        return buffer.isEmpty();
    }
    
    @Override
    int getRequiredCharge() {
        return 1 + target_speed*target_speed;
    }
    
    private AxisAlignedBB area = AxisAlignedBB.getBoundingBox(0, 0, 0, 0, 0, 0);
    private AxisAlignedBB death_area = AxisAlignedBB.getBoundingBox(0, 0, 0, 0, 0, 0);
    
    void addCoord(double x, double y, double z) {
        if (x < 0.0D) area.minX += x;
        if (x > 0.0D) area.maxX += x;
        if (y < 0.0D) area.minY += y;
        if (y > 0.0D) area.maxY += y;
        if (z < 0.0D) area.minZ += z;
        if (z > 0.0D) area.maxZ += z;
    }

    @Override
    protected void fanturpellerUpdate(ISocketHolder socket, Coord coord, boolean powered) {
        if (powered) return;
        //We can't do an isRemote check because position doesn't get synced enough.
        if (!shouldDoWork()) return;
        if (!isSucking && !worldObj.isRemote) {
            dropItems(coord, socket);
        }
        if (!worldObj.isRemote && socket.dumpBuffer(buffer)) {
            return;
        }
        if (socket == this) {
            // If we're on a servo, we want to include the servo's block in the sucking.
            coord.adjust(facing);
            // Since that var isn't a copy, we need to adjust it back as well.
        }
        area.minX = death_area.minX = coord.x;
        area.minY = death_area.minY = coord.y;
        area.minZ = death_area.minZ = coord.z;
        area.maxX = death_area.maxX = coord.x + 1;
        area.maxY = death_area.maxY = coord.y + 1;
        area.maxZ = death_area.maxZ = coord.z + 1;
        if (socket == this) {
            coord.adjust(facing.getOpposite());
        }
        int side_range = target_speed;
        int front_range = 3 + target_speed*target_speed;
        if (isSucking) front_range++;
        for (ForgeDirection dir : ForgeDirection.VALID_DIRECTIONS) {
            if (dir == facing) {
                addCoord(dir.offsetX*front_range, dir.offsetY*front_range, dir.offsetZ*front_range);
            } else if (dir.getOpposite() != facing) {
                addCoord(dir.offsetX*side_range, dir.offsetY*side_range, dir.offsetZ*side_range);
            }
        }
        double s = 0.025;
        ForgeDirection dir = isSucking ? facing.getOpposite() : facing;
        found_player = false;
        iterateEntities(front_range, s, dir, area, death_area, worldObj);
        if (worldObj == DeltaChunk.getWorld(worldObj)) {
            for (IDeltaChunk idc : DeltaChunk.getSlicesContainingPoint(coord)) {
                iterateFzdsEntities(front_range, s, dir, idc);
            }
        }

        if (worldObj.isRemote) {
            s *= 8;
            if (isSucking) s *= -1;
            int count = target_speed - 1;
            if (count <= 0) {
                count = 1;
                if (rand.nextBoolean()) {
                    return;
                }
            }
            ForgeDirection d = facing;
            float ds = isSucking ? 3 : 0;
            for (int i = 0; i < count; i++) {
                double x = pick(area.minX, area.maxX) + d.offsetX*ds;
                double y = pick(area.minY, area.maxY) + d.offsetY*ds;
                double z = pick(area.minZ, area.maxZ) + d.offsetZ*ds;
                //Good ones: explode, cloud, smoke, snowshovel
                worldObj.spawnParticle("cloud", x, y, z, facing.offsetX*s, facing.offsetY*s, facing.offsetZ*s);
            }
        }
    }

    private void iterateFzdsEntities(int front_range, double s, ForgeDirection dir, IDeltaChunk idc) {
        iterateEntities(front_range, s, idc.shadow2real(dir), idc.shadow2real(area), idc.shadow2real(death_area), idc.worldObj);
        if (!worldObj.isRemote && Core.dev_environ && idc.getController() instanceof MechanicsController) {
            Vec3 force = SpaceUtil.fromDirection(dir);
            double forceScale = target_speed / 20.0;
            if (isSucking) forceScale *= -1;
            SpaceUtil.incrScale(force, forceScale);
            MechanicsController.push(idc, getCoord(), force);
        }
    }

    private void iterateEntities(int front_range, double s, ForgeDirection dir, AxisAlignedBB box, AxisAlignedBB deathBox, World w) {
        AabbDebugger.addBox(box);
        for (Entity ent : (Iterable<Entity>)w.getEntitiesWithinAABBExcludingEntity(null, box, this)) {
            if (dir.offsetY == (isSucking ? -1 : +1)) {
                NORELEASE.fixme("Test; is it backwards?");
                waftEntity(ent);
            }
            suckEntity(ent, front_range, dir, s);
            if (!w.isRemote && isSucking && ent.boundingBox != null && ent.boundingBox.intersectsWith(deathBox)) {
                murderEntity(ent);
            }
            ent.fallDistance *= 0.8F;
            if (!isSucking && found_player && ent instanceof EntityPlayer) {
                ent.onGround = true;
                if (!worldObj.isRemote) {
                    // Need to let the player fly!? Grr.
                    // See NetHandlerPlayServer.processPlayer
                    ent.posY += ent.motionY * 10;
                    /*double floatFuzz = 0.03125;
                    ent.posY += floatFuzz * 2;*/
                }
            }
        }
    }

    double pick(double min, double max) {
        double d = max - min;
        return min + d*rand.nextDouble();
    }
    
    void suckEntity(Entity ent, int front_range, ForgeDirection dir, double s) {
        double ms = s;
        double distSq = ent.getDistanceSq(xCoord + 0.5, yCoord + 0.5, zCoord + 0.5);
        ms /= distSq/(front_range*10);
        if (ms > 0.15) ms = 0.15;
        double dx = ms*dir.offsetX;
        double dy = ms*dir.offsetY;
        double dz = ms*dir.offsetZ;
        if (isSucking || dir == ForgeDirection.UP) {
            if (dir.offsetX == 0) {
                double diff = ent.posX - xCoord - 0.5;
                dx -= diff*ms;
            }
            if (dir.offsetY == 0) {
                double diff = ent.posY - yCoord - 0.5;
                dy -= diff*ms;
            }
            if (dir.offsetZ == 0) {
                double diff = ent.posZ - zCoord - 0.5;
                dz -= diff*ms;
            }
            if (ent.motionY < dy) {
                ent.motionY = dy;
            }
        }
        ent.moveEntity(dx, dy, dz);
    }
    
    void waftEntity(Entity ent) {
        double damp = 0.5;
        ent.motionX *= damp;
        ent.motionZ *= damp;
        if (ent.motionY < 0.05) {
            ent.motionY = (ent.motionY + 0.05)/2;
        }
    }
    
    void murderEntity(Entity ent) {
        if (ent.isDead) return;
        if (ent instanceof EntityItem) {
            EntityItem ei = (EntityItem) ent;
            buffer.add(ei.getEntityItem());
            ent.setDead();
            return;
        }
        if (ent instanceof EntityGhast) {
            if (ent.getClass() != EntityGhast.class) return; //I'm thinking of Twilight Forest's Ur-Ghast here.
            EntityGhast ghast = (EntityGhast) ent;
            if (worldObj.getTotalWorldTime() % 30 == 0) {
                ghast.attackEntityFrom(DamageSource.generic, 1);
                ghast.waypointX = ghast.posX;
                ghast.waypointY = ghast.posY;
                ghast.waypointZ = ghast.posZ;
                ghast.courseChangeCooldown = 40;
                if (ghast.isDead) {
                    //NOTE: Potential for bonus ghast tears here. I'm okay with this?
                    buffer.add(new ItemStack(Items.ghast_tear));
                }
            }
            if (!worldObj.isRemote && ghast.getHealth() > 0) {
                ghast.rotationYaw += rand.nextGaussian()*12;
            }
            
        } else if (ent instanceof EntityChicken) {
            EntityChicken chicken = (EntityChicken) ent;
            if (chicken.getHealth() <= 1) {
                chicken.setDead();
                buffer.add(new ItemStack(Items.egg));
            } else {
                chicken.attackEntityFrom(DamageSource.generic, 1);
            }
        } else if (ent instanceof EntityBat) {
            EntityBat bat = (EntityBat) ent;
            bat.attackEntityFrom(DamageSource.generic, 1);
        }
    }
    
    boolean found_player = false;
    @Override
    public boolean isEntityApplicable(Entity entity) {
        if (entity instanceof EntityItem) {
            return true;
        }
        if (target_speed <= 1) {
            return false;
        }
        if (entity instanceof EntityLiving || entity instanceof IProjectile || entity instanceof EntityTNTPrimed || entity instanceof EntityFallingBlock) {
            return true;
        }
        if (FzConfig.fanturpeller_works_on_players && entity instanceof EntityPlayer) {
            EntityPlayer player = (EntityPlayer) entity;
            found_player = true;
            return !player.capabilities.isCreativeMode && !player.isSneaking();
        }
        return false;
        // Falling sand doesn't work too well with this
        // Minecarts seem to just jump back down (and would be too heavy anyways.)
        // Let's try to keep this method light, hmm?
    }

    private void dropItems(Coord coord, ISocketHolder socket) {
        if (dropDelay > 0) {
            dropDelay--;
            return;
        }
        dropDelay = 20*2;
        FzInv back = null;
        if (socket == this) {
            coord.adjust(facing.getOpposite());
            back = InvUtil.openInventory(coord.getTE(IInventory.class), facing);
            coord.adjust(facing);
        } else {
            back = InvUtil.openInventory((Entity) socket, false);
        }
        if (back == null) {
            return;
        }
        ItemStack is = back.pullWithLimit(1);
        back.onInvChanged();
        if (is == null) {
            dropDelay = 20*6;
            return;
        }
        coord.adjust(facing);
        coord.spawnItem(is);
        coord.adjust(facing.getOpposite());
    }
    
    @Override
    protected void onRemove() {
        super.onRemove();
        Coord here = getCoord();
        for (ItemStack item : buffer) {
            InvUtil.spawnItemStack(here, item);
        }
    }
    
    @Override
    public boolean activate(EntityPlayer player, ForgeDirection side) {
        if (!buffer.isEmpty()) {
            new Notice(this, "Buffered output").send(player);
            return false;
        }
        return super.activate(player, side);
    }
    
    @Override
    public void click(EntityPlayer entityplayer) {
        InvUtil.emptyBuffer(entityplayer, buffer, this);
    }
    
    @Override
    public void installedOnServo(ServoMotor servoMotor) {
        super.installedOnServo(servoMotor);
        servoMotor.resizeInventory(3);
    }
    
    @Override
    @SideOnly(Side.CLIENT)
    public void renderItemOnServo(RenderServoMotor render, ServoMotor motor, ItemStack is, float partial) {
        GL11.glPushMatrix();
        GL11.glTranslatef(0, 5F/16F, 0);
        float turn = scaleRotation(NumUtil.interp(prevFanRotation, fanRotation, partial));
        GL11.glRotatef(-turn, 0, 1, 0);
        float s = 9F/16F;
        GL11.glScalef(s, s, s);

        GL11.glPushMatrix();
        GL11.glRotatef(90, 1, 0, 0);
        GL11.glRotatef(45/2F, 0, 0, 1);
        GL11.glTranslatef(0, 0, -2F/16F);
        render.renderItem(is);
        GL11.glPopMatrix();
        
        for (int i = 1; i < motor.getSizeInventory(); i++) {
            is = motor.getStackInSlot(i);
            if (is == null) continue;
            GL11.glPushMatrix();
            float r = 360*i/2 + 58;
            GL11.glRotatef(r, 0, 1, 0);
            GL11.glTranslatef(0, 0, 9F/16F);
            GL11.glRotatef(r, 0, 1, 0);
            /*if (isSucking) {
                GL11.glRotatef(15, 1, 0, 0);
            } else {
                GL11.glRotatef(-15, 1, 0, 0);
            }*/
            render.renderItem(is);
            GL11.glPopMatrix();
        }
        GL11.glPopMatrix();
    }
}
