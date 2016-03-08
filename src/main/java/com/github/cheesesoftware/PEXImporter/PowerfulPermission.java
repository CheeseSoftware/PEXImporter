package com.github.cheesesoftware.PEXImporter;

import com.github.cheesesoftware.PowerfulPermsAPI.Permission;

public class PowerfulPermission implements Permission {
    private String permission;
    private String world = "";
    private String server = "";

    public PowerfulPermission(String permission) {
        this.permission = permission;
    }

    public PowerfulPermission(String permission, String world, String server) {
        this.permission = permission;

        if (world != null && !world.isEmpty() && !world.equalsIgnoreCase("ALL"))
            this.world = world;
        if (server != null && !server.isEmpty() && !server.equalsIgnoreCase("ALL"))
            this.server = server;
    }

    @Override
    public String getPermissionString() {
        return this.permission;
    }

    @Override
    public String getWorld() {
        return this.world;
    }

    @Override
    public String getServer() {
        return this.server;
    }
}
