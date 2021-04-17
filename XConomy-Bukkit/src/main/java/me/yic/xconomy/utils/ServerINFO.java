/*
 *  This file (ServerINFO.java) is a part of project XConomy
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
package me.yic.xconomy.utils;

import org.bukkit.configuration.file.FileConfiguration;

public class ServerINFO {

    public static FileConfiguration DataBaseINFO;

    public static boolean IsBungeeCordMode = false;

    public static boolean IsSemiOnlineMode = false;

    public static String Lang;

    public static boolean EnableConnectionPool = false;

    public static String Sign;

    public static boolean DDrivers = false;

    public static Double InitialAmount = 0.0;

    public static boolean IgnoreCase = false;
}