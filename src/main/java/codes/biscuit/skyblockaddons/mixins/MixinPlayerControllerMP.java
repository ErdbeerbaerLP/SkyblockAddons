package codes.biscuit.skyblockaddons.mixins;

import codes.biscuit.skyblockaddons.SkyblockAddons;
import codes.biscuit.skyblockaddons.utils.Blacklist;
import codes.biscuit.skyblockaddons.utils.EnumUtils;
import codes.biscuit.skyblockaddons.utils.Feature;
import codes.biscuit.skyblockaddons.utils.Message;
import net.minecraft.block.Block;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityOtherPlayerMP;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.multiplayer.PlayerControllerMP;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumActionResult;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

@Mixin(PlayerControllerMP.class)
public class MixinPlayerControllerMP {

    private long lastStemMessage = -1;
    private long lastProfileMessage = -1;

    /**
     * Cancels stem breaks if holding an item, to avoid accidental breaking.
     */
    @Inject(method = "clickBlock", at = @At(value = "HEAD"), locals = LocalCapture.CAPTURE_FAILSOFT, cancellable = true)
    private void onPlayerDamageBlock(BlockPos loc, EnumFacing face, CallbackInfoReturnable<Boolean> cir) {
        SkyblockAddons main = SkyblockAddons.getInstance();
        Minecraft mc = Minecraft.getMinecraft();
        EntityPlayerSP p = mc.player;
        ItemStack heldItem = p.getHeldItem(EnumHand.MAIN_HAND);
        Block block = mc.world.getBlockState(loc).getBlock();
        if (heldItem != null) {
            if (main.getConfigValues().isEnabled(Feature.AVOID_BREAKING_STEMS) && (block.equals(Blocks.MELON_STEM) || block.equals(Blocks.PUMPKIN_STEM))) {
                if (System.currentTimeMillis()- lastStemMessage > 20000) {
                    lastStemMessage = System.currentTimeMillis();
                    main.getUtils().sendMessage(TextFormatting.RED + Message.MESSAGE_CANCELLED_STEM_BREAK.getMessage());
                }
                cir.setReturnValue(false);
                return;
            }
        }
        if (main.getConfigValues().isEnabled(Feature.AVOID_BREAKING_BOTTOM_SUGAR_CANE) && main.getUtils().getLocation() == EnumUtils.Location.ISLAND
                && (block.equals(Blocks.REEDS) && mc.world.getBlockState(loc.down()).getBlock() != Blocks.REEDS)) {
            if (heldItem == null || heldItem.getItem().equals(Items.REEDS) || heldItem.getItem().equals(Items.DIAMOND_HOE)
                    || heldItem.getItem().equals(Items.IRON_HOE) || heldItem.getItem().equals(Items.GOLDEN_HOE) || heldItem.getItem().equals(Items.STONE_HOE)
                    || heldItem.getItem().equals(Items.WOODEN_HOE) || heldItem.getItem().equals(Items.WOODEN_HOE)) {
                if (System.currentTimeMillis() - lastStemMessage > 20000) {
                    lastStemMessage = System.currentTimeMillis();
                    main.getUtils().sendMessage(TextFormatting.RED + Message.MESSAGE_CANCELLED_CANE_BREAK.getMessage());
                }
                cir.setReturnValue(false);
            }
        }
    }

    /**
     * This blocks interaction with Ember Rods on your island, to avoid blowing up chests.
     */
    @Inject(method = "processRightClick", at = @At(value = "HEAD"), cancellable = true)
    public void processRightClick(EntityPlayer player, World worldIn, EnumHand hand, CallbackInfoReturnable<EnumActionResult> cir) {
        Minecraft mc = Minecraft.getMinecraft();
        ItemStack heldItem = player.getHeldItemMainhand();

        if (player.equals(mc.player) && !ItemStack.EMPTY.equals(heldItem)) {
            for (Blacklist.BlacklistedItem blacklistedItem : Blacklist.DO_NOT_RIGHT_CLICK) {
                if (blacklistedItem.isEnabled()) {
                    if (!blacklistedItem.isOnlyEnchanted() || heldItem.isItemEnchanted()) {
                        if (!blacklistedItem.isOnlyOnIsland() || SkyblockAddons.getInstance().getUtils().getLocation() == EnumUtils.Location.ISLAND) {
                            if (blacklistedItem.getItem().equals(heldItem.getItem())) {
                                if (!blacklistedItem.canCtrlKeyBypass() || !GuiScreen.isCtrlKeyDown()) {
                                    cir.setReturnValue(EnumActionResult.FAIL);
                                    break;
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    @Inject(method = "interactWithEntity(Lnet/minecraft/entity/player/EntityPlayer;Lnet/minecraft/entity/Entity;Lnet/minecraft/util/EnumHand;)Lnet/minecraft/util/EnumActionResult;", at = @At(value = "HEAD"), locals = LocalCapture.CAPTURE_FAILSOFT, cancellable = true)
    private void onPlayerRightClickEntity(EntityPlayer player, Entity entityIn, EnumHand hand, CallbackInfoReturnable<Boolean> cir) {
        SkyblockAddons main = SkyblockAddons.getInstance();
        if (main.getConfigValues().isEnabled(Feature.DONT_OPEN_PROFILES_WITH_BOW)) {
            if (entityIn instanceof EntityOtherPlayerMP) {
                ItemStack item = player.inventory.getCurrentItem();
                if (item != null && item.getItem() != null && item.getItem().equals(Items.BOW)) {
                    if (System.currentTimeMillis()- lastProfileMessage > 20000) {
                        lastProfileMessage = System.currentTimeMillis();
                        main.getUtils().sendMessage(TextFormatting.RED + Message.MESSAGE_STOPPED_OPENING_PROFILE.getMessage());
                    }
                    cir.setReturnValue(true);
                }
            }
        }
    }
}
