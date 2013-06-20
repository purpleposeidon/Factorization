package factorization.api;

import java.util.ArrayList;
import java.util.Arrays;

import cpw.mods.fml.common.registry.GameRegistry;

import net.minecraft.block.Block;
import net.minecraft.item.ItemStack;
import net.minecraftforge.oredict.OreDictionary;
import factorization.common.Core;
import factorization.common.FactorizationUtil;

public class ChargeMetalBlockConductance {
    //static HashMap<Integer, ArrayList<Integer>> validBlocks = new HashMap();
    static boolean[][] validBlocks = new boolean[Block.blocksList.length][];
    public static ArrayList<ItemStack> excludeOres = new ArrayList<ItemStack>();
    
    public static void setup() {
        if (!Core.invasiveCharge) {
            return;
        }
        GameRegistry.registerTileEntity(InvasiveCharge.class, "factorization.invasiveCharge");
        
        for (String metalName : Arrays.asList(
                "Copper", "Tin", "Lead", "Gold", "Silver", /* standard blocks */
                "Emerald" /* a joke. */, 
                "Aluminum", "Platinum", "Zinc", /* weird mod metals */
                "AluminumBrass", "Cobalt", "Ardite", "Manyullyn" /* tcons metalss */ )) {
            subIngots: for (ItemStack is : OreDictionary.getOres("block" + metalName)) {
                int metadata = is.getItemDamage();
                if (is.hasTagCompound() || metadata < 0 || metadata > 16) {
                    continue;
                }
                if (is.itemID >= Block.blocksList.length || is.itemID <= 0) {
                    continue;
                }
                Block b = Block.blocksList[is.itemID];
                if (b == null) {
                    continue;
                }
                if (b.hasTileEntity(metadata)) {
                    continue;
                }
                for (ItemStack exclude : excludeOres) {
                    if (FactorizationUtil.couldMerge(exclude, is)) {
                        continue subIngots;
                    }
                }
                put(b.blockID, metadata);
            }
        }
        
    }
    
    static void put(int id, int md) {
        boolean[] mds = validBlocks[id];
        if (mds == null) {
            validBlocks[id] = mds = new boolean[16];
        }
        mds[md] = true;
    }
    
    public static void taintBlock(Coord c) {
        /*int blockID = c.getId(), md = c.getMd();
        if (validBlocks[blockID] == null || !validBlocks[blockID][md]) {
            return;
        }
        if (c.getTE() == null) {
            InvasiveCharge te = new InvasiveCharge();
            //te.validate();
            te.initialize(blockID, md);
            c.setTE(te);
        }*/
    }
}
