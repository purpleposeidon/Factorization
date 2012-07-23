package net.minecraft.src;

import java.awt.BorderLayout;
import java.awt.Color;
import java.util.Calendar;
import java.util.Date;
import java.util.Random;
import java.util.Vector;

import javax.swing.JFrame;
import javax.swing.JTextPane;

import org.lwjgl.opengl.Display;

import net.minecraft.client.Minecraft;

public class EntityPlayerSP extends EntityPlayer
{
   public MovementInput movementInput;
   protected Minecraft mc;

   /**
    * Used to tell if the player pressed forward twice. If this is at 0 and it's pressed (And they are allowed to
    * sprint, aka enough food on the ground etc) it sets this to 7. If it's pressed and it's greater than 0 enable
    * sprinting.
    */
   protected int sprintToggleTimer;

   /** Ticks left before sprinting is disabled. */
   public int sprintingTicksLeft;
   public float renderArmYaw;
   public float renderArmPitch;
   public float prevRenderArmYaw;
   public float prevRenderArmPitch;
   private MouseFilter field_21903_bJ;
   private MouseFilter field_21904_bK;
   private MouseFilter field_21902_bL;

   public PlayerHelper ph;
   public boolean multiplayer;
   public boolean phexists;
   public static Object MESSAGESHOWN;
   public static Object STARTUP;
   public String curmcversion;
   public static final String MCVERSION = "1.2.5";
   public static final SPCVersion SPCVERSION = new SPCVersion("Single Player Commands","3.2.2",new Date(1333630063890L)); // 2012-04-05 22:47:43
   public Vector<String> missingRequiredClasses;
   public Vector<String> missingOptionalClasses;

   public EntityPlayerSP(Minecraft par1Minecraft, World par2World, Session par3Session, int par4)
   {
      super(par2World);
      sprintToggleTimer = 0;
      sprintingTicksLeft = 0;
      field_21903_bJ = new MouseFilter();
      field_21904_bK = new MouseFilter();
      field_21902_bL = new MouseFilter();
      mc = par1Minecraft;
      dimension = par4;

      if (par3Session != null && par3Session.username != null && par3Session.username.length() > 0)
      {
         skinUrl = (new StringBuilder()).append("http://s3.amazonaws.com/MinecraftSkins/").append(par3Session.username).append(".png").toString();
      }

      username = par3Session.username;

      initPlayerHelper(par3Session);
      phexists = true;
   }

   /**
    * Tries to moves the entity by the passed in displacement. Args: x, y, z
    */
   public void moveEntity(double par1, double par3, double par5)
   {
      if (canRunSPC() && ph.moveplayer && !ph.movecamera && mc.renderViewEntity instanceof SPCEntityCamera) {
         ((SPCEntityCamera)mc.renderViewEntity).setCamera(0, 0, 0,ph.freezecamyaw, ph.freezecampitch);
      } else if (canRunSPC() && ph.noClip) {
         posX += par1;
         posY += par3;
         posZ += par5;
         return;
      } else if (canRunSPC() && mc.renderViewEntity instanceof SPCEntityCamera) {
         ((SPCEntityCamera)mc.renderViewEntity).setCamera(par1, par3, par5,rotationYaw, rotationPitch);
         return;
      }
      super.moveEntity(par1, par3, par5);
   }

   public void updateEntityActionState()
   {
      super.updateEntityActionState();
      moveStrafing = movementInput.moveStrafe;
      moveForward = movementInput.moveForward;
      isJumping = movementInput.jump;
      prevRenderArmYaw = renderArmYaw;
      prevRenderArmPitch = renderArmPitch;
      renderArmPitch += (double)(rotationPitch - renderArmPitch) * 0.5D;
      renderArmYaw += (double)(rotationYaw - renderArmYaw) * 0.5D;
   }

   /**
    * Returns whether the entity is in a local (client) world
    */
   protected boolean isClientWorld()
   {
      return true;
   }

