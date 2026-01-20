package com.github.grule.teleportrequests.commands;

import com.github.grule.teleportrequests.TeleportRequests;
import com.github.grule.teleportrequests.TeleportRequest;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.protocol.GameMode;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.arguments.system.OptionalArg;
import com.hypixel.hytale.server.core.command.system.arguments.types.ArgTypes;
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractPlayerCommand;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.Universe;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.awt.*;
import java.util.UUID;

public class CancelTeleportCommand extends AbstractPlayerCommand {
    private static final Message MESSAGE_NO_PENDING_REQUEST = Message.translation("teleport_requests.commands.tpcancel.none").color(new Color(0xff3333));
    private static final Message MESSAGE_REQUEST_CANCELLED_TARGET = Message.translation("teleport_requests.commands.tpcancel.success.target").color(Color.YELLOW);
    private static final Message MESSAGE_REQUEST_CANCELLED_REQUESTER = Message.translation("teleport_requests.commands.tpcancel.success.requester").color(Color.YELLOW);

    @Nonnull
    private final OptionalArg<PlayerRef> requesterPlayerArg;
    @Nonnull
    private final TeleportRequests plugin;

    public CancelTeleportCommand() {
        super("tpcancel", "teleport_requests.commands.tpcancel.desc");
        this.setPermissionGroup(GameMode.Adventure);
        this.plugin = TeleportRequests.get();

        this.requesterPlayerArg = this.withOptionalArg("requesterPlayer", "teleport_requests.commands.tpcancel.args.requesterPlayer", ArgTypes.PLAYER_REF);
    }

    @Override
    protected void execute(@NotNull CommandContext context, @NotNull Store<EntityStore> store, @NotNull Ref<EntityStore> ref, @NotNull PlayerRef playerRef, @NotNull World world) {
        UUID targetUuid = playerRef.getUuid();
        PlayerRef requesterPlayer = this.requesterPlayerArg.get(context);

        TeleportRequest request;
        if (requesterPlayer == null) {
            request = this.plugin.findRequestForTarget(targetUuid);
        } else {
            request = this.plugin.findRequestFromRequester(requesterPlayer.getUuid());
            // Verify the request is actually for the current player
            if (request != null && !request.getTarget().equals(targetUuid)) {
                request = null;
            }
        }

        if (request == null) {
            playerRef.sendMessage(MESSAGE_NO_PENDING_REQUEST);
            return;
        }

        PlayerRef requesterRef = getPlayerByUuid(request.getRequester());
        if (requesterRef != null) {
            requesterRef.sendMessage(MESSAGE_REQUEST_CANCELLED_REQUESTER.param("username", playerRef.getUsername()));
        }

        playerRef.sendMessage(MESSAGE_REQUEST_CANCELLED_TARGET.param("username", requesterRef != null ? requesterRef.getUsername() : "Unknown"));
        plugin.removeRequest(request.getRequester());
    }

    @Nullable
    private PlayerRef getPlayerByUuid(UUID uuid) {
        return Universe.get().getPlayer(uuid);
    }
}
