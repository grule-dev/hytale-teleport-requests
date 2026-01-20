package com.github.grule.teleportrequests;

import com.github.grule.teleportrequests.commands.AcceptTeleportCommand;
import com.github.grule.teleportrequests.commands.CancelTeleportCommand;
import com.github.grule.teleportrequests.commands.RequestTeleportCommand;
import com.github.grule.teleportrequests.commands.RequestTeleportHereCommand;
import com.hypixel.hytale.server.core.HytaleServer;
import com.hypixel.hytale.server.core.plugin.JavaPlugin;
import com.hypixel.hytale.server.core.plugin.JavaPluginInit;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;

/**
 * Main plugin class.
 * 
 * @author Grule
 * @version 1.0.0
 */
public class TeleportRequests extends JavaPlugin {

    public static final int REQUEST_TIMEOUT_SECONDS = 30;
    private static final int CLEANUP_INTERVAL_SECONDS = 30;

    private static TeleportRequests instance;
    private final Map<UUID, TeleportRequest> teleportRequests = new ConcurrentHashMap<>();
    private ScheduledFuture<Void> cleanupTask;

    public TeleportRequests(@Nonnull JavaPluginInit init) {
        super(init);
    }

    @Override
    protected void setup() {
        instance = this;
        var commandRegistry = this.getCommandRegistry();
        commandRegistry.registerCommand(new RequestTeleportCommand());
        commandRegistry.registerCommand(new RequestTeleportHereCommand());
        commandRegistry.registerCommand(new AcceptTeleportCommand());
        commandRegistry.registerCommand(new CancelTeleportCommand());

        startCleanupTask();

        this.getLogger().at(Level.INFO).log("[TeleportPlugin] Initialized Successfully");
    }

    @Override
    protected void shutdown() {
        if (cleanupTask != null && !cleanupTask.isCancelled()) {
            cleanupTask.cancel(false);
        }
    }

    @SuppressWarnings("unchecked")
    private void startCleanupTask() {
        cleanupTask = (ScheduledFuture<Void>) HytaleServer.SCHEDULED_EXECUTOR.scheduleAtFixedRate(
                this::cleanupExpiredRequests,
                CLEANUP_INTERVAL_SECONDS,
                CLEANUP_INTERVAL_SECONDS,
                TimeUnit.SECONDS);

        this.getTaskRegistry().registerTask(cleanupTask);
    }

    private void cleanupExpiredRequests() {
        teleportRequests.entrySet().removeIf(entry -> entry.getValue().isExpired());
    }

    /**
     * @return If the request was successful or not
     */
    public boolean addTeleportRequest(UUID requester, UUID target, TeleportRequest.Type type) {
        var existingRequest = teleportRequests.get(requester);
        if (existingRequest != null && !existingRequest.isExpired()) {
            return false;
        }

        var request = TeleportRequest.create(requester, target, REQUEST_TIMEOUT_SECONDS, type);
        teleportRequests.put(requester, request);
        return true;
    }

    @Nullable
    public TeleportRequest findRequestForTarget(UUID targetUuid) {
        return teleportRequests.values().stream()
                .filter(request -> request.getTarget().equals(targetUuid))
                .findFirst()
                .orElse(null);
    }

    @Nullable
    public TeleportRequest findRequestFromRequester(UUID requesterUuid) {
        return teleportRequests.get(requesterUuid);
    }

    public void removeRequest(UUID requester) {
        teleportRequests.remove(requester);
    }

    /**
     * Get plugin instance.
     */
    public static TeleportRequests get() {
        return instance;
    }
}