   /**
    * Called frequently so the entity can update its state every tick as required. For example, zombies and skeletons
    * use this to react to sunlight and start to burn.
    */
   public void onLivingUpdate()
   {
      if (canRunSPC() && ph.sprinting) {
         setSprinting(true);
      } else if (sprintingTicksLeft > 0)
      {
         sprintingTicksLeft--;

         if (sprintingTicksLeft == 0)
         {
            setSprinting(false);
         }
      }

      if (sprintToggleTimer > 0)
      {
         sprintToggleTimer--;
      }

      if (mc.playerController.func_35643_e())
      {
         posX = posZ = 0.5D;
         posX = 0.0D;
         posZ = 0.0D;
         rotationYaw = (float)ticksExisted / 12F;
         rotationPitch = 10F;
         posY = 68.5D;
         return;
      }

      if (!mc.statFileWriter.hasAchievementUnlocked(AchievementList.openInventory))
      {
         mc.guiAchievement.queueAchievementInformation(AchievementList.openInventory);
      }

      prevTimeInPortal = timeInPortal;

      if (inPortal)
      {
         if (!worldObj.isRemote && ridingEntity != null)
         {
            mountEntity(null);
         }

         if (mc.currentScreen != null)
         {
            mc.displayGuiScreen(null);
         }

         if (timeInPortal == 0.0F)
         {
            mc.sndManager.playSoundFX("portal.trigger", 1.0F, rand.nextFloat() * 0.4F + 0.8F);
         }

         timeInPortal += 0.0125F;

         if (timeInPortal >= 1.0F)
         {
            timeInPortal = 1.0F;

            if (!worldObj.isRemote)
            {
               timeUntilPortal = 10;
               mc.sndManager.playSoundFX("portal.travel", 1.0F, rand.nextFloat() * 0.4F + 0.8F);
               byte byte0 = 0;

               if (dimension == -1)
               {
                  byte0 = 0;
               }
               else
               {
                  byte0 = -1;
               }

               mc.usePortal(byte0);
               triggerAchievement(AchievementList.portal);
            }
         }

         inPortal = false;
      }
      else if (isPotionActive(Potion.confusion) && getActivePotionEffect(Potion.confusion).getDuration() > 60)
      {
         timeInPortal += 0.006666667F;

         if (timeInPortal > 1.0F)
         {
            timeInPortal = 1.0F;
         }
      }
      else
      {
         if (timeInPortal > 0.0F)
         {
            timeInPortal -= 0.05F;
         }

         if (timeInPortal < 0.0F)
         {
            timeInPortal = 0.0F;
         }
      }

      if (timeUntilPortal > 0)
      {
         timeUntilPortal--;
      }

      boolean flag = movementInput.jump;
      float f = 0.8F;
      boolean flag1 = movementInput.moveForward >= f;
      movementInput.updatePlayerMoveState();

      if (isUsingItem())
      {
         movementInput.moveStrafe *= 0.2F;
         movementInput.moveForward *= 0.2F;
         sprintToggleTimer = 0;
      }

      if (movementInput.sneak && ySize < 0.2F)
      {
         ySize = 0.2F;
      }

      pushOutOfBlocks(posX - (double)width * 0.34999999999999998D, boundingBox.minY + 0.5D, posZ + (double)width * 0.34999999999999998D);
      pushOutOfBlocks(posX - (double)width * 0.34999999999999998D, boundingBox.minY + 0.5D, posZ - (double)width * 0.34999999999999998D);
      pushOutOfBlocks(posX + (double)width * 0.34999999999999998D, boundingBox.minY + 0.5D, posZ - (double)width * 0.34999999999999998D);
      pushOutOfBlocks(posX + (double)width * 0.34999999999999998D, boundingBox.minY + 0.5D, posZ + (double)width * 0.34999999999999998D);
      boolean flag2 = (float)getFoodStats().getFoodLevel() > 6F;

      if (onGround && !flag1 && movementInput.moveForward >= f && !isSprinting() && flag2 && !isUsingItem() && !isPotionActive(Potion.blindness))
      {
         if (sprintToggleTimer == 0)
         {
            sprintToggleTimer = 7;
         }
         else
         {
            setSprinting(true);
            sprintToggleTimer = 0;
         }
      }

      if (isSneaking())
      {
         sprintToggleTimer = 0;
      }

      if (isSprinting() && (movementInput.moveForward < f || isCollidedHorizontally || !flag2))
      {
         setSprinting(false);
      }

      if (capabilities.allowFlying && !flag && movementInput.jump)
      {
         if (flyToggleTimer == 0)
         {
            flyToggleTimer = 7;
         }
         else
         {
            capabilities.isFlying = !capabilities.isFlying;
            func_50009_aI();
            flyToggleTimer = 0;
         }
      }

      if (capabilities.isFlying)
      {
         if (movementInput.sneak)
         {
            motionY -= 0.14999999999999999D;
         }

         if (movementInput.jump)
         {
            motionY += 0.14999999999999999D;
         }
      } else if (canRunSPC() && ph.flying && ph.flymode.equalsIgnoreCase("minecraft") && !capabilities.isFlying) { // SPC_FLYMODE_MINECRAFT
         if(movementInput.sneak)
         {
            motionY -= 0.14999999999999999D * 2;
         }
         if(movementInput.jump)
         {
            motionY += 0.14999999999999999D * 2;
         }
      }

      super.onLivingUpdate();

      if (onGround && capabilities.isFlying)
      {
         capabilities.isFlying = false;
         func_50009_aI();
      }
   }

