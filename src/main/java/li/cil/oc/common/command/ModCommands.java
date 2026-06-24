package li.cil.oc.common.command;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import li.cil.oc.api.API;
import li.cil.oc.common.DebugCardWhitelist;
import li.cil.oc.common.ModSettings;
import li.cil.oc.common.NanomachinesRegistry;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

import java.io.IOException;
import java.util.List;
import java.util.Set;

public final class ModCommands {
    private static final SimpleCommandExceptionType WHITELIST_DISABLED =
        new SimpleCommandExceptionType(Component.literal("§cDebug card whitelisting is not enabled."));
    private static final SimpleCommandExceptionType NANOMACHINES_UNAVAILABLE =
        new SimpleCommandExceptionType(Component.literal("§cNanomachines are not available."));

    private ModCommands() {
    }

    public static void register(final RegisterCommandsEvent event) {
        event.getDispatcher().register(debugWhitelistCommand());
        event.getDispatcher().register(debugNanomachinesCommand("oc_debugNanomachines"));
        event.getDispatcher().register(debugNanomachinesCommand("oc_dn"));
        event.getDispatcher().register(logNanomachinesCommand("oc_nanomachines"));
        event.getDispatcher().register(logNanomachinesCommand("oc_nm"));
    }

    private static LiteralArgumentBuilder<CommandSourceStack> debugWhitelistCommand() {
        return Commands.literal("oc_debugWhitelist")
            .then(Commands.literal("revoke")
                .executes(context -> revoke(context.getSource(), context.getSource().getTextName()))
                .then(Commands.argument("player", StringArgumentType.word())
                    .requires(source -> source.hasPermission(2))
                    .executes(context -> revoke(context.getSource(), StringArgumentType.getString(context, "player")))))
            .then(Commands.literal("list")
                .requires(source -> source.hasPermission(2))
                .executes(context -> list(context.getSource())))
            .then(Commands.literal("add")
                .requires(source -> source.hasPermission(2))
                .then(Commands.argument("player", StringArgumentType.word())
                    .executes(context -> add(context.getSource(), StringArgumentType.getString(context, "player")))))
            .then(Commands.literal("remove")
                .requires(source -> source.hasPermission(2))
                .then(Commands.argument("player", StringArgumentType.word())
                    .executes(context -> remove(context.getSource(), StringArgumentType.getString(context, "player")))));
    }

    private static LiteralArgumentBuilder<CommandSourceStack> debugNanomachinesCommand(final String name) {
        return Commands.literal(name)
            .requires(source -> source.hasPermission(2))
            .executes(context -> debugNanomachines(context.getSource()));
    }

    private static LiteralArgumentBuilder<CommandSourceStack> logNanomachinesCommand(final String name) {
        return Commands.literal(name)
            .requires(source -> source.hasPermission(2))
            .executes(context -> logNanomachines(context.getSource(), context.getSource().getPlayerOrException()))
            .then(Commands.argument("player", EntityArgument.player())
                .executes(context -> logNanomachines(context.getSource(), EntityArgument.getPlayer(context, "player"))));
    }

    private static int debugNanomachines(final CommandSourceStack source) throws CommandSyntaxException {
        if (API.nanomachines instanceof NanomachinesRegistry registry) {
            registry.debugController(source.getPlayerOrException());
            source.sendSuccess(() -> Component.literal("Debug configuration created, see log for mappings."), false);
            return 1;
        }
        throw NANOMACHINES_UNAVAILABLE.create();
    }

    private static int logNanomachines(final CommandSourceStack source, final ServerPlayer player) throws CommandSyntaxException {
        if (API.nanomachines instanceof NanomachinesRegistry registry) {
            final List<String> lines = registry.controllerConfigurationLines(player);
            if (lines.isEmpty()) {
                player.sendSystemMessage(Component.literal("<empty>"));
            } else {
                for (final String line : lines) {
                    player.sendSystemMessage(Component.literal(line));
                }
            }
            return Math.max(1, lines.size());
        }
        throw NANOMACHINES_UNAVAILABLE.create();
    }

    private static int revoke(final CommandSourceStack source, final String player) throws CommandSyntaxException {
        final DebugCardWhitelist whitelist = whitelist();
        try {
            if (whitelist.isWhitelisted(player)) {
                whitelist.invalidate(player);
                source.sendSuccess(() -> Component.literal("§aAll your debug cards were invalidated."), false);
            } else {
                source.sendFailure(Component.literal("§cYou are not whitelisted to use debug card."));
            }
            return 1;
        } catch (final IOException e) {
            throw new CommandSyntaxException(new SimpleCommandExceptionType(Component.literal(e.getMessage())), Component.literal(e.getMessage()));
        }
    }

    private static int list(final CommandSourceStack source) throws CommandSyntaxException {
        final Set<String> players = whitelist().whitelist();
        if (players.isEmpty()) {
            source.sendFailure(Component.literal("§cThere is no currently whitelisted players."));
        } else {
            source.sendSuccess(() -> Component.literal("§aCurrently whitelisted players: §e" + String.join(", ", players)), false);
        }
        return 1;
    }

    private static int add(final CommandSourceStack source, final String player) throws CommandSyntaxException {
        try {
            whitelist().add(player);
            source.sendSuccess(() -> Component.literal("§aPlayer was added to whitelist."), false);
            return 1;
        } catch (final IOException e) {
            throw new CommandSyntaxException(new SimpleCommandExceptionType(Component.literal(e.getMessage())), Component.literal(e.getMessage()));
        }
    }

    private static int remove(final CommandSourceStack source, final String player) throws CommandSyntaxException {
        try {
            whitelist().remove(player);
            source.sendSuccess(() -> Component.literal("§aPlayer was removed from whitelist"), false);
            return 1;
        } catch (final IOException e) {
            throw new CommandSyntaxException(new SimpleCommandExceptionType(Component.literal(e.getMessage())), Component.literal(e.getMessage()));
        }
    }

    private static DebugCardWhitelist whitelist() throws CommandSyntaxException {
        if (!"whitelist".equals(ModSettings.debugCardAccess())) {
            throw WHITELIST_DISABLED.create();
        }
        return DebugCardWhitelist.instance();
    }
}
