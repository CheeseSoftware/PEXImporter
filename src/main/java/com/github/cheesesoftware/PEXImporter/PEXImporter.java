package com.github.cheesesoftware.PEXImporter;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.event.Listener;
import org.bukkit.plugin.java.JavaPlugin;

import com.github.cheesesoftware.PowerfulPermsAPI.PermissionManager;
import com.github.cheesesoftware.PowerfulPermsAPI.PowerfulPermsPlugin;
import com.github.cheesesoftware.PowerfulPermsAPI.ResponseRunnable;

public class PEXImporter extends JavaPlugin implements Listener {

    public List<PowerfulPermission> getPermissions(ConfigurationSection section) {
        List<PowerfulPermission> permissions = new ArrayList<PowerfulPermission>();

        List<String> ownPermissions = section.getStringList("permissions");
        if (ownPermissions != null) {
            for (String permission : ownPermissions) {
                permissions.add(new PowerfulPermission(permission, "", ""));
            }
        }

        ConfigurationSection worldsSection = section.getConfigurationSection("worlds");
        if (worldsSection != null) {
            for (String worldName : worldsSection.getKeys(false)) {
                ConfigurationSection worldSection = worldsSection.getConfigurationSection(worldName);
                if (worldSection != null) {

                    ownPermissions = worldSection.getStringList("permissions");
                    if (ownPermissions != null) {
                        for (String permission : ownPermissions) {
                            permissions.add(new PowerfulPermission(permission, worldName, ""));
                        }
                    }

                    // Will not import world specific prefix
                }
            }
        }

        return permissions;
    }

