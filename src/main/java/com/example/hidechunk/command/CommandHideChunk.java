package com.example.hidechunk.command;

import com.example.hidechunk.HideChunkMod;
import com.google.common.collect.Lists;
import net.minecraft.command.CommandBase;
import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.command.WrongUsageException;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemStack;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.world.WorldServer;
import net.minecraft.world.storage.loot.LootContext;
import net.minecraft.world.storage.loot.LootTable;
import net.minecraftforge.fml.common.Loader;

import java.util.List;
import java.util.Random;

/**
 * Root command {@code /hidechunk ...}. Subcommand {@code lootcaps} rolls EnderIO-style loot capacitors.
 */
public class CommandHideChunk extends CommandBase {

    static final ResourceLocation LOOTCAPS_TABLE =
            new ResourceLocation(HideChunkMod.MODID, "lootcaps_roll");

    /** Matches EnderIO chest pools: special capacitor meta with {@code enderio:set_capacitor}. */
    private static final int MAX_LOOTCAPS_PER_RUN = 256;

    @Override
    public String getName() {
        return "hidechunk";
    }

    @Override
    public String getUsage(ICommandSender sender) {
        return "commands.hidechunk.usage";
    }

    @Override
    public int getRequiredPermissionLevel() {
        return 2;
    }

    @Override
    public void execute(MinecraftServer server, ICommandSender sender, String[] args) throws CommandException {
        if (args.length < 1) {
            throw new WrongUsageException("commands.hidechunk.usage", new Object[0]);
        }
        String sub = args[0].toLowerCase();
        if ("lootcaps".equals(sub)) {
            executeLootCaps(server, sender, args);
            return;
        }
        throw new WrongUsageException("commands.hidechunk.usage", new Object[0]);
    }

    private void executeLootCaps(MinecraftServer server, ICommandSender sender, String[] args)
            throws CommandException {
        if (!Loader.isModLoaded("enderio")) {
            throw new CommandException("commands.hidechunk.lootcaps.need_enderio", new Object[0]);
        }
        if (!(sender instanceof EntityPlayerMP)) {
            throw new CommandException("commands.hidechunk.lootcaps.player_only", new Object[0]);
        }
        if (args.length < 2) {
            throw new WrongUsageException("commands.hidechunk.lootcaps.usage", new Object[0]);
        }
        int qty = parseInt(args[1], 1, MAX_LOOTCAPS_PER_RUN);
        EntityPlayerMP player = (EntityPlayerMP) sender;
        WorldServer world = player.getServerWorld();
        LootTable table = world.getLootTableManager().getLootTableFromLocation(LOOTCAPS_TABLE);
        if (table == LootTable.EMPTY_LOOT_TABLE) {
            throw new CommandException("commands.hidechunk.lootcaps.missing_table", new Object[0]);
        }
        Random rand = world.rand;
        int stacksSpawned = 0;
        int stacksDropped = 0;
        for (int i = 0; i < qty; i++) {
            LootContext.Builder ctx = new LootContext.Builder(world);
            ctx.withLuck(player.getLuck());
            List<ItemStack> rolled = table.generateLootForPools(rand, ctx.build());
            for (ItemStack stack : rolled) {
                if (stack.isEmpty()) {
                    continue;
                }
                stacksSpawned++;
                if (!player.inventory.addItemStackToInventory(stack)) {
                    player.dropItem(stack, false);
                    stacksDropped++;
                }
            }
        }
        sender.sendMessage(
                new TextComponentTranslation(
                        "commands.hidechunk.lootcaps.success",
                        stacksSpawned,
                        qty,
                        stacksDropped));
    }

    @Override
    public List<String> getTabCompletions(
            MinecraftServer server,
            ICommandSender sender,
            String[] args,
            net.minecraft.util.math.BlockPos targetPos) {
        if (args.length == 1) {
            return getListOfStringsMatchingLastWord(args, "lootcaps");
        }
        return Lists.newArrayList();
    }
}
