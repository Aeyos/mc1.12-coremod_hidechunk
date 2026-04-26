package com.example.hidechunk.assembler;

import com.example.hidechunk.HideChunkMod;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.fml.common.network.IGuiHandler;

import javax.annotation.Nullable;

public class AssemblerGuiHandler implements IGuiHandler {

    @Nullable
    @Override
    public Object getServerGuiElement(int id, EntityPlayer player, World world, int x, int y, int z) {
        if (id != HideChunkMod.GUI_ULTIMATE_ASSEMBLER) {
            return null;
        }
        TileUltimateAssembler te = getTile(world, x, y, z);
        if (te == null) {
            return null;
        }
        return new ContainerUltimateAssembler(player.inventory, te);
    }

    @Nullable
    @Override
    public Object getClientGuiElement(int id, EntityPlayer player, World world, int x, int y, int z) {
        if (id != HideChunkMod.GUI_ULTIMATE_ASSEMBLER) {
            return null;
        }
        TileUltimateAssembler te = getTile(world, x, y, z);
        if (te == null) {
            return null;
        }
        return new com.example.hidechunk.assembler.client.GuiUltimateAssembler(player.inventory, te);
    }

    @Nullable
    private static TileUltimateAssembler getTile(World world, int x, int y, int z) {
        TileEntity te = world.getTileEntity(new BlockPos(x, y, z));
        if (te instanceof TileUltimateAssembler) {
            return (TileUltimateAssembler) te;
        }
        return null;
    }
}
