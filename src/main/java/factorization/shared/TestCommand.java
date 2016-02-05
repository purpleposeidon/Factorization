package factorization.shared;

import factorization.api.Mat;
import factorization.fzds.FZDSCommand;
import factorization.fzds.interfaces.IDeltaChunk;
import factorization.util.FzUtil;
import factorization.util.NORELEASE;
import factorization.util.SpaceUtil;
import net.minecraft.command.CommandBase;
import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.entity.Entity;
import net.minecraft.util.BlockPos;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.Vec3;

import java.util.List;

public class TestCommand extends CommandBase {
    @Override
    public String getCommandName() {
        return "test";
    }

    @Override
    public String getCommandUsage(ICommandSender sender) {
        return "/test ??? (this is a generic debug stub for FZ)";
    }

    @Override
    public List<String> addTabCompletionOptions(ICommandSender sender, String[] args, BlockPos pos) {
        return super.addTabCompletionOptions(sender, args, pos);
    }

    @Override
    public void processCommand(ICommandSender sender, String[] args) throws CommandException {
        IDeltaChunk idc = FZDSCommand.getSelection();
        Mat mat = idc.getReal2Shadow();
        Vec3 me = SpaceUtil.fromEntPos((Entity) sender);
        Vec3 matRet = mat.mul(me);
        Vec3 oldRet = idc.real2shadow(SpaceUtil.copy(me));
        NORELEASE.println(mat);
        NORELEASE.println("Mat: " + matRet);
        NORELEASE.println("Old: " + oldRet);
    }
}