   public void travelToTheEnd(int par1)
   {
      if (worldObj.isRemote)
      {
         return;
      }

      if (dimension == 1 && par1 == 1)
      {
         triggerAchievement(AchievementList.theEnd2);
         mc.displayGuiScreen(new GuiWinGame());
      }
      else
      {
         triggerAchievement(AchievementList.theEnd);
         mc.sndManager.playSoundFX("portal.travel", 1.0F, rand.nextFloat() * 0.4F + 0.8F);
         mc.usePortal(1);
      }
   }

   /**
    * Gets the player's field of view multiplier. (ex. when flying)
    */
   public float getFOVMultiplier()
   {
      float f = 1.0F;

      if (capabilities.isFlying)
      {
         f *= 1.1F;
      }

      f *= ((landMovementFactor * getSpeedModifier()) / speedOnGround + 1.0F) / 2.0F;

      if (isUsingItem() && getItemInUse().itemID == Item.bow.shiftedIndex)
      {
         int i = getItemInUseDuration();
         float f1 = (float)i / 20F;

         if (f1 > 1.0F)
         {
            f1 = 1.0F;
         }
         else
         {
            f1 *= f1;
         }

         f *= 1.0F - f1 * 0.15F;
      }

      return f;
   }

   /**
    * (abstract) Protected helper method to write subclass entity data to NBT.
    */
   public void writeEntityToNBT(NBTTagCompound par1NBTTagCompound)
   {
      super.writeEntityToNBT(par1NBTTagCompound);
      par1NBTTagCompound.setInteger("Score", score);
      if (canRunSPC()) {
         ph.writeWaypointsToNBT(((SaveHandler) this.mc.theWorld.saveHandler).getSaveDirectory());
      }
   }

   /**
    * (abstract) Protected helper method to read subclass entity data from NBT.
    */
   public void readEntityFromNBT(NBTTagCompound par1NBTTagCompound)
   {
      super.readEntityFromNBT(par1NBTTagCompound);
      score = par1NBTTagCompound.getInteger("Score");
      if (canRunSPC()) {
         ph.readWaypointsFromNBT(((SaveHandler) this.mc.theWorld.saveHandler).getSaveDirectory());
      }
   }

   /**
    * sets current screen to null (used on escape buttons of GUIs)
    */
   public void closeScreen()
   {
      super.closeScreen();
      mc.displayGuiScreen(null);
   }

   /**
    * Displays the GUI for editing a sign. Args: tileEntitySign
    */
   public void displayGUIEditSign(TileEntitySign par1TileEntitySign)
   {
      mc.displayGuiScreen(new GuiEditSign(par1TileEntitySign));
   }

   /**
    * Displays the GUI for interacting with a chest inventory. Args: chestInventory
    */
   public void displayGUIChest(IInventory par1IInventory)
   {
      mc.displayGuiScreen(new GuiChest(inventory, par1IInventory));
   }

   /**
    * Displays the crafting GUI for a workbench.
    */
   public void displayWorkbenchGUI(int par1, int par2, int par3)
   {
      mc.displayGuiScreen(new GuiCrafting(inventory, worldObj, par1, par2, par3));
   }

   public void displayGUIEnchantment(int par1, int par2, int par3)
   {
      mc.displayGuiScreen(new GuiEnchantment(inventory, worldObj, par1, par2, par3));
   }

