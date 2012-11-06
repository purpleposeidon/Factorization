package factorization.common;

import java.io.DataInput;
import java.io.DataOutputStream;
import java.io.IOException;

import net.minecraft.src.DamageSource;
import net.minecraft.src.EntityLiving;
import net.minecraft.src.EntityPlayer;
import net.minecraft.src.IChunkProvider;
import net.minecraft.src.ItemStack;
import net.minecraft.src.NBTBase;
import net.minecraft.src.NBTTagCompound;
import net.minecraft.src.World;


public class FactorizationHack {
    //TODO: All this can be moved elsewhere
    static public DamageSource acidBurn = new AcidDamage("acidburn");
    
    static class AcidDamage extends DamageSource {

        protected AcidDamage(String par1Str) {
            super(par1Str);
            setDamageBypassesArmor();
        }
        
        @Override
        public String getDeathMessage(EntityPlayer player) {
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
        tag.writeNamedTag(tag, output);
    }
}
