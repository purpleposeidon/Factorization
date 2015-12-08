package factorization.misc;

import factorization.util.FzUtil;
import net.minecraft.entity.IRangedAttackMob;
import net.minecraft.entity.monster.EntityMob;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.EnumAction;
import net.minecraft.item.ItemStack;
import net.minecraft.util.BlockPos;
import net.minecraft.world.EnumDifficulty;
import net.minecraftforge.event.entity.living.LivingSpawnEvent;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

import java.util.ArrayList;

public class MobEqualizer {
    @SubscribeEvent(priority = EventPriority.LOW)
    public void upgradeMob(LivingSpawnEvent.SpecialSpawn event) {
        EnumDifficulty difficulty = event.world.getDifficulty();
        if (difficulty == null || difficulty.getDifficultyId() <= 1) {
            return;
        }
        if (!(event.entityLiving instanceof EntityMob)) {
            return;
        }
        EntityMob ent = (EntityMob) event.entityLiving;
        if (event.world.rand.nextInt(400) > difficulty.getDifficultyId()) {
            return;
        }
        if (!ent.canPickUpLoot()) return;
        EntityPlayer template = pickNearPlayer(event);
        if (template == null) {
            return;
        }
        int equipment_count = 0;
        ItemStack[] armorCopies = new ItemStack[4];
        ItemStack weaponCopy = null;
        if (event.entity instanceof IRangedAttackMob || event.world.rand.nextBoolean()) {
            for (int i = 0; i < 4; i++) {
                ItemStack is = template.getCurrentArmor(i);
                if (is != null && is.getItem().isValidArmor(is, 3 - i, ent)) {
                    armorCopies[i] = is.copy();
                    equipment_count++;
                }
                //It's okay to leave slots empty
            }
        }
        ArrayList<ItemStack> weapons = new ArrayList<ItemStack>();
        if (!(ent instanceof IRangedAttackMob) || event.world.rand.nextBoolean()) {
            ItemStack orig_weapon = ent.getHeldItem();
            double orig_damage = FzUtil.rateDamage(orig_weapon);
            for (int i = 0; i < 9; i++) {
                ItemStack is = template.inventory.getStackInSlot(i);
                if (is == null) continue;
                if (is.stackSize != 1 || is.getMaxStackSize() != 1) {
                    continue;
                }
                EnumAction act = is.getItemUseAction();
                if (act != EnumAction.BLOCK && act != EnumAction.NONE && act != EnumAction.BOW) {
                    continue;
                }
                double new_damage = FzUtil.rateDamage(is);
                if (new_damage > orig_damage) {
                    weapons.add(is.copy());
                }
            }
        }
        if (!weapons.isEmpty()) {
            weaponCopy = weapons.get(event.world.rand.nextInt(weapons.size())).copy();
            equipment_count++;
        }
        if (equipment_count <= 0) {
            return;
        }
        
        event.setCanceled(true);
        ent.onInitialSpawn(ent.worldObj.getDifficultyForLocation(new BlockPos(event.entity)), null);
        // We need to cancel the event so that we can call this before the below happens
        ent.setCanPickUpLoot(false);
        if (weaponCopy != null) {
            ent.setCurrentItemOrArmor(0, weaponCopy);
        }
        for (int i = 0; i < 4; i++) {
            // Do we want to set the stacksize to 0?
            // Perhaps increase the damage?
            ent.setCurrentItemOrArmor(i + 1, armorCopies[i]);
        }
        for (int i = 0; i < 5; i++) {
            ent.setEquipmentDropChance(i, 0);
        }
    }

    private EntityPlayer pickNearPlayer(LivingSpawnEvent.SpecialSpawn event) {
        //See "Algorithm R (Reservoir sampling)" in "The Art of Computer Programming: Seminumerical Algorithms" by Donald Knuth, Chapter 3.4.2, page 144.
        double maxDistanceSq = Math.pow(16*8, 2);
        EntityPlayer secretary = null;
        int interviews = 0;
        for (EntityPlayer player : event.world.playerEntities) {
            if (player.capabilities.isCreativeMode) {
                continue;
            }
            if (event.entity.getDistanceSqToEntity(player) > maxDistanceSq) {
                continue;
            }
            interviews++;
            int M = event.world.rand.nextInt(interviews) + 1 /* converts from [0,i-1] to [1, i] */;
            if (M <= 1 /* we need only 1 sample */) {
                secretary = player;
            }
        }
        return secretary;
    }
}