   /**
    * Displays the furnace GUI for the passed in furnace entity. Args: tileEntityFurnace
    */
   public void displayGUIFurnace(TileEntityFurnace par1TileEntityFurnace)
   {
      mc.displayGuiScreen(new GuiFurnace(inventory, par1TileEntityFurnace));
   }

   /**
    * Displays the GUI for interacting with a brewing stand.
    */
   public void displayGUIBrewingStand(TileEntityBrewingStand par1TileEntityBrewingStand)
   {
      mc.displayGuiScreen(new GuiBrewingStand(inventory, par1TileEntityBrewingStand));
   }

   /**
    * Displays the dipsenser GUI for the passed in dispenser entity. Args: TileEntityDispenser
    */
   public void displayGUIDispenser(TileEntityDispenser par1TileEntityDispenser)
   {
      mc.displayGuiScreen(new GuiDispenser(inventory, par1TileEntityDispenser));
   }

   /**
    * is called when the player performs a critical hit on the Entity. Args: entity that was hit critically
    */
   public void onCriticalHit(Entity par1Entity)
   {
      mc.effectRenderer.addEffect(new EntityCrit2FX(mc.theWorld, par1Entity));
   }

   public void onEnchantmentCritical(Entity par1Entity)
   {
      EntityCrit2FX entitycrit2fx = new EntityCrit2FX(mc.theWorld, par1Entity, "magicCrit");
      mc.effectRenderer.addEffect(entitycrit2fx);
   }

   /**
    * Called whenever an item is picked up from walking over it. Args: pickedUpEntity, stackSize
    */
   public void onItemPickup(Entity par1Entity, int par2)
   {
      mc.effectRenderer.addEffect(new EntityPickupFX(mc.theWorld, par1Entity, this, -0.5F));
   }

   /**
    * Sends a chat message from the player. Args: chatMessage
    */
   public void sendChatMessage(String s)
   {
      if (mc.ingameGUI.getSentMessageList().size() == 0 || !((String)mc.ingameGUI.getSentMessageList().get(mc.ingameGUI.getSentMessageList().size() - 1)).equals(s)) {
         mc.ingameGUI.getSentMessageList().add(s);
      }
      if (canRunSPC()) {
         ph.processCommand(s);
      }
   }

   /**
    * Returns if this entity is sneaking.
    */
   public boolean isSneaking()
   {
      return movementInput.sneak && !sleeping;
   }

   /**
    * Updates health locally.
    */
   public void setHealth(int par1)
   {
      int i = getHealth() - par1;

      if (i <= 0)
      {
         setEntityHealth(par1);

         if (i < 0)
         {
            heartsLife = heartsHalvesLife / 2;
         }
      }
      else
      {
         naturalArmorRating = i;
         setEntityHealth(getHealth());
         heartsLife = heartsHalvesLife;
         damageEntity(DamageSource.generic, i);
         hurtTime = maxHurtTime = 10;
      }
   }

   public void respawnPlayer()
   {
      mc.respawn(false, 0, false);
   }

   public void func_6420_o()
   {
   }

   /**
    * Add a chat message to the player
    */
   public void addChatMessage(String par1Str)
   {
      mc.ingameGUI.addChatMessageTranslate(par1Str);
   }

   /**
    * Adds a value to a statistic field.
    */
   public void addStat(StatBase par1StatBase, int par2)
   {
      if (par1StatBase == null)
      {
         return;
      }

      if (par1StatBase.isAchievement())
      {
         Achievement achievement = (Achievement)par1StatBase;

         if (achievement.parentAchievement == null || mc.statFileWriter.hasAchievementUnlocked(achievement.parentAchievement))
         {
            if (!mc.statFileWriter.hasAchievementUnlocked(achievement))
            {
               mc.guiAchievement.queueTakenAchievement(achievement);
            }

            mc.statFileWriter.readStat(par1StatBase, par2);
         }
      }
      else
      {
         mc.statFileWriter.readStat(par1StatBase, par2);
      }
   }

   private boolean isBlockTranslucent(int par1, int par2, int par3)
   {
      return worldObj.isBlockNormalCube(par1, par2, par3);
   }

