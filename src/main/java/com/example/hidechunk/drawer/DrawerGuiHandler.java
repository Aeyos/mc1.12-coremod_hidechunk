package com.example.hidechunk.drawer;

import com.example.hidechunk.HideChunkMod;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.fml.common.network.IGuiHandler;

import javax.annotation.Nullable;

public class DrawerGuiHandler implements IGuiHandler {

    @Nullable
    @Override
    public Object getServerGuiElement(int id, EntityPlayer player, World world, int x, int y, int z) {
        if (id != HideChunkMod.GUI_DRAWER_CAPACITOR) {
            return null;
        }
        TileDrawerCapacitor te = getTile(world, x, y, z);
        if (te == null) {
            return null;
        }
        return new ContainerDrawerCapacitor(player.inventory, te);
    }

    @Nullable
    @Override
    public Object getClientGuiElement(int id, EntityPlayer player, World world, int x, int y, int z) {
        if (id != HideChunkMod.GUI_DRAWER_CAPACITOR) {
            return null;
        }
        TileDrawerCapacitor te = getTile(world, x, y, z);
        if (te == null) {
            return null;
        }
        return new com.example.hidechunk.drawer.client.GuiDrawerCapacitor(player.inventory, te);
    }

    @Nullable
    private static TileDrawerCapacitor getTile(World world, int x, int y, int z) {
        net.minecraft.tileentity.TileEntity te = world.getTileEntity(new BlockPos(x, y, z));
        if (te instanceof TileDrawerCapacitor) {
            return (TileDrawerCapacitor) te;
        }
        return null;
    }
}
