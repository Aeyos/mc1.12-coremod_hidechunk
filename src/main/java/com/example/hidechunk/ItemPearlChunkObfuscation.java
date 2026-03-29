package com.example.hidechunk;

import com.example.hidechunk.client.HideChunkClientRefresh;
import com.example.hidechunk.core.HideChunkState;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ActionResult;
import net.minecraft.util.EnumActionResult;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.world.World;
import net.minecraft.world.chunk.Chunk;

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
            int dimension = world.provider.getDimension();
            ChunkPos cpos = new ChunkPos(pos);
            Chunk chunk = world.getChunk(cpos.x, cpos.z);
            int cx = chunk.x;
            int cz = chunk.z;

            if (HideChunkState.isHidden(dimension, cx, cz)) {
                HideChunkState.showChunk(dimension, cx, cz);
                player.sendStatusMessage(new TextComponentString("Chunk visible: " + cx + ", " + cz), true);
                HideChunkClientRefresh.scheduleColumnMeshRebuild(world, cx, cz);
            } else {
                HideChunkState.hideChunk(dimension, cx, cz);
                player.sendStatusMessage(new TextComponentString("Chunk hidden: " + cx + ", " + cz), true);
                HideChunkClientRefresh.scheduleColumnMeshRebuild(world, cx, cz);
            }
        }
        return new ActionResult<>(EnumActionResult.SUCCESS, stack);
    }
}
