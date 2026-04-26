package com.example.hidechunk.assembler;

import com.example.hidechunk.HideChunkMod;
import net.minecraft.block.BlockContainer;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.MapColor;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumBlockRenderType;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import javax.annotation.Nullable;

public class BlockUltimateAssembler extends BlockContainer {

    public BlockUltimateAssembler() {
        super(Material.IRON, MapColor.GRAY);
        setTranslationKey(HideChunkMod.MODID + ".ultimate_assembler");
        setHardness(3.5F);
        setResistance(10F);
        setSoundType(SoundType.METAL);
        setCreativeTab(CreativeTabs.REDSTONE);
    }

    @Nullable
    @Override
    public TileEntity createNewTileEntity(World worldIn, int meta) {
        return new TileUltimateAssembler();
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
                    HideChunkMod.GUI_ULTIMATE_ASSEMBLER,
                    world,
                    pos.getX(),
                    pos.getY(),
                    pos.getZ());
        }
        return true;
    }

    @Override
    public EnumBlockRenderType getRenderType(IBlockState state) {
        return EnumBlockRenderType.MODEL;
    }

    @Override
    public boolean hasTileEntity(IBlockState state) {
        return true;
    }
}
