package factorization.sockets.fanturpeller;

import factorization.api.*;
import factorization.api.datahelpers.DataHelper;
import factorization.api.datahelpers.IDataSerializable;
import factorization.api.datahelpers.Share;
import factorization.common.FactoryType;
import factorization.servo.RenderServoMotor;
import factorization.servo.ServoMotor;
import factorization.shared.Core;
import factorization.shared.NetworkFactorization.MessageType;
import factorization.sockets.ISocketHolder;
import factorization.sockets.TileEntitySocketBase;
import factorization.util.NumUtil;
import io.netty.buffer.ByteBuf;
import net.minecraft.block.Block;
import net.minecraft.init.Blocks;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ITickable;
import net.minecraftforge.fluids.IFluidBlock;
import net.minecraftforge.fluids.IFluidHandler;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.lwjgl.opengl.GL11;

import java.io.IOException;

public abstract class SocketFanturpeller extends TileEntitySocketBase implements IChargeConductor, ITickable {
    Charge charge = new Charge(this);
    boolean isSucking = true;
    byte target_speed = 1;
    float fanω;
    boolean dirty = false;

    transient float fanRotation, prevFanRotation;

    @Override
    public IDataSerializable serialize(String prefix, DataHelper data) throws IOException {
        charge = data.as(Share.PRIVATE, "charge").putIDS(charge);
        isSucking = data.as(Share.MUTABLE, "suck").putBoolean(isSucking);
        target_speed = data.as(Share.MUTABLE, "target_speed").putByte(target_speed);
        if (target_speed < 0) target_speed = 0;
        if (target_speed > 3) target_speed = 3;
        fanω = data.as(Share.VISIBLE, "fanw").putFloat(fanω);
        return this;
    }
    
    @Override
    public ItemStack getCreatingItem() {
        return new ItemStack(Core.registry.fan);
    }
    
    @Override
    public FactoryType getParentFactoryType() {
        return FactoryType.SOCKET_BARE_MOTOR;
    }
    
    @Override
    public String getInfo() {
        return null;
    }

    @Override
    public Charge getCharge() {
        return charge;
    }

    @Override
    public void update() {
        charge.update();
    }
    
    boolean isLiquid(Coord at) {
        final Block block = at.getBlock();
        if (block == Blocks.water || block == Blocks.flowing_water || block == Blocks.lava || block == Blocks.flowing_lava) {
            return at.getMd() == 0;
        }
        if (block instanceof IFluidBlock) {
            IFluidBlock ifb = (IFluidBlock) block;
            return ifb.canDrain(worldObj, at.toBlockPos());
        }
        return false;
    }

    boolean hasTank(Coord at) {
        return at.getTE(IFluidHandler.class) != null;
    }

    boolean hasInv(Coord at) {
        return at.getTE(IInventory.class) != null;
    }

    boolean isClear(Coord at) {
        return at.isReplacable() && !isLiquid(at);
    }
    
    boolean noCollision(Coord at) {
        return at.getCollisionBoundingBox() == null;
    }
    
    @Override
    protected void replaceWith(TileEntitySocketBase baseReplacement, ISocketHolder socket) {
        if (baseReplacement instanceof SocketFanturpeller) {
            SocketFanturpeller replacement = (SocketFanturpeller) baseReplacement;
            if (!isSafeToDiscard()) {
                if (replacement instanceof PumpLiquids && this instanceof PumpLiquids) {
                    // Ugly! :|
                    PumpLiquids old = (PumpLiquids) this;
                    PumpLiquids rep = (PumpLiquids) replacement;
                    rep.buffer = old.buffer;
                } else {
                    return;
                }
            }
            replacement.isSucking = isSucking;
            replacement.target_speed = target_speed;
            replacement.fanω = fanω;
            replacement.fanRotation = fanRotation;
            replacement.prevFanRotation = prevFanRotation;
            replacement.charge = charge;
        }
        super.replaceWith(baseReplacement, socket);
    }
    
    float getTargetSpeed() {
        if (!shouldFeedJuice()) return 0;
        return target_speed*10;
    }
    
    boolean shouldDoWork() {
        if (target_speed == 0) return false;
        int direction = (isSucking ? -1 : 1);
        float ts = getTargetSpeed();
        if (Math.signum(fanω) != direction) return false;
        float ω = Math.abs(fanω);
        if (ω >= ts) return true;
        return false;
        /*if (ts > ω + 10) return false;
        return (ts - ω)/10.0F > rand.nextFloat(); */
    }
    
