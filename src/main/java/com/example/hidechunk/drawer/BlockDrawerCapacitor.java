package com.example.hidechunk.drawer;

import com.example.hidechunk.HideChunkMod;
import net.minecraft.block.BlockContainer;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.MapColor;
import net.minecraft.block.material.Material;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumBlockRenderType;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;

import javax.annotation.Nullable;

public class BlockDrawerCapacitor extends BlockContainer {

    public BlockDrawerCapacitor() {
        super(Material.IRON, MapColor.GRAY);
        setTranslationKey(HideChunkMod.MODID + ".drawer_capacitor_wonders");
        setHardness(3.5F);
        setResistance(10F);
        setSoundType(SoundType.METAL);
        setCreativeTab(CreativeTabs.REDSTONE);
    }

    @Nullable
    @Override
    public TileEntity createNewTileEntity(World worldIn, int meta) {
        return new TileDrawerCapacitor();
    }

    @Override
    public boolean onBlockActivated(
            World world,
            BlockPos pos,
            IBlockState state,
            EntityPlayer player,
            EnumHand hand,
            EnumFacing facing,
            float hitX,
            float hitY,
            float hitZ) {
        if (!world.isRemote) {
            player.openGui(
                    HideChunkMod.INSTANCE,
                    HideChunkMod.GUI_DRAWER_CAPACITOR,
                    world,
                    pos.getX(),
                    pos.getY(),
                    pos.getZ());
        }
        return true;
    }

    @Override
    public void breakBlock(World world, BlockPos pos, IBlockState state) {
        TileEntity te = world.getTileEntity(pos);
        if (te instanceof TileDrawerCapacitor && !world.isRemote) {
            TileDrawerCapacitor drawer = (TileDrawerCapacitor) te;
            drawer.dropAllStoredItems();
            drawer.dropAutomationSlot();
        }
        super.breakBlock(world, pos, state);
    }

    @Override
    public EnumBlockRenderType getRenderType(IBlockState state) {
        return EnumBlockRenderType.MODEL;
    }

    @Override
    public boolean hasTileEntity(IBlockState state) {
        return true;
    }

    @Override
    public boolean canConnectRedstone(
            IBlockState state,
            IBlockAccess world,
            BlockPos pos,
            @Nullable EnumFacing side) {
        return true;
    }
}
