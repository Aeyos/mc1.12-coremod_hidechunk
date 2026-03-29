package com.example.hidechunk;

import com.example.hidechunk.core.HideChunkState;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ActionResult;
import net.minecraft.util.EnumActionResult;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.world.World;

public class ItemPearlChunkObfuscation extends Item {

    public ItemPearlChunkObfuscation() {
        setMaxStackSize(1);
        setCreativeTab(CreativeTabs.MISC);
    }

    @Override
    public ActionResult<ItemStack> onItemRightClick(World world, EntityPlayer player, EnumHand hand) {
        ItemStack stack = player.getHeldItem(hand);
        if (world.isRemote) {
            BlockPos pos = player.getPosition();
            int cx = pos.getX() >> 4;
            int cz = pos.getZ() >> 4;

            int minX = cx << 4;
            int minZ = cz << 4;
            int maxX = minX + 15;
            int maxZ = minZ + 15;

            if (HideChunkState.isHidden(cx, cz)) {
                HideChunkState.showChunk(cx, cz);
                player.sendStatusMessage(new TextComponentString("Chunk visible: " + cx + ", " + cz), true);
            } else {
                HideChunkState.hideChunk(cx, cz);
                player.sendStatusMessage(new TextComponentString("Chunk hidden: " + cx + ", " + cz), true);
            }
            // Force chunk section recompile so shouldSkipRebuild runs (skipping alone does not drop already-built meshes).
            world.markBlockRangeForRenderUpdate(minX, 0, minZ, maxX, 255, maxZ);
        }
        return new ActionResult<>(EnumActionResult.SUCCESS, stack);
    }
}
