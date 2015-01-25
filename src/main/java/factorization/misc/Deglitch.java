package factorization.misc;

import factorization.shared.FzUtil;
import net.minecraft.command.CommandBase;
import net.minecraft.command.ICommandSender;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.network.play.server.S18PacketEntityTeleport;
import net.minecraft.util.Vec3;

import java.util.Random;

public class Deglitch extends CommandBase {
    @Override
    public int getRequiredPermissionLevel() {
        return 0;
    }

    @Override
    public boolean canCommandSenderUseCommand(ICommandSender sender) {
        return sender instanceof EntityPlayerMP;
    }

    @Override
    public String getCommandName() {
        return "deglitch";
    }

    @Override
    public String getCommandUsage(ICommandSender sender) {
        return "/deglitch -- teleport a few cm at cost of food";
    }

    @Override
    public void processCommand(ICommandSender sender, String[] args) {
        EntityPlayerMP player = (EntityPlayerMP) sender;
        player.addExhaustion(3);
        Vec3 at = FzUtil.fromEntPos(player);
        double r = 0.1;
        Random rng = player.worldObj.rand;
        at.xCoord += rng.nextDouble() * r;
        at.yCoord += rng.nextDouble() * r;
        at.zCoord += rng.nextDouble() * r;
        player.setPositionAndUpdate(at.xCoord, at.yCoord, at.zCoord);
    }
}
