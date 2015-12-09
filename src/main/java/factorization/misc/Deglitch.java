package factorization.misc;

import factorization.util.SpaceUtil;
import net.minecraft.command.CommandBase;
import net.minecraft.command.ICommandSender;
import net.minecraft.entity.player.EntityPlayerMP;
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
        double r = 0.1;
        Random rng = player.worldObj.rand;
        Vec3 at = SpaceUtil.fromEntPos(player).addVector(
                rng.nextDouble() * r,
                rng.nextDouble() * r,
                rng.nextDouble() * r);
        player.setPositionAndUpdate(at.xCoord, at.yCoord, at.zCoord);
    }
}
