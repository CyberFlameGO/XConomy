/*
 *  This file (SQL.java) is a part of project XConomy
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
package me.yic.xconomy.data.sql;

import me.yic.xconomy.XConomy;
import me.yic.xconomy.data.DataFormat;
import me.yic.xconomy.data.GetUUID;
import me.yic.xconomy.data.caches.Cache;
import me.yic.xconomy.data.caches.CacheNonPlayer;
import me.yic.xconomy.info.DataBaseINFO;
import me.yic.xconomy.info.ServerINFO;
import me.yic.xconomy.utils.DatabaseConnection;
import me.yic.xconomy.utils.PlayerData;

import java.math.BigDecimal;
import java.sql.*;
import java.util.Date;
import java.util.List;
import java.util.UUID;

public class SQL {

    public static String tableName = "xconomy";
    public static String tableNonPlayerName = "xconomynon";
    public static String tableRecordName = "xconomyrecord";
    public final static DatabaseConnection database = new DatabaseConnection();
    private static final String encoding = DataBaseINFO.DataBaseINFO.getNode("MySQL", "property", "encoding").getString();

    public static boolean con() {
        return database.setGlobalConnection();
    }

    public static void close() {
        database.close();
    }

    public static void getwaittimeout() {
        if (DataBaseINFO.isMySQL() && !ServerINFO.EnableConnectionPool) {
            try {
                Connection connection = database.getConnectionAndCheck();

                String query = "show variables like 'wait_timeout'";

                PreparedStatement statement = connection.prepareStatement(query);

                ResultSet rs = statement.executeQuery();
                if (rs.next()) {
                    int waittime = rs.getInt(2);
                    if (waittime > 50) {
                        database.waittimeout = waittime - 30;
                    }

                }

                rs.close();
                statement.close();
                database.closeHikariConnection(connection);

            } catch (SQLException ignored) {
                XConomy.getInstance().logger("Get 'wait_timeout' error", 1, null);
            }
        }
    }

    public static void createTable() {
        try {
            Connection connection = database.getConnectionAndCheck();
            Statement statement = connection.createStatement();

            if (statement == null) {
                return;
            }

            String query1;
            String query2;
            String query3 = "CREATE TABLE IF NOT EXISTS " + tableRecordName
                    + "(id int(20) not null auto_increment, type varchar(50) not null, uid varchar(50) not null, player varchar(50) not null,"
                    + "balance double(20,2), amount double(20,2) not null, operation varchar(50) not null,"
                    + " date varchar(50) not null, command varchar(50) not null,"
                    + "primary key (id)) DEFAULT CHARSET = " + encoding + ";";
            if (DataBaseINFO.isMySQL()) {
                query1 = "CREATE TABLE IF NOT EXISTS " + tableName
                        + "(UID varchar(50) not null, player varchar(50) not null, balance double(20,2) not null, hidden int(5) not null, "
                        + "primary key (UID)) DEFAULT CHARSET = " + encoding + ";";
                query2 = "CREATE TABLE IF NOT EXISTS " + tableNonPlayerName
                        + "(account varchar(50) not null, balance double(20,2) not null, "
                        + "primary key (account)) DEFAULT CHARSET = " + encoding + ";";
            } else {
                query1 = "CREATE TABLE IF NOT EXISTS " + tableName
                        + "(UID varchar(50) not null, player varchar(50) not null, balance double(20,2) not null, hidden int(5) not null, "
                        + "primary key (UID));";
                query2 = "CREATE TABLE IF NOT EXISTS " + tableNonPlayerName
                        + "(account varchar(50) not null, balance double(20,2) not null, "
                        + "primary key (account));";
            }
            statement.executeUpdate(query1);
            if (XConomy.config.getNode("Settings", "non-player-account").getBoolean()) {
                statement.executeUpdate(query2);
            }
            if (DataBaseINFO.isMySQL() && XConomy.config.getNode("Settings", "transaction-record").getBoolean()) {
                statement.executeUpdate(query3);
            }
            statement.close();
            database.closeHikariConnection(connection);

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public static void getPlayerData(UUID uuid) {
        try {
            Connection connection = database.getConnectionAndCheck();
            PreparedStatement statement = connection.prepareStatement("select * from " + tableName + " where UID = ?");
            statement.setString(1, uuid.toString());

            ResultSet rs = statement.executeQuery();

            if (rs.next()) {
                BigDecimal cacheThisAmt = DataFormat.formatString(rs.getString(3));
                if (cacheThisAmt != null) {
                    PlayerData bd = new PlayerData(uuid, rs.getString(2), cacheThisAmt);
                    Cache.insertIntoCache(uuid, bd);
                }
            }

            rs.close();
            statement.close();
            database.closeHikariConnection(connection);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public static void getPlayerData(String name) {
        try {
            Connection connection = database.getConnectionAndCheck();
            String query;

            if (ServerINFO.IgnoreCase) {
                if (DataBaseINFO.isMySQL()) {
                    query = "select * from " + tableName + " where player = ?";
                } else {
                    query = "select * from " + tableName + " where player = ? COLLATE NOCASE";
                }
            } else {
                if (DataBaseINFO.isMySQL()) {
                    query = "select * from " + tableName + " where binary player = ?";
                } else {
                    query = "select * from " + tableName + " where player = ?";
                }
            }

            PreparedStatement statement = connection.prepareStatement(query);
            statement.setString(1, name);
            ResultSet rs = statement.executeQuery();
            while (rs.next()) {
                UUID uuid = UUID.fromString(rs.getString(1));
                UUID puuid = null;
                if (ServerINFO.IsOnlineMode) {
                    puuid = GetUUID.getUUID(null, name);
                }
                if (!ServerINFO.IsOnlineMode || (puuid != null && uuid.toString().equalsIgnoreCase(puuid.toString()))) {
                    String username = rs.getString(2);
                    BigDecimal cacheThisAmt = DataFormat.formatString(rs.getString(3));
                    if (cacheThisAmt != null) {
                        PlayerData bd = new PlayerData(uuid, username, cacheThisAmt);
                        Cache.insertIntoCache(uuid, bd);
                    }
                    break;
                }
            }

            rs.close();
            statement.close();
            database.closeHikariConnection(connection);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }


    public static void getNonPlayerData(String playerName) {
        try {
            Connection connection = database.getConnectionAndCheck();
            String query;

            if (DataBaseINFO.isMySQL()) {
                query = "select * from " + tableNonPlayerName + " where binary account = ?";
            } else {
                query = "select * from " + tableNonPlayerName + " where account = ?";
            }

            PreparedStatement statement = connection.prepareStatement(query);
            statement.setString(1, playerName);

            ResultSet rs = statement.executeQuery();
            if (rs.next()) {
                CacheNonPlayer.insertIntoCache(playerName, DataFormat.formatString(rs.getString(2)));
            } else {
                SQLCreateNewAccount.createNonPlayerAccount(playerName, 0.0, connection);
                CacheNonPlayer.insertIntoCache(playerName, BigDecimal.ZERO);
            }

            rs.close();
            statement.close();
            database.closeHikariConnection(connection);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public static void save(String type, PlayerData pd, Boolean isAdd, BigDecimal amount, String command) {
        Connection connection = database.getConnectionAndCheck();
        try {
            String query = " set balance = ? where UID = ?";
            PreparedStatement statement = connection.prepareStatement("update " + tableName + query);
            statement.setDouble(1, pd.getbalance().doubleValue());
            statement.setString(2, pd.getUniqueId().toString());
            statement.executeUpdate();
            statement.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
        record(connection, type, pd, isAdd, amount, pd.getbalance(), command);
        database.closeHikariConnection(connection);
    }

    //public static void save(String type, PlayerData pd, Boolean isAdd,
    //                        BigDecimal oldbalance, BigDecimal amount, String command) {
    //    Connection connection = database.getConnectionAndCheck();
    //    UUID u = pd.getUniqueId();
    //    try {
    //        String query;
    //        query = " set balance = " + pd.getbalance().doubleValue() + " where UID = ?";
    //        boolean requirefresh = false;
    //        if (isAdd != null) {
    //            requirefresh = true;
    //            query = query + "AND balance = " + oldbalance.toString();
    //        }
    //        PreparedStatement statement1 = connection.prepareStatement("update " + tableName + query);
    //        statement1.setString(1, u.toString());
    //        int rs = statement1.executeUpdate();
    //        statement1.close();
    //        if (requirefresh && rs == 0) {
    //            command += "(Cache Correction)";
    //        }
    //        record(connection, type, pd, isAdd, amount, pd.getbalance(), command);
    //        if (requirefresh && rs == 0) {
    //            DataCon.refreshFromCache(u);
    //            PlayerData npd = DataCon.cachecorrection(u, amount, isAdd);
    //            if (isAdd) {
    //               query = " set balance = balance + " + amount.doubleValue() + " where UID = ?";
    //            } else {
    //                query = " set balance = balance - " + amount.doubleValue() + " where UID = ?";
    //            }
    //            PreparedStatement statement2 = connection.prepareStatement("update " + tableName + query);
    //            statement2.setString(1, u.toString());
    //            statement2.executeUpdate();
    //            statement2.close();
    //            record(connection, type, npd, isAdd, amount, npd.getbalance(), "Cache Correction Detail");
    //        }
    //    } catch (SQLException e) {
    //        e.printStackTrace();
    //    }
    //    database.closeHikariConnection(connection);
    //}

    public static void saveall(String targettype, String type, List<UUID> players, BigDecimal amount, boolean isAdd,
                               String reason) {
        Connection connection = database.getConnectionAndCheck();
        try {
            if (targettype.equalsIgnoreCase("all")) {
                String query;
                if (isAdd) {
                    query = " set balance = balance + " + amount.doubleValue();
                } else {
                    query = " set balance = balance - " + amount.doubleValue();
                }
                PreparedStatement statement = connection.prepareStatement("update " + tableName + query);
                statement.executeUpdate();
                statement.close();
            } else if (targettype.equalsIgnoreCase("online")) {
                StringBuilder query;
                if (isAdd) {
                    query = new StringBuilder(" set balance = balance + " + amount + " where");
                } else {
                    query = new StringBuilder(" set balance = balance - " + amount + " where");
                }
                int jsm = players.size();
                int js = 1;

                for (UUID u : players) {
                    if (js == jsm) {
                        query.append(" UID = '").append(u.toString()).append("'");
                    } else {
                        query.append(" UID = '").append(u.toString()).append("' OR");
                        js = js + 1;
                    }
                }
                PreparedStatement statement = connection.prepareStatement("update " + tableName + query);
                statement.executeUpdate();
                statement.close();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        if (type != null) {
            record(connection, type, null, isAdd, amount, BigDecimal.ZERO, reason);
        }
        database.closeHikariConnection(connection);
    }

    public static void saveNonPlayer(String type, String account, BigDecimal amount,
                                     BigDecimal newbalance, boolean isAdd) {
        Connection connection = database.getConnectionAndCheck();
        try {
            String query = " set balance = ? where account = ?";
            PreparedStatement statement = connection.prepareStatement("update " + tableNonPlayerName + query);
            statement.setDouble(1, newbalance.doubleValue());
            statement.setString(2, account);
            statement.executeUpdate();
            statement.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
        record(connection, type, new PlayerData(null, account, null), isAdd, amount, newbalance, "N/A");
        database.closeHikariConnection(connection);
    }

    public static void getBaltop() {
        try {
            Connection connection = database.getConnectionAndCheck();
            PreparedStatement statement = connection.prepareStatement(
                    "select * from " + tableName + " where hidden != '1' order by balance desc limit " + ServerINFO.RankingSize);

            ResultSet rs = statement.executeQuery();
            while (rs.next()) {
                Cache.baltop.put(rs.getString(2), DataFormat.formatString(rs.getString(3)));
                Cache.baltop_papi.add(rs.getString(2));
            }

            rs.close();
            statement.close();
            database.closeHikariConnection(connection);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public static String sumBal() {
        String bal = "0.0";

        try {
            Connection connection = database.getConnectionAndCheck();
            PreparedStatement statement = connection.prepareStatement("select SUM(balance) from " + tableName + " where hidden != '1'");

            ResultSet rs = statement.executeQuery();
            if (rs.next()) {
                bal = rs.getString(1);
            }

            rs.close();
            statement.close();
            database.closeHikariConnection(connection);
        } catch (SQLException e) {
            e.printStackTrace();
        }

        return bal;
    }

    public static void hidetop(UUID u, int type) {
        Connection connection = database.getConnectionAndCheck();
        try {
            String query = " set hidden = ? where UID = ?";
            PreparedStatement statement = connection.prepareStatement("update " + tableName + query);
            statement.setInt(1, type);
            statement.setString(2, u.toString());
            statement.executeUpdate();
            statement.close();

        } catch (SQLException e) {
            e.printStackTrace();
        }
        database.closeHikariConnection(connection);
    }

    public static void record(Connection co, String type, PlayerData pd, Boolean isAdd,
                              BigDecimal amount, BigDecimal newbalance, String command) {
        if (DataBaseINFO.isMySQL() && XConomy.config.getNode("Settings", "transaction-record").getBoolean()) {
            String uid = "N/A";
            String name = "N/A";
            String operation;
            if (pd != null) {
                if (pd.getUniqueId() != null) {
                    uid = pd.getUniqueId().toString();
                }
                name = pd.getName();
            }
            if (isAdd != null) {
                if (isAdd) {
                    operation = "DEPOSIT";
                } else {
                    operation = "WITHDRAW";
                }
            } else {
                operation = "SET";
            }
            try {
                String query;
                query = "INSERT INTO " + tableRecordName + "(type,uid,player,balance,amount,operation,date,command) values(?,?,?,?,?,?,?,?)";
                PreparedStatement statement = co.prepareStatement(query);
                statement.setString(1, type);
                statement.setString(2, uid);
                statement.setString(3, name);
                statement.setDouble(4, newbalance.doubleValue());
                statement.setDouble(5, amount.doubleValue());
                statement.setString(6, operation);
                statement.setString(7, new Date().toString());
                statement.setString(8, command);
                statement.executeUpdate();
                statement.close();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }

}