package factorization.shared;

import factorization.api.Mat;
import factorization.fzds.FZDSCommand;
import factorization.fzds.interfaces.IDimensionSlice;
import factorization.util.NORELEASE;
import factorization.util.SpaceUtil;
import net.minecraft.command.CommandBase;
import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.entity.Entity;
import net.minecraft.util.BlockPos;
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
        IDimensionSlice idc = FZDSCommand.getSelection();
        Mat mat = idc.getReal2Shadow();
        /*mat = Mat.mul(
                Mat.trans(idc.getCorner().toVector()),
                Mat.trans(idc.getRotationalCenterOffset()),
                Mat.rotate(idc.getRotation()),
                Mat.trans(SpaceUtil.fromEntPos(idc)).invert(),
                Mat.IDENTITY
        );*/
        /*
        Vec3 buffer = realVector.subtract(SpaceUtil.fromEntPos(this));
        rotation.applyRotation(buffer).addVector(cornerMin.x).add(centerOffset);
        */

        Vec3 me = SpaceUtil.fromEntPos((Entity) sender);
        Vec3 matRet = mat.mul(me);
        Vec3 oldRet = idc.real2shadow(SpaceUtil.copy(me));
        NORELEASE.println("\n\n\n");
        NORELEASE.println(mat);
        NORELEASE.println("Mat: " + matRet);
        NORELEASE.println("Old: " + oldRet);
        Vec3 error = matRet.subtract(oldRet);
        NORELEASE.println("Error: " + error.lengthVector() + "   " + error);
    }
}
