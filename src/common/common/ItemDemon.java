package factorization.common;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.src.CreativeTabs;
import net.minecraft.src.Entity;
import net.minecraft.src.EntityLiving;
import net.minecraft.src.EntityPlayer;
import net.minecraft.src.IInventory;
import net.minecraft.src.Item;
import net.minecraft.src.ItemStack;
import net.minecraft.src.NBTTagCompound;
import net.minecraft.src.Potion;
import net.minecraft.src.PotionEffect;
import net.minecraft.src.TileEntity;
import net.minecraft.src.TileEntityChest;
import net.minecraft.src.World;
import net.minecraft.src.WorldProviderHell;
import net.minecraftforge.event.ForgeSubscribe;

public class ItemDemon extends Item {

    public ItemDemon(int par1) {
        super(par1);
        setMaxStackSize(1);
        setTabToDisplayOn(CreativeTabs.tabMisc);
    }

    static void init(ItemStack is) {
        NBTTagCompound tag = is.getTagCompound();
        if (tag == null) {
            tag = new NBTTagCompound();
            is.setTagCompound(tag);
        }
        if (!tag.hasKey("lastbred")) {
            tag.setLong("lastbred", System.currentTimeMillis());
        }
    }

    static boolean isBound(ItemStack is) {
        return is.getItem() == Core.registry.bound_tiny_demon;
    }

    static boolean canBreed(ItemStack is) {
        init(is);
        if (isBound(is)) {
            return false;
        }
        Long last = is.getTagCompound().getLong("lastbred");
        return System.currentTimeMillis() > last + 1000 * 60 * 5;
    }

    static void resetBreeding(World world, ItemStack is) {
        // world may occasionally be null
        init(is);
        is.getTagCompound().setLong("lastbred", System.currentTimeMillis());
    }

    @Override
    public boolean hitEntity(ItemStack is, EntityLiving creature, EntityLiving player) {
        bitePlayer(is, player, false); // hurt both of them! >:D
        bitePlayer(is, creature, true);
        resetBreeding(player.worldObj, is); // getting smashed ruins the mood
        return true;
    }

    @Override
    public int getDamageVsEntity(Entity par1Entity) {
        return 5;
    }

    public void bitePlayer(ItemStack is, EntityLiving player, boolean hardBite) {
        if (is == null || player == null) {
            return;
        }
        if (!(is.getItem() instanceof ItemDemon)) {
            return;
        }
        if (player.worldObj.isRemote) {
            return;
        }
        int poison = 0;
        if (isBound(is)) {
            if (!hardBite) {
                return;
            }
            if (itemRand.nextInt(3) == 0) {
                poison = itemRand.nextInt(2);
            }
        } else {
            poison = itemRand.nextInt(5) + 2;
            FactorizationHack.damageEntity(player, FactorizationHack.imp_bite, 3);
        }
        if (hardBite) {
            poison += 3 + itemRand.nextInt(12);
        }
        if (poison > 0) {
            poison *= 2;
            player.addPotionEffect(new PotionEffect(Potion.poison.id, poison * 20, 1));
        }
        if (is.animationsToGo <= 0) {
            is.animationsToGo += 5;
            if (is.animationsToGo > 50) {
                is.animationsToGo = 10;
            }
        }
        Sound.demonSqueek.playAt(player);
    }

    public void playerHolding(ItemStack is, EntityLiving player) {
        if (is == null || !(is.getItem() instanceof ItemDemon)) {
            return;
        }
        if (isBound(is)) {
            return;
        }
        if (itemRand.nextInt(50) == 0) {
            bitePlayer(is, player, true);
        }
    }

    @Override
    public String getItemName() {
        if (this == Core.registry.bound_tiny_demon) {
            return "item.boundtinydemon";
        }
        return "item.tinydemon";
    }

    @Override
    public String getItemNameIS(ItemStack is) {
        return getItemName();
    }

    @Override
    // -- DAMN YOU, SERVER!
    public void addInformation(ItemStack is, List list) {
        if (isBound(is)) {
            list.add("Less likely to bite");
        } else {
            list.add("Beware its poisonous fangs!");
        }
        //Core.brand(list); -- I hereby disclaim all responsibility for this thing!
    }

    @Override
    public ItemStack onItemRightClick(ItemStack is, World world, EntityPlayer player) {
        bitePlayer(is, player, true);
        return is;
    }

    @Override
    public String getTextureFile() {
        return Core.texture_file_item;
    }

