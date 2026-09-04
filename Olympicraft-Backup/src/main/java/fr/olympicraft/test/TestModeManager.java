package fr.olympicraft.test;

import net.minecraft.server.MinecraftServer;

public final class TestModeManager {

    private boolean enabled;
    private MinecraftServer server;

    public void attachServer(MinecraftServer server) {
        this.server = server;
    }

    public void detachServer() {
        enabled = false;
        server = null;
    }

    public boolean enable() {
        if (enabled) {
            return false;
        }

        enabled = true;
        return true;
    }

    public boolean disable() {
        if (!enabled) {
            return false;
        }

        enabled = false;
        return true;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public boolean isServerAttached() {
        return server != null;
    }

    public MinecraftServer getServer() {
        return server;
    }
}
