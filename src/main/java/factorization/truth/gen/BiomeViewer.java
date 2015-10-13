package factorization.truth.gen;

import factorization.truth.api.IDocGenerator;
import factorization.truth.api.ITypesetter;
import factorization.truth.api.TruthError;
import net.minecraft.block.Block;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.world.biome.BiomeDecorator;
import net.minecraft.world.biome.BiomeGenBase;
import net.minecraftforge.common.BiomeDictionary;
import net.minecraftforge.common.BiomeDictionary.Type;

import java.util.ArrayList;

public class BiomeViewer implements IDocGenerator {

    @Override
    public void process(ITypesetter out, String arg) throws TruthError {
        ArrayList<Integer> free = new ArrayList<Integer>();
        BiomeGenBase[] biomeGenArray = BiomeGenBase.getBiomeGenArray();
        for (int i = 0; i < biomeGenArray.length; i++) {
            BiomeGenBase biome = biomeGenArray[i];
            if (biome == null) {
                free.add(i);
                continue;
            }
            out.write("\\newpage\\title{#" + biome.biomeID + " " + biome.biomeName + "}");
            out.write("\\nl Temperature: " + biome.temperature);
            out.write("\\nl Humidity: " + biome.rainfall);
            out.write(String.format("\\nl Color: #%06X", biome.color));
            if (biome.waterColorMultiplier != 0xFFFFFF) {
                out.write(String.format("\\nl Water Tint: #%06X", biome.waterColorMultiplier));
            }
            out.write("\\nl Blocks: ");
            out.write(new ItemStack(biome.topBlock));
            out.write(new ItemStack(biome.fillerBlock));
            
            {
                final BiomeDecorator dec = biome.theBiomeDecorator;
                out.write("\\nl\\nl");
                
                feature(out, dec.waterlilyPerChunk, Blocks.waterlily);
                feature(out, dec.treesPerChunk, Blocks.sapling);
                feature(out, dec.flowersPerChunk, Blocks.red_flower);
                feature(out, dec.grassPerChunk, Blocks.tallgrass);
                feature(out, dec.deadBushPerChunk, Blocks.deadbush);
                feature(out, dec.mushroomsPerChunk, Blocks.brown_mushroom);
                feature(out, dec.reedsPerChunk, Blocks.reeds);
                feature(out, dec.cactiPerChunk, Blocks.cactus);
                feature(out, dec.sandPerChunk + dec.sandPerChunk2, Blocks.sand);
                feature(out, dec.clayPerChunk, Blocks.clay);
                feature(out, dec.bigMushroomsPerChunk, Blocks.red_mushroom_block);
                if (dec.generateLakes) {
                    out.write(new ItemStack(Items.water_bucket));
                }
                
                out.write("\\nl");
            }
            
            if (biome.canSpawnLightningBolt()) {
                out.write("\\nl Rainy");
            }
            
            if (biome.getEnableSnow()) {
                out.write("\\nl Snowy");
            }
            
            
            BiomeDictionary.Type[] types = BiomeDictionary.getTypesForBiome(biome);
            if (types == null || types.length == 0) continue;
            out.write("\\nl \\nl");
            for (Type t : types) {
                out.write(" " + t);
            }
        }
        out.write("\\newpage\\title{Free IDs}\\nl");
        if (free.isEmpty()) {
            out.write("There are no free biome IDs!");
        } else {
            ArrayList<Integer> contig = new ArrayList<Integer>();
            int last = -100;
            boolean firstContig = false;
            for (Integer i : free) {
                if (i == last + 1) {
                    if (firstContig) {
                        firstContig = false;
                    } else {
                        contig.remove(contig.size() - 1);
                    }
                    contig.add(-i);
                } else {
                    contig.add(i);
                    firstContig = true;
                }
                last = i;
            }
            for (Integer i : contig) {
                if (i < 0) {
                    out.write("to " + (-i) + "\\nl");
                } else {
                    out.write(i + " ");
                }
            }
        }
    }
    
    void feature(ITypesetter out, int val, Block symbol) {
        if (val <= 0) return;
        if (val > 99) val = 99;
        Item it = symbol.getItem(null, 0, 0, 0);
        ItemStack is = new ItemStack(it);
        if (symbol == Blocks.tallgrass) {
            is = new ItemStack(Blocks.tallgrass, 0, 1);
        }
        is.stackSize = val;
        out.write(is);
    }

}
