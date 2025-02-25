/*
 *  This file (GetUUID.java) is a part of project XConomy
 *  Copyright (C) YiC and contributors
 *
 *  This program is free software: you can redistribute it and/or modify it
 *  under the terms of the GNU General Public License as published by the
 *  Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful, but
 *  WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 *  or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 *  for more details.
 *
 *  You should have received a copy of the GNU General Public License along
 *  with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 */
package me.yic.xconomy.data;

import me.yic.xconomy.XConomy;
import me.yic.xconomy.info.ServerINFO;
import ninja.leaping.configurate.ConfigurationNode;
import ninja.leaping.configurate.yaml.YAMLConfigurationLoader;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.text.Text;

import java.net.URL;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class GetUUID {
    private static final Map<String, UUID> cache = new ConcurrentHashMap<>();

    private static String doGetUUID(Player pp, String name) {
        String URL = "https://api.mojang.com/users/profiles/minecraft/" + name;
        StringBuilder uuid = new StringBuilder();
        try {
            URL url = new URL(URL);
            YAMLConfigurationLoader proloader = YAMLConfigurationLoader.builder().setURL(url).build();
            ConfigurationNode proFile = proloader.load();
            String u = proFile.getNode("id").getString();
            if (u != null) {
                for (int i = 0; i <= 31; i++) {
                    uuid.append(u.charAt(i));
                    if (i == 7 || i == 11 || i == 15 || i == 19) {
                        uuid.append("-");
                    }
                }
            }
        } catch (Exception e) {
            kickplayer(pp);
            e.printStackTrace();
        }
        return uuid.toString();
    }

    private static void kickplayer(Player pp) {
        if (pp != null) {
            Sponge.getScheduler().createAsyncExecutor(XConomy.getInstance()).execute(() ->
                    pp.kick(Text.of("Failed to Get profile")));
        }
    }


    public static UUID getUUID(Player pp, String name) {
        if (CacheContainsKey(name)) {
            return getUUIDFromCache(name);
        }

        UUID u = null;
        try {
            u = UUID.fromString(doGetUUID(pp, name));
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
        }

        if (u != null) {
            insertIntoCache(name, u);
        }

        return u;
    }


    private static void insertIntoCache(final String name, final UUID uuid) {
        if (ServerINFO.IgnoreCase) {
            cache.put(name.toLowerCase(), uuid);
        } else {
            cache.put(name, uuid);
        }
    }

    private static boolean CacheContainsKey(final String name) {
        if (ServerINFO.IgnoreCase) {
            return cache.containsKey(name.toLowerCase());
        }
        return cache.containsKey(name);
    }


    private static UUID getUUIDFromCache(final String name) {
        if (ServerINFO.IgnoreCase) {
            return cache.get(name.toLowerCase());
        } else {
            return cache.get(name);
        }
    }
}