   /**
    * Adds velocity to push the entity out of blocks at the specified x, y, z position Args: x, y, z
    */
   protected boolean pushOutOfBlocks(double par1, double par3, double par5)
   {
      int i = MathHelper.floor_double(par1);
      int j = MathHelper.floor_double(par3);
      int k = MathHelper.floor_double(par5);
      double d = par1 - (double)i;
      double d1 = par5 - (double)k;

      if (isBlockTranslucent(i, j, k) || isBlockTranslucent(i, j + 1, k))
      {
         boolean flag = !isBlockTranslucent(i - 1, j, k) && !isBlockTranslucent(i - 1, j + 1, k);
         boolean flag1 = !isBlockTranslucent(i + 1, j, k) && !isBlockTranslucent(i + 1, j + 1, k);
         boolean flag2 = !isBlockTranslucent(i, j, k - 1) && !isBlockTranslucent(i, j + 1, k - 1);
         boolean flag3 = !isBlockTranslucent(i, j, k + 1) && !isBlockTranslucent(i, j + 1, k + 1);
         byte byte0 = -1;
         double d2 = 9999D;

         if (flag && d < d2)
         {
            d2 = d;
            byte0 = 0;
         }

         if (flag1 && 1.0D - d < d2)
         {
            d2 = 1.0D - d;
            byte0 = 1;
         }

         if (flag2 && d1 < d2)
         {
            d2 = d1;
            byte0 = 4;
         }

         if (flag3 && 1.0D - d1 < d2)
         {
            double d3 = 1.0D - d1;
            byte0 = 5;
         }

         float f = 0.1F;

         if (byte0 == 0)
         {
            motionX = -f;
         }

         if (byte0 == 1)
         {
            motionX = f;
         }

         if (byte0 == 4)
         {
            motionZ = -f;
         }

         if (byte0 == 5)
         {
            motionZ = f;
         }
      }

      return false;
   }

   /**
    * Set sprinting switch for Entity.
    */
   public void setSprinting(boolean par1)
   {
      super.setSprinting(par1);
      sprintingTicksLeft = par1 ? 600 : 0;
   }

   /**
    * Sets the current XP, total XP, and level number.
    */
   public void setXPStats(float par1, int par2, int par3)
   {
      experience = par1;
      experienceTotal = par2;
      experienceLevel = par3;
   }
   
   @Override
   public boolean isEntityInsideOpaqueBlock() {
      if (canRunSPC() && ph.noClip) {
         return false;
      }
      return super.isEntityInsideOpaqueBlock();
   }   

   @Override
   protected String getHurtSound() {
      if (multiplayer || (canRunSPC() && ph.damage)) {
         return super.getHurtSound();
      } else {
         return "";
      }
   }

   @Override
   public float getCurrentPlayerStrVsBlock(Block block) {
      if (canRunSPC() && ph.instant) {
         return Float.MAX_VALUE;
      }
      return super.getCurrentPlayerStrVsBlock(block);
   }

   @Override
   public boolean canHarvestBlock(Block block) {
      if (canRunSPC() && ph.instant) {
         return true;
      }
      return super.canHarvestBlock(block);
   }

   @Override
   protected void fall(float f) {
      if (canRunSPC() && !ph.falldamage) {
         return;
      }
      super.fall(f);
   }
   
   @Override
   public void addExhaustion(float f) {
      if (canRunSPC() && (ph.flying || ph.disableHunger)) {
         return;
      }
      super.addExhaustion(f);
   }

   @Override
   protected void jump() {
      if (canRunSPC() && ph.gravity > 1.0D) {
         this.motionY = (0.4199999868869782D * ph.gravity);
         return;
      }
      super.jump();
   }

   @Override
   public void moveFlying(float f, float f1, float f2) {
      if (!canRunSPC() || ph.speed <= 1.0F) {
         super.moveFlying(f, f1, f2);
         return;
      }
      float f3 = MathHelper.sqrt_float(f * f + f1 * f1);
      if (f3 < 0.01F) {
         return;
      }
      if (f3 < 1.0F) {
         f3 = 1.0F;
      }
      f3 = f2 / f3;
      f *= f3;
      f1 *= f3;
      float f4 = MathHelper.sin(this.rotationYaw * 3.141593F / 180.0F);
      float f5 = MathHelper.cos(this.rotationYaw * 3.141593F / 180.0F);
      double speed = ((canRunSPC()) ? ph.speed : 1);
      this.motionX += (f * f5 - f1 * f4) * speed;
      this.motionZ += (f1 * f5 + f * f4) * speed;
   }