    public void onEnable() {
        // getServer().getPluginManager().registerEvents(this, this);
        getDataFolder().mkdir();

        if (Bukkit.getPluginManager().isPluginEnabled("PowerfulPerms")) {
            PowerfulPermsPlugin plugin = (PowerfulPermsPlugin) Bukkit.getPluginManager().getPlugin("PowerfulPerms");
            final PermissionManager permissionManager = plugin.getPermissionManager();

            File customConfigFile = new File(getDataFolder(), "permissions.yml");
            if (customConfigFile == null || !customConfigFile.exists()) {
                getLogger().severe("Place your PEX permissions.yml in PEXImporter's plugin folder.");
                return;
            }

            FileConfiguration customConfig = YamlConfiguration.loadConfiguration(customConfigFile);

            // Begin import
            ConfigurationSection groups = customConfig.getConfigurationSection("groups");
            for (final String groupName : groups.getKeys(false)) {
                ConfigurationSection group = groups.getConfigurationSection(groupName);
                String ladder = "default";
                int rank = 100;
                String prefix = "";
                String suffix = "";
                List<String> parents = new ArrayList<String>();
                List<PowerfulPermission> permissions = getPermissions(group);

                ConfigurationSection options = group.getConfigurationSection("options");
                if (options != null) {
                    if (options.getString("ladder-rank") != null)
                        ladder = options.getString("ladder-rank");
                    if (options.getString("rank") != null)
                        rank = Integer.parseInt(options.getString("rank"));
                    if (options.getString("prefix") != null)
                        prefix = options.getString("prefix");
                    if (options.getString("suffix") != null)
                        suffix = options.getString("suffix");
                }

                List<String> parentsList = group.getStringList("inheritance");
                if (parentsList != null) {
                    for (String parent : parentsList) {
                        parents.add(parent);
                    }
                }

                final String prefixFinal = prefix;
                final String suffixFinal = suffix;
                final List<String> parentsFinal = parents;
                final List<PowerfulPermission> permissionsFinal = permissions;

                permissionManager.createGroup(groupName, ladder, rank, new ResponseRunnable(true) {

                    @Override
                    public void run() {
                        if (super.success) {
                            getLogger().info("Imported group " + groupName);
                            permissionManager.setGroupPrefix(groupName, prefixFinal, new ResponseRunnable(true) {

                                @Override
                                public void run() {
                                    if (super.success) {
                                        getLogger().info("Set group " + groupName + " prefix to \"" + prefixFinal + "\"");
                                        permissionManager.setGroupSuffix(groupName, suffixFinal, new ResponseRunnable(true) {

                                            @Override
                                            public void run() {
                                                if (super.success) {
                                                    getLogger().info("Set group " + groupName + " suffix to \"" + suffixFinal + "\"");
                                                } else
                                                    getLogger().severe("Could not set group suffix of group " + groupName + ". " + response);
                                            }
                                        });
                                    } else
                                        getLogger().severe("Could not set group prefix of group " + groupName + ". " + response);
                                }
                            });

                            for (final String parent : parentsFinal) {
                                permissionManager.addGroupParent(groupName, parent, new ResponseRunnable(true) {

                                    @Override
                                    public void run() {
                                        if (success)
                                            getLogger().info("Added group parent " + parent + " to group " + groupName);
                                        else
                                            getLogger().severe("Could not add group parent. " + response);
                                    }
                                });
                            }

                            for (final PowerfulPermission permission : permissionsFinal) {
                                permissionManager.addGroupPermission(groupName, permission.getPermissionString(), permission.getWorld(), permission.getServer(), new ResponseRunnable(true) {

                                    @Override
                                    public void run() {
                                        if (success)
                                            getLogger().info("Added group permission " + permission.getPermissionString() + " to group " + groupName);
                                        else
                                            getLogger().severe("Could not add group permission. " + response);
                                    }
                                });
                            }
                        } else
                            getLogger().severe("Could not create group " + groupName + ". " + response);
                    }
                });
            }
            ConfigurationSection users = customConfig.getConfigurationSection("users");
            for (final String userUUID : users.getKeys(false)) {
                UUID uuidTemp;
                try {
                    uuidTemp = UUID.fromString(userUUID);
                } catch (IllegalArgumentException e) {
                    continue;
                }
                ConfigurationSection user = users.getConfigurationSection(userUUID);
                String userName = "";
                String prefix = "";
                String suffix = "";
                List<PowerfulPermission> permissions = getPermissions(user);

                ConfigurationSection options = user.getConfigurationSection("options");
                if (options != null) {
                    if (options.getString("name") != null)
                        userName = options.getString("name");
                    if (options.getString("prefix") != null)
                        prefix = options.getString("prefix");
                    if (options.getString("suffix") != null)
                        suffix = options.getString("suffix");
                }

                List<String> group = user.getStringList("group");
                final List<String> groupFinal = new ArrayList<String>(group);

                final UUID uuid = uuidTemp;
                final String userNameFinal = userName;
                final String prefixFinal = prefix;
                final String suffixFinal = suffix;
                final List<PowerfulPermission> permissionsFinal = new ArrayList<PowerfulPermission>(permissions);

                permissionManager.createPlayer(userName, uuid, new ResponseRunnable(true) {

                    @Override
                    public void run() {
                        if (super.success) {
                            getLogger().info("Imported player " + userNameFinal + " with UUID " + uuid);
                            permissionManager.setPlayerPrefix(uuid, prefixFinal, new ResponseRunnable(true) {

                                @Override
                                public void run() {
                                    if (super.success) {
                                        getLogger().info("Set player " + userNameFinal + " prefix to \"" + prefixFinal + "\"");
                                        permissionManager.setPlayerSuffix(uuid, suffixFinal, new ResponseRunnable(true) {

                                            @Override
                                            public void run() {
                                                if (super.success) {
                                                    getLogger().info("Set player " + userNameFinal + " suffix to \"" + suffixFinal + "\"");
                                                } else
                                                    getLogger().severe("Could not set player suffix. " + response);
                                            }
                                        });
                                    } else
                                        getLogger().severe("Could not set player prefix. " + response);
                                }
                            });

                            for (final PowerfulPermission permission : permissionsFinal) {
                                permissionManager.addPlayerPermission(uuid, userNameFinal, permission.getPermissionString(), permission.getWorld(), permission.getServer(), new ResponseRunnable(true) {

                                    @Override
                                    public void run() {
                                        if (success)
                                            getLogger().info("Added player permission " + permission.getPermissionString() + " to player " + userNameFinal);
                                        else
                                            getLogger().severe("Could not add player permission. " + response);
                                    }
                                });
                            }

                            if (groupFinal != null) {
                                for (final String groupName : groupFinal) {
                                    permissionManager.addPlayerGroup(uuid, groupName, new ResponseRunnable() {

                                        @Override
                                        public void run() {
                                            if (success)
                                                getLogger().info("Added player group " + groupName + " to player " + userNameFinal);
                                            else
                                                getLogger().severe("Could not add player group. " + response);
                                        }
                                    });
                                }
                            }
                        } else
                            getLogger().severe("Could not create player. " + response);
                    }
                });
            }

        } else
            getLogger().severe("PowerfulPerms has to be loaded to be able to import permissions.");
    }

    public void onDisable() {

    }

}
