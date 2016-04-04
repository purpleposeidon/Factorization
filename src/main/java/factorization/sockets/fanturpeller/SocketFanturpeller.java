package factorization.sockets.fanturpeller;

import factorization.api.Coord;
import factorization.api.FzOrientation;
import factorization.api.Quaternion;
import factorization.api.datahelpers.DataHelper;
import factorization.api.datahelpers.IDataSerializable;
import factorization.api.datahelpers.Share;
import factorization.api.energy.EnergyCategory;
import factorization.api.energy.IWorker;
import factorization.api.energy.IWorkerContext;
import factorization.api.energy.WorkUnit;
import factorization.common.FactoryType;
import factorization.net.StandardMessageType;
import factorization.servo.RenderServoMotor;
import factorization.servo.ServoMotor;
import factorization.shared.Core;
import factorization.shared.FzModel;
import factorization.sockets.ISocketHolder;
import factorization.sockets.TileEntitySocketBase;
import factorization.util.NumUtil;
import io.netty.buffer.ByteBuf;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ITickable;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.lwjgl.opengl.GL11;

import java.io.IOException;

public abstract class SocketFanturpeller extends TileEntitySocketBase implements IWorker<IWorkerContext>, ITickable {
    boolean isSucking = true;
    byte target_speed = 1;
    float fanω;
    boolean dirty = false;
    short powerTicks = 0;
    short POWER_TICKS_PER_UNIT = 20 * 20;

    transient float fanRotation, prevFanRotation;

    @Override
    public Accepted accept(IWorkerContext context, WorkUnit unit, boolean simulate) {
        if (unit.category != EnergyCategory.ELECTRIC && unit.category != EnergyCategory.ROTATIONAL) return Accepted.NEVER;
        if (powerTicks > POWER_TICKS_PER_UNIT) return Accepted.LATER;
        if (!simulate) {
            powerTicks += POWER_TICKS_PER_UNIT;
        }
        return Accepted.NOW;
    }

    @Override
    public IDataSerializable serialize(String prefix, DataHelper data) throws IOException {
        isSucking = data.as(Share.MUTABLE, "suck").putBoolean(isSucking);
        target_speed = data.as(Share.MUTABLE, "target_speed").putByte(target_speed);
        if (target_speed < 0) target_speed = 0;
        if (target_speed > 3) target_speed = 3;
        fanω = data.as(Share.VISIBLE, "fanw").putFloat(fanω);
        powerTicks = data.as(Share.PRIVATE, "powerTicks").putShort(powerTicks);
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
            replacement.powerTicks = powerTicks;
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
        return ω >= ts;
    }

    @Override
    public final void genericUpdate(ISocketHolder socket, Coord coord, boolean powered) {
        prevFanRotation = fanRotation;
        fanturpellerUpdate(socket, coord, powered);
        fanRotation += fanω;
        if (worldObj.isRemote) {
            return;
        }
        float orig_speed = fanω;
        if (powered || !shouldFeedJuice()) {
            fanω *= 0.95;
        } else {
            final float targetSpeed = getTargetSpeed() * (isSucking ? -1 : 1);
            if (powerTicks > 0) {
                powerTicks--;
            }
            if (powerTicks <= 0) {
                fanω *= 0.9;
                powerTicks = 0;
            } else if (Math.abs(fanω) > Math.abs(targetSpeed)) { // we've been switched to a slower speed
                fanω = (fanω*9 + targetSpeed)/10;
                if (Math.abs(fanω) < Math.abs(targetSpeed)) {
                    fanω = targetSpeed;
                }
            } else if ((isSucking && targetSpeed < fanω) || (!isSucking && targetSpeed > fanω)) {
                fanω += Math.signum(targetSpeed);
                if (fanω > Math.abs(targetSpeed)) {
                    fanω = targetSpeed;
                }
            }
        }
        if (dirty || orig_speed != fanω) {
            socket.sendMessage(StandardMessageType.SetSpeed, fanω, isSucking, target_speed);
            dirty = false;
        }
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
            fan.draw();
            GL11.glPopMatrix();
        }
    }

    static FzModel fan = new FzModel("socket/fan");
    
    @Override
    @SideOnly(Side.CLIENT)
    public boolean handleMessageFromServer(Enum messageType, ByteBuf input) throws IOException {
        if (super.handleMessageFromServer(messageType, input)) {
            return true;
        }
        if (messageType == StandardMessageType.SetSpeed) {
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