    int getRequiredCharge() {
        return 0;
    }
    
    @Override
    public final void genericUpdate(ISocketHolder socket, Coord coord, boolean powered) {
        prevFanRotation = fanRotation;
        fanturpellerUpdate(socket, coord, powered);
        if (!worldObj.isRemote) {
            final int need = getRequiredCharge();
            float orig_speed = fanω;
            if (powered || !shouldFeedJuice()) {
                fanω *= 0.95;
            } else if (need > 0) {
                final float ts = getTargetSpeed() * (isSucking ? -1 : 1);
                if (!socket.extractCharge(need)) {
                    fanω *= 0.9;
                } else if (Math.abs(fanω) > Math.abs(ts)) { // we've been switched to a slower speed
                    fanω = (fanω*9 + ts)/10;
                    if (Math.abs(fanω) < Math.abs(ts)) {
                        fanω = ts;
                    }
                } else if ((isSucking && ts < fanω) || (!isSucking && ts > fanω)) {
                    fanω += Math.signum(ts);
                    if (fanω > Math.abs(ts)) {
                        fanω = ts;
                    }
                }
            }
            if (dirty || orig_speed != fanω) {
                socket.sendMessage(MessageType.FanturpellerSpeed, fanω, isSucking, target_speed);
                dirty = false;
            }
        }
        fanRotation += fanω;
    }
    
    protected boolean shouldFeedJuice() {
        return false;
    }
    
    protected void fanturpellerUpdate(ISocketHolder socket, Coord coord, boolean powered) {
        fanω *= 0.95F;
    }
    
    protected boolean isSafeToDiscard() {
        return true;
    }

    protected float scaleRotation(float rotation) {
        return rotation;
    }
    
    @Override
    @SideOnly(Side.CLIENT)
    public void renderTesr(ServoMotor motor, float partial) {
        float d = 0.5F;
        GL11.glTranslatef(d, d, d);
        Quaternion.fromOrientation(FzOrientation.fromDirection(facing.getOpposite())).glRotate();
        float turn = scaleRotation(NumUtil.interp(prevFanRotation, fanRotation, partial));
        float dr = Math.abs(scaleRotation(fanRotation) - scaleRotation(prevFanRotation));
        GL11.glRotatef(turn, 0, 1, 0);
        float sd = motor == null ? -2F/16F : 3F/16F;
        GL11.glTranslatef(0, sd, 0);
        
        
        float s = 12F/16F;
        if (motor != null) {
            s = 10F/16F;
            GL11.glTranslatef(0, -3F/16F, 0);
        }
        GL11.glScalef(s, 1, s);
        float count = dr/60;
        if (count > 2) {
            count = 2;
        }
        if (count < 1) {
            count = 1;
        }
        //TileEntityGrinderRender.renderGrindHead();
        for (float i = 0; i < count; i++) {
            if (i > 0) {
                GL11.glRotatef(45F, 0, 1, 0);
                GL11.glTranslatef(0, -1F/64F, 0);
            }
            GL11.glPushMatrix();
            GL11.glRotatef(90, 1, 0, 0);
            GL11.glTranslatef(-0.5F, -0.5F, 0);
            FactorizationBlockRender.renderItemIIcon(Core.registry.fan.getIconFromDamage(0));
            GL11.glPopMatrix();
        }
    }
    
    @Override
    @SideOnly(Side.CLIENT)
    public boolean handleMessageFromServer(MessageType messageType, ByteBuf input) throws IOException {
        if (super.handleMessageFromServer(messageType, input)) {
            return true;
        }
        if (messageType == MessageType.FanturpellerSpeed) {
            fanω = input.readFloat();
            isSucking = input.readBoolean();
            target_speed = input.readByte();
            return true;
        }
        return false;
    }
    
    @Override
    @SideOnly(Side.CLIENT)
    public void renderItemOnServo(RenderServoMotor render, ServoMotor motor, ItemStack is, float partial) {
        GL11.glPushMatrix();
        
        GL11.glTranslatef(8F/16F, 1F/16F, 0);
        GL11.glRotatef(90, 0, 1, 0);
        
        render.renderItem(is);
        GL11.glPopMatrix();
    }
}
