package factorization.common;

import java.io.DataInput;
import java.io.DataOutputStream;
import java.io.IOException;

import net.minecraft.util.DamageSource;
import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.world.chunk.IChunkProvider;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTBase;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.world.World;


public class FactorizationHack {
    //TODO: All this can be moved elsewhere
    static public DamageSource acidBurn = new AcidDamage("acidburn");
    
    static class AcidDamage extends DamageSource {

        protected AcidDamage(String par1Str) {
            super(par1Str);
            setDamageBypassesArmor();
        }
        
        @Override
        public String getDeathMessage(EntityLiving player) {
            return player.getEntityName() + " drank acid";
            // TODO translation
            // return super.getDeathMessage(par1EntityPlayer);
        }
        
    }

    static public void damageEntity(EntityLiving ent, DamageSource source, int i) {
        ent.attackEntityFrom(source, i);
    }

    static public ItemStack loadItemStackFromDataInput(DataInput input) throws IOException {
        return ItemStack.loadItemStackFromNBT((NBTTagCompound) NBTBase.readNamedTag(input));
    }

    static public void tagWrite(NBTTagCompound tag, DataOutputStream output) throws IOException {
        NBTBase.writeNamedTag(tag, output);
    }
}