   @Override
   public void onUpdate() {
      if (canRunSPC()) {
         ph.beforeUpdate();
         super.onUpdate();
         ph.afterUpdate();
      } else {
         super.onUpdate();
      }
   }

   @Override
   protected void damageEntity(DamageSource d, int i) {
      if (canRunSPC() && !ph.damage) {
         return;
      }
      super.damageEntity(d,i);
   }

   @Override
   public void setDead() {
      if (canRunSPC()) {
         ph.setCurrentPosition();
      }
      super.setDead();
   }

   @Override
   public double getDistanceSqToEntity(Entity entity) {
      if (canRunSPC() && (!ph.mobdamage || ph.mobsfrozen)) {
         return Double.MAX_VALUE;
      }
      return super.getDistanceSqToEntity(entity);
   }

   @Override
   public void onDeath(DamageSource entity) {
      if (canRunSPC() && ph.keepitems && PlayerHelper.INV_BEFORE_DEATH != null) {

         for (int j = 0; j < inventory.armorInventory.length; j++) {
            PlayerHelper.INV_BEFORE_DEATH.armorInventory[j] = inventory.armorItemInSlot(j);
         }
         for (int j = 0; j < inventory.mainInventory.length; j++) {
            PlayerHelper.INV_BEFORE_DEATH.mainInventory[j] = inventory.mainInventory[j];
         }
         ph.destroyInventory();
      }
      super.onDeath(entity);
   }

   @Override
   public void attackTargetEntityWithCurrentItem(Entity entity) {
      if (canRunSPC() && ph.instantkill) {
         entity.attackEntityFrom(DamageSource.causePlayerDamage(this), Integer.MAX_VALUE);
         entity.kill();
         return;
      } else if (canRunSPC() && ph.criticalHit) {
         double my = motionY;
         boolean og = onGround;
         boolean iw = inWater;
         float fd = fallDistance;
         super.motionY = -0.1D;
         super.inWater = false;
         super.onGround = false;
         super.fallDistance = 0.1F;
         super.attackTargetEntityWithCurrentItem(entity);
         motionY = my;
         onGround = og;
         inWater = iw;
         fallDistance = fd;
         return;
      }
      super.attackTargetEntityWithCurrentItem(entity);
   }

   @Override
   public boolean handleWaterMovement() {
      if (canRunSPC() && !ph.watermovement) {
         return false;
      }
      return super.handleWaterMovement();
   }

   @Override
   public boolean handleLavaMovement() {
      if (canRunSPC() && !ph.watermovement) {
         return false;
      }
      return super.handleLavaMovement();
   }

   @Override
   public EntityItem dropPlayerItemWithRandomChoice(ItemStack itemstack, boolean flag) {
      if (canRunSPC()) {
         ph.givePlayerItemNaturally(itemstack);
         return null;
      }
      return super.dropPlayerItemWithRandomChoice(itemstack,flag);
   }

   @Override
   public MovingObjectPosition rayTrace(double d, float f) {
      if (canRunSPC() && d == this.mc.playerController.getBlockReachDistance()) {
         d = ph.reachdistance;
      }
      return super.rayTrace(d, f);
   }
   
   @Override
   public boolean isOnLadder() {
      if (canRunSPC() && ph.ladderMode && isCollidedHorizontally) {
         return true;
      }
      return super.isOnLadder();
   }

   /*
    * showErrorFrame - shows a Swing JFrame containing troubleshooting information if SPC was installed incorrectly.
    */
   public void showErrorFrame() {
      JFrame frame = new JFrame();
      JTextPane textarea = new JTextPane();

      frame.setBackground(Color.lightGray);
      textarea.setContentType("text/html");

      String text = "<html><p>";
      text = text.concat("Single Player Commands v" + SPCVERSION.getVersion() + " for Minecraft version " + MCVERSION + "<br />");
      text = text.concat("Running Minecraft version " + curmcversion + "<br />");
      text = text.concat("You are missing the following class files necessary for <br />" + "Single Player Commands to operate:<br /><br /><ul>");
      for (String missing : missingRequiredClasses) {
         text = text.concat("<li>" + missing + "</li>");
      }
      text = text.concat("</ul><br />");

      text = text.concat("Make sure that all of the class files listed above are in minecraft.jar.<br />");
      text = text.concat("If they are not, copy them from your SPC download folder into minecraft.jar<br />" + "and try running Minecraft again.<br />");
      text = text.concat("If errors persist, copy and paste this error log to <a href=\"http://bit.ly/spcmod\">http://bit.ly/spcmod</a> for help.");
      text = text.concat("</p></html>");

      textarea.setText(text);

      frame.setLayout(new BorderLayout());
      frame.add(textarea, BorderLayout.CENTER);
      frame.pack();
      frame.setVisible(true);
      addChatMessage("\2474" + "SPC Error: Not installed properly.");
      addChatMessage("\2474" + "Check dialog window for more information.");
      MESSAGESHOWN = new Object();
   }

