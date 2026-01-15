package com.github.grule.teleportrequests.commands;

import com.github.grule.teleportrequests.TeleportRequest;
import com.github.grule.teleportrequests.TeleportRequests;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.protocol.GameMode;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.arguments.system.RequiredArg;
import com.hypixel.hytale.server.core.command.system.arguments.types.ArgTypes;
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractPlayerCommand;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nonnull;

public class RequestTeleportHereCommand extends AbstractPlayerCommand {
    private static final Message MESSAGE_COMMANDS_TELEPORT_REQUEST = Message.translation("teleport_requests.commands.tphere.target");
    private static final Message MESSAGE_COMMANDS_TELEPORT_REQUEST_SENT = Message.translation("teleport_requests.commands.tphere.requester");
    private static final Message MESSAGE_COMMANDS_TELEPORT_CANNOT_REQUEST_SELF = Message.translation("teleport_requests.commands.tphere.self");

    @Nonnull
    private final RequiredArg<PlayerRef> targetPlayerArg;
    @Nonnull
    private final TeleportRequests plugin;

    public RequestTeleportHereCommand() {
        super("tphere", "teleport_requests.commands.tphere.desc");
        this.plugin = TeleportRequests.get();
        this.setPermissionGroup(GameMode.Adventure);
        this.addAliases("tph");

        this.targetPlayerArg = this.withRequiredArg("targetPlayer", "teleport_requests.commands.tphere.args.targetPlayer", ArgTypes.PLAYER_REF);
    }

    @Override
    protected void execute(@NotNull CommandContext context, @NotNull Store<EntityStore> store, @NotNull Ref<EntityStore> ref, @NotNull PlayerRef playerRef, @NotNull World world) {
        PlayerRef targetPlayerRef = this.targetPlayerArg.get(context);
        Ref<EntityStore> targetRef = targetPlayerRef.getReference();

        if (targetRef == null || !targetRef.isValid()) {
            return;
        }

        if (playerRef.getUuid().equals(targetPlayerRef.getUuid())) {
            playerRef.sendMessage(MESSAGE_COMMANDS_TELEPORT_CANNOT_REQUEST_SELF);
            return;
        }

        plugin.addTeleportRequest(playerRef.getUuid(), targetPlayerRef.getUuid(), TeleportRequest.Type.TELEPORT_HERE);

        targetPlayerRef.sendMessage(MESSAGE_COMMANDS_TELEPORT_REQUEST.param("username", playerRef.getUsername()));
        playerRef.sendMessage(MESSAGE_COMMANDS_TELEPORT_REQUEST_SENT.param("username", targetPlayerRef.getUsername()));
    }
}
