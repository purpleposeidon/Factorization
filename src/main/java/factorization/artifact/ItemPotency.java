package factorization.artifact;

import factorization.shared.Core;
import factorization.shared.ISensitiveMesh;
import factorization.shared.ItemCraftingComponent;
import factorization.util.ItemUtil;
import factorization.util.StatUtil;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.stats.StatList;
import net.minecraft.stats.StatisticsFile;
import net.minecraft.world.World;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.event.AnvilUpdateEvent;
import net.minecraftforge.event.entity.player.AnvilRepairEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

import javax.annotation.Nonnull;
import java.util.Arrays;
import java.util.List;

public class ItemPotency extends ItemCraftingComponent implements ISensitiveMesh {
    public ItemPotency() {
        super("potencyBottle");
        setMaxStackSize(3);
        Core.loadBus(this);
        Core.tab(this, Core.TabType.ARTIFACT);
    }

    int getDeaths(EntityPlayer player) {
        return StatUtil.load(player, StatList.deathsStat).get();
    }

    @Override
    public void onCreated(ItemStack stack, World world, EntityPlayer _player) {
        EntityPlayerMP player = (EntityPlayerMP) _player;
        NBTTagCompound tag = ItemUtil.getTag(stack);
        String ownerId = player.getGameProfile().getId().toString();
        String ownerName = player.getName();
        StatisticsFile stats = StatUtil.getStatsFile(player);
        if (stats == null) return;
        int deaths = getDeaths(player);

        tag.setString("ownerId", ownerId);
        tag.setString("ownerName", ownerName);
        tag.setInteger("deathId", deaths);
    }

    public static boolean validBottle(ItemStack stack, EntityPlayer player) {
        if (!player.worldObj.isRemote && player instanceof EntityPlayerMP) {
            return checkDamage(stack, (EntityPlayerMP) player) == 0;
        }
        return stack.getItemDamage() == 0;
    }

    public static int checkDamage(ItemStack stack, EntityPlayerMP player) {
        if (stack.getItemDamage() == 1) return 1; // It'll never change back! (Even for other players.)
        NBTTagCompound tag = ItemUtil.getTag(stack);
        String ownerId = player.getGameProfile().getId().toString();
        String ownerName = player.getName();
        int deaths = StatUtil.load(player, StatList.deathsStat).get();
        if (!ownerId.equals(tag.getString("ownerId")) && !ownerName.equals(tag.getString("ownerName"))) return 2;
        if (deaths != tag.getInteger("deathId")) return 1;
        return 0;
    }

    @Override
    public void onUpdate(ItemStack stack, World world, Entity holder, int slotIndex, boolean isHeld) {
        // Does happen each tick. Probaly just in player inventories.
        if (world.isRemote) return;
        if (holder instanceof EntityPlayerMP) {
            final EntityPlayerMP player = (EntityPlayerMP) holder;
            if (!stack.hasTagCompound()) {
                onCreated(stack, world, player);
            }
            stack.setItemDamage(checkDamage(stack, player));
        }
    }

    @SubscribeEvent
    public void anvilRecipe(AnvilUpdateEvent event) {
        ItemStack left = event.left;
        ItemStack right = event.right;
        if (left == null || right == null) return;
        Item enth = Core.registry.entheas;
        Item botl = Items.glass_bottle;
        boolean hasBotl = ItemUtil.is(left, botl) || ItemUtil.is(right, botl);
        boolean hasEnth = ItemUtil.is(left, enth) || ItemUtil.is(right, enth);
        if (!hasBotl || !hasEnth) return;
        event.output = new ItemStack(this);
        event.cost = 30;
        event.materialCost = 1;
    }

    @SubscribeEvent
    public void setupAnvilOutput(AnvilRepairEvent event) {
        // Great job, Forge!
        ItemStack output = event.right;
        ItemStack right = event.left; // The right side's fine
        ItemStack left = event.output;
        if (!ItemUtil.is(output, this)) return;
        // But the left side eats everything, so give the item back
        if (left.stackSize > 1) {
            int free = left.stackSize - 1;
            ItemStack toGive = left.splitStack(free);
            ItemUtil.giveItem(event.entityPlayer, null, toGive, null);
        }

        if (event.entityPlayer.worldObj.isRemote) return;
        this.onUpdate(output, event.entityPlayer.worldObj, event.entityPlayer, 0, false);
        if (!(event.entityPlayer instanceof FakePlayer)) {
            Core.proxy.updatePlayerInventory(event.entityPlayer);
        }
    }

    @Override
    public String getUnlocalizedName(ItemStack stack) {
        String ret = super.getUnlocalizedName(stack);
        final int dmg = stack.getItemDamage();
        if (dmg != 0) {
            if (dmg == 2) return ret + ".foreign";
            return ret + ".wasted";
        }
        return ret;
    }

    @Override
    public String getMeshName(@Nonnull ItemStack is) {
        // We've lost player-sensitivity. :/
        return is.getItemDamage() == 0 ? "potencyBottle" : "potencyBottleWasted";
    }

    @Nonnull
    @Override
    public List<ItemStack> getMeshSamples() {
        return Arrays.asList(new ItemStack(this, 1, 0), new ItemStack(this, 1, 1));
    }
}
