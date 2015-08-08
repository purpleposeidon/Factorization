package factorization.redstone;

import factorization.api.Coord;
import factorization.api.datahelpers.DataHelper;
import factorization.api.datahelpers.Share;
import factorization.common.FactoryType;
import factorization.shared.*;
import factorization.util.InvUtil;
import factorization.util.ItemUtil;
import factorization.util.PlayerUtil;
import net.minecraft.block.Block;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Items;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;
import net.minecraftforge.common.util.ForgeDirection;

import java.io.IOException;
import java.util.ArrayList;

public class TileEntityBlockIntake extends TileEntityCommon implements ICaptureDrops {
    ArrayList<ItemStack> buffer = new ArrayList<ItemStack>();

    @Override
    public BlockClass getBlockClass() {
        return BlockClass.Machine;
    }

    @Override
    public void putData(DataHelper data) throws IOException {
        buffer = data.as(Share.PRIVATE, "buf").putItemArray(buffer);
    }

    @Override
    public FactoryType getFactoryType() {
        return FactoryType.BLOCK_INTAKE;
    }

    @Override
    public void blockUpdateTick(Block myself) {
        push();
    }

    void push() {
        final Coord at = getCoord();
        InvUtil.FzInv inv = InvUtil.openInventory(at.add(ForgeDirection.DOWN).getTE(IInventory.class), ForgeDirection.UP);
        if (inv == null) return;
        ItemStack is;
        while (true) {
            if (buffer.isEmpty()) return;
            is = ItemUtil.normalize(buffer.get(0));
            if (is != null) break;
            buffer.remove(0);
        }
        is = inv.push(is);
        if (is != null) {
            buffer.add(is);
        } else {
            buffer.remove(0);
        }
        if (!buffer.isEmpty()) {
            at.scheduleUpdate(4);
        }
        inv.onInvChanged();
    }

    boolean is_working = false;

    @Override
    public void neighborChanged() {
        if (is_working) return;
        if (!buffer.isEmpty()) return;
        Coord at = getCoord().add(ForgeDirection.UP);
        if (at.isAir()) return;
        int md = at.getMd();
        Block block = at.getBlock();
        if (!block.canCollideCheck(md, false)) return;
        EntityPlayer player = PlayerUtil.makePlayer(at, "BlockIntake");
        DropCaptureHandler.startCapture(this, at, 2);
        ItemStack pick = new ItemStack(Items.diamond_pickaxe);
        player.inventory.mainInventory[0] = pick;
        is_working = true;
        try {
            // (Mostly copied from SocketLacerator. Ahem.)
            NORELEASE.println(at.getHardness());
            {
                boolean canHarvest = false;
                canHarvest = block.canHarvestBlock(player, md);
                if (!canHarvest) return;

                boolean didRemove = removeBlock(player, block, md, at.w, at.x, at.y, at.z);
                if (didRemove) {
                    block.harvestBlock(at.w, player, at.x, at.y, at.z, md);
                }
            }
            block.onBlockHarvested(at.w, at.x, at.y, at.z, md, player);
            if (block.removedByPlayer(at.w, player, at.x, at.y, at.z, true)) {
                block.onBlockDestroyedByPlayer(at.w, at.x, at.y, at.z, md);
                block.harvestBlock(at.w, player, at.x, at.y, at.z, 0);
            }
        } finally {
            is_working = false;
            player.inventory.mainInventory[0] = null;
            DropCaptureHandler.endCapture();
            PlayerUtil.recycleFakePlayer(player);
        }
        push();
    }

    private boolean removeBlock(EntityPlayer thisPlayerMP, Block block, int md, World mopWorld, int x, int y, int z) {
        if (block == null) return false;
        block.onBlockHarvested(mopWorld, x, y, z, md, thisPlayerMP);
        if (block.removedByPlayer(mopWorld, thisPlayerMP, x, y, z, false)) {
            block.onBlockDestroyedByPlayer(mopWorld, x, y, z, md);
            return true;
        }
        return false;
    }

    @Override
    public boolean captureDrops(ArrayList<ItemStack> stacks) {
        buffer.addAll(stacks);
        stacks.clear();
        return true;
    }

    @Override
    protected void onRemove() {
        super.onRemove();
        Coord at = new Coord(this);
        for (ItemStack is : buffer) {
            at.spawnItem(is);
        }
    }
}