   /*
    * initPlayerHelper - initializes the PlayerHelper variable. Only called if all necessary SPC files exist.
    */
   public void initPlayerHelper(Session session) {
      ph = new PlayerHelper(this.mc, this);
      ph.readWaypointsFromNBT(ph.getWorldDir());
      multiplayer = mc.isMultiplayerWorld();

      if (STARTUP == null && !multiplayer) {
         ph.sendMessage("\2478Single Player Commands (" + SPCVERSION.getVersion() + ") - http://bit.ly/spcmod");
         Calendar cal = Calendar.getInstance();
         if (cal.get(Calendar.DAY_OF_MONTH) == 25 && cal.get(Calendar.MONTH) == 11) {
            String name = username == null || username.equalsIgnoreCase("") ? "" : "Dear " + username + ", ";
            ph.sendMessage("\2474" + name + "Merry Christmas! From simo_415");
         } else if (cal.get(Calendar.DAY_OF_MONTH) == 6 && cal.get(Calendar.MONTH) == 11) {
            ph.sendMessage("\2475Happy birthday Single Player Commands. Now a year older!");
         }
         STARTUP = new Object();
      }
      if (session != null && session.username != null && session.username.length() > 0) {
         ph.sessionusername = session.username;
      }
   }

   /*
    * checkClasses - checks if all the required SPC classes exist. If they do, returns true. Otherwise, returns false.
    */
   public boolean checkClasses() {
      missingRequiredClasses = new Vector<String>();
      missingOptionalClasses = new Vector<String>();
      phexists = true;
      curmcversion = Display.getTitle().split(" ")[Display.getTitle().split(" ").length - 1];
      /*
       * Pointless bit of code which trunks insists on leaving in
       */
      if (!curmcversion.equalsIgnoreCase(MCVERSION)) {
         addChatMessage("\2474" + "Single Player Commands v" + SPCVERSION.getVersion() + " is not compatible with Minecraft v" + curmcversion);
         addChatMessage("\2474" + "Visit http://bit.ly/spcmod to download the correct version.");
         System.err.println("Single Player Commands v" + SPCVERSION.getVersion() + " is not compatible with Minecraft v" + curmcversion);
         System.err.println("Visit http://bit.ly/spcmod to download the correct version.");
      }
      Package p = EntityPlayerSP.class.getPackage();
      String prefix = p == null ? "" : p.getName() + ".";
      String requiredClasses[] = new String[] { "PlayerHelper", "Settings", "SPCPlugin", "SPCPluginManager", "SPCCommand" };
      String optionalClasses[] = new String[] { "spc_WorldEdit", "SPCLocalConfiguration", "SPCLocalPlayer", "SPCLocalWorld", "SPCServerInterface", "WorldEditPlugin" };

      for (String classname : requiredClasses) {
         try {
            Class.forName(prefix + classname);
         } catch (Throwable e) {
            missingRequiredClasses.add(classname);
         }
      }
      for (String classname : optionalClasses) {
         try {
            Class.forName(prefix + classname);
         } catch (Throwable e) {
            missingOptionalClasses.add(classname);
         }
      }

      if (missingRequiredClasses.size() != 0) {
         addChatMessage("\2474" + "You are missing these class files: ");
         String list = "";
         for (String missing : missingRequiredClasses) {
            list += missing + ", ";
         }
         addChatMessage("\2474" + list);
         addChatMessage("\2474" + "Please try reinstalling.");
         phexists = false;
      }
      return phexists;
   }

   /**
    * Checks if SPC is allowed to run or not.
    * @return true if SPC is allowed to run
    */
   public boolean canRunSPC() {
      return phexists && !multiplayer;
   }
}
