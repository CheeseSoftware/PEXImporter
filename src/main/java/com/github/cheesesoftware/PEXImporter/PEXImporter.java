package com.github.cheesesoftware.PEXImporter;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map.Entry;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.event.Listener;
import org.bukkit.plugin.java.JavaPlugin;

import com.github.cheesesoftware.PowerfulPermsAPI.Group;
import com.github.cheesesoftware.PowerfulPermsAPI.PermissionManager;
import com.github.cheesesoftware.PowerfulPermsAPI.PowerfulPermsPlugin;
import com.github.cheesesoftware.PowerfulPermsAPI.ResponseRunnable;

public class PEXImporter extends JavaPlugin implements Listener {

    public List<PowerfulPermission> getPermissions(ConfigurationSection section) {
        List<PowerfulPermission> permissions = new ArrayList<PowerfulPermission>();

        List<String> ownPermissions = section.getStringList("permissions");
        if (ownPermissions != null) {
            for (String permission : ownPermissions) {
                permissions.add(new PowerfulPermission(-1, permission, "", "", null));
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
                            permissions.add(new PowerfulPermission(-1, permission, worldName, "", null));
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

            final HashMap<String, List<String>> groupParents = new HashMap<String, List<String>>();

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
                groupParents.put(groupName, parents);

                final String prefixFinal = prefix;
                final String suffixFinal = suffix;
                final List<PowerfulPermission> permissionsFinal = permissions;

                permissionManager.createGroup(groupName, ladder, rank, new ResponseRunnable(true) {

                    @Override
                    public void run() {
                        if (super.success) {
                            final int groupId = permissionManager.getGroup(groupName).getId();
                            getLogger().info("Imported group " + groupName);
                            permissionManager.setGroupPrefix(groupId, prefixFinal, new ResponseRunnable(true) {

                                @Override
                                public void run() {
                                    if (super.success) {
                                        getLogger().info("Set group " + groupName + " prefix to \"" + prefixFinal + "\"");
                                        permissionManager.setGroupSuffix(groupId, suffixFinal, new ResponseRunnable(true) {

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

                            for (final PowerfulPermission permission : permissionsFinal) {
                                permissionManager.addGroupPermission(groupId, permission.getPermissionString(), permission.getWorld(), permission.getServer(), null, new ResponseRunnable(true) {

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

            for (Entry<String, List<String>> e : groupParents.entrySet()) {
                final String groupName = e.getKey();
                Group group = permissionManager.getGroup(groupName);
                if (group == null) {
                    getLogger().severe("Could add parent to null group " + groupName);
                    continue;
                }
                for (final String parent : e.getValue()) {
                    Group parentGroup = permissionManager.getGroup(parent);
                    if (parentGroup == null) {
                        getLogger().severe("Could not add null parent " + parent + " to group " + groupName);
                        continue;
                    }
                    permissionManager.addGroupParent(group.getId(), parentGroup.getId(), new ResponseRunnable(true) {

                        @Override
                        public void run() {
                            if (success)
                                getLogger().info("Added group parent " + parent + " to group " + groupName);
                            else
                                getLogger().severe("Could not add group parent. " + response);
                        }
                    });
                }
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
                                permissionManager.addPlayerPermission(uuid, userNameFinal, permission.getPermissionString(), permission.getWorld(), permission.getServer(), null,
                                        new ResponseRunnable(true) {

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
                                    Group group = permissionManager.getGroup(groupName);
                                    permissionManager.addPlayerGroup(uuid, group.getId(), new ResponseRunnable(true) {

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

}
