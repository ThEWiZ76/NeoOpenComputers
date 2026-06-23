package li.cil.oc.common.command;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import li.cil.oc.common.DebugCardWhitelist;
import li.cil.oc.common.ModSettings;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

import java.io.IOException;
import java.util.Set;

public final class ModCommands {
    private static final SimpleCommandExceptionType WHITELIST_DISABLED =
        new SimpleCommandExceptionType(Component.literal("§cDebug card whitelisting is not enabled."));

    private ModCommands() {
    }

    public static void register(final RegisterCommandsEvent event) {
        event.getDispatcher().register(debugWhitelistCommand());
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