    @Override
    // -- can't override due to the stupidly typical reason.
    public int getIconFromDamage(int par1) {
        if (this == Core.registry.bound_tiny_demon) {
            return (2 * 16) + 1;
        }
        return 2 * 16;
    }

    static class ChestEnviron {
        IInventory chest;
        TileEntityChest realChest;
        int demons = 0, worts = 0, freespace = 0, fertile = 0;
        boolean has_weird_stuff = false;

        public ChestEnviron(TileEntityChest realChest, IInventory chest) {
            this.realChest = realChest;
            this.chest = chest;
            collectInfo();
        }

        void collectInfo() {
            for (int i = 0; i < chest.getSizeInventory(); i++) {
                ItemStack is = chest.getStackInSlot(i);
                if (is == null) {
                    freespace += 1;
                    continue;
                }
                else if (is.getItem() instanceof ItemDemon) {
                    demons += 1;
                    if (canBreed(is)) {
                        fertile += 1;
                    }
                }
                else if (is.getItem() == Item.netherStalkSeeds) {
                    worts += is.stackSize;
                }
                else {
                    has_weird_stuff = true;
                }
            }
        }

        boolean cantHost() {
            return has_weird_stuff || demons > 8 || worts < 24 * (demons + 1.5) || freespace == 0;
        }

        void updateDemonActivity() {
            // eat worts & breed
            if (demons <= 0 || worts <= 0) {
                return;
            }
            worts -= demons;
            int noms = (int) (demons * 1.2);
            for (int i = 0; i < chest.getSizeInventory(); i++) {
                ItemStack is = chest.getStackInSlot(i);
                if (is == null || is.getItem() != Item.netherStalkSeeds) {
                    continue;
                }
                is.stackSize -= noms;
                if (is.stackSize <= 0) {
                    noms = -is.stackSize;
                    chest.setInventorySlotContents(i, null);
                    freespace += 1;
                } else {
                    noms = 0;
                    break;
                }
                if (noms <= 0) {
                    break;
                }
            }

            if (fertile > 2 && cantHost() && itemRand.nextInt(7) == 0) {
                int found = 0;
                for (int i = 0; i < chest.getSizeInventory(); i++) {
                    ItemStack is = chest.getStackInSlot(i);
                    if (is == null) {
                        continue;
                    }
                    if (!(is.getItem() == Core.registry.tiny_demon)) {
                        continue;
                    }
                    if (canBreed(is)) {
                        resetBreeding(realChest.worldObj, is);
                        found += 1;
                        if (found == 2) {
                            break;
                        }
                    }
                }
                addDemon();
            }
        }

        void addDemon() {
            if (freespace <= 0) {
                return;
            }
            int chosen = -1;
            int i = 0;
            for (int slot = 0; slot < chest.getSizeInventory(); slot++) {
                ItemStack is = chest.getStackInSlot(slot);
                if (is != null) {
                    continue;
                }
                if (i == 0 || itemRand.nextInt(i) == 0) {
                    chosen = slot;
                }
                i += 1;

            }
            if (chosen != -1) {
                chest.setInventorySlotContents(chosen, new ItemStack(Core.registry.tiny_demon));
                Core.proxy.pokeChest(realChest);
            }
        }
    }

    public static void spawnDemons(World world) {
        // find a chest in world to stick a demon into
        if (!(world.provider instanceof WorldProviderHell)) {
            return;
        }
        //XXX TODO: We could check if the chests were in a Hell biome instead...

        ChestEnviron chosen = null;
        int i = 0;
        if (world.loadedTileEntityList instanceof ArrayList) {
            ArrayList<TileEntity> tes = (ArrayList<TileEntity>) world.loadedTileEntityList;
            for (int index = 0; index < tes.size(); index++) {
                TileEntity ent = tes.get(index);
                if (!(ent instanceof TileEntityChest)) {
                    continue;
                }
                IInventory chest = FactorizationUtil.openDoubleChest((TileEntityChest) ent);
                //XXX What happens here if the ent's actually a subclass of TEChest?
                if (chest == null) {
                    continue;
                }
                ChestEnviron env = new ChestEnviron((TileEntityChest) ent, chest);

                env.updateDemonActivity();
                if (env.cantHost()) {
                    continue;
                }

                if (i == 0 || itemRand.nextInt(i) == 0) {
                    chosen = env;
                }
                i += 1;
            }
        }

        if (chosen != null) {
            chosen.addDemon();
        }
    }
}
