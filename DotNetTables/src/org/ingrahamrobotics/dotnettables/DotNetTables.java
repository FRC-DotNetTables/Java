package org.ingrahamrobotics.dotnettables;

import edu.wpi.first.wpilibj.networktables.NetworkTable;
import java.io.IOException;
import java.util.ArrayList;

public class DotNetTables {

    // The table name used for the underlying NetworkTable
    public static final String TABLE_NAME = "DotNet";
    private static NetworkTable nt_table;
    private static boolean client = false;
    private static boolean connected = false;
    private static ArrayList<DotNetTable> tables;
    private static final Object syncLock = null;

    static private void init() throws IOException {
        synchronized (syncLock) {
            tables = new ArrayList<DotNetTable>();

            // Attempt to init the underlying NetworkTable
            try {
                NetworkTable.initialize();
                nt_table = NetworkTable.getTable(TABLE_NAME);
                connected = true;
            } catch (IOException ex) {
                System.err.println("Unable to initialize NetworkTable: " + TABLE_NAME);
                throw ex;
            }
        }
    }

    static public void startServer() throws IOException {
        init();
    }

    static public void startClient(String IP) throws IOException {
        NetworkTable.setClientMode();
        if (IP.matches("^\\d{4}$")) {
            NetworkTable.setTeam(Integer.valueOf(IP));
        } else if (IP.matches("^(?:\\d{1,3}.){3}\\d{1,3}$")) {
            NetworkTable.setIPAddress(IP);
        } else {
            throw new IllegalArgumentException("Invalid IP address or team number: " + IP);
        }

        client = true;
        init();
    }

    /**
     * @return True if this device is configured as a NetworkTable subscriber
     */
    public static boolean isClient() {
        return client;
    }

    /**
     * @return True if the connection has been successfully initialized
     */
    public static boolean isConnected() {
        return connected;
    }

    /**
     * Find the specified table in the subscribed tables list, if it exists
     *
     * @param name The table to be found
     * @return The specified table, if available. NULL if no such table exists.
     */
    private static DotNetTable findTable(String name) throws IllegalArgumentException {
        for (DotNetTable table : tables) {
            if (table.name().equals(name)) {
                return table;
            }
        }
        throw new IllegalArgumentException("No such table: " + name);
    }

    /**
     * Subscribe to a table published by a remote host. Works in both server and
     * client modes.
     *
     * @param name Name of the table to subscribe
     * @return The subscribed table
     */
    public static DotNetTable subscribe(String name) {
        return getTable(name, false);
    }

    /**
     * Publish a table for remote hosts. Works in both server or client modes.
     *
     * @param name Name of the table to publish
     * @return The published table
     */
    public static DotNetTable publish(String name) {
        return getTable(name, true);
    }

    /**
     * Get a table, creating and subscribing/publishing as necessary
     *
     * @param name New or existing table name
     * @return The table to get/create
     */
    private static DotNetTable getTable(String name, boolean writable) {
        synchronized (syncLock) {
            DotNetTable table;
            try {
                table = findTable(name);
            } catch (IllegalArgumentException ex) {
                table = new DotNetTable(name, writable);
                tables.add(table);

                // Publish or subscribe the new table
                if (writable) {
                    table.send();
                } else {
                    nt_table.addTableListener(table);
                }
            }

            // Ensure the table has the specified writable state
            if (table.isWritable() != writable) {
                throw new IllegalStateException("Table already exists but does not share writable state: " + name);
            }
            return table;
        }
    }

    /**
     * Removes a table from the subscription/publish list
     *
     * @param name The table to remove
     */
    public static void drop(String name) {
        synchronized (syncLock) {
            try {
                DotNetTable table = findTable(name);
                nt_table.removeTableListener(table);
                tables.remove(table);
            } catch (IllegalArgumentException ex) {
                // Ignore invalid drop requests
            }
        }
    }

    /**
     * Push the provided object into the NetworkTable
     *
     * @param name DotNetTable name
     * @param data StringArray-packed DotNetTable data
     */
    protected static void push(String name, Object data) {
        synchronized (syncLock) {
            if (!isConnected()) {
                throw new IllegalStateException("NetworkTable not initalized");
            }
            DotNetTable table;
            try {
                table = findTable(name);
            } catch (IllegalArgumentException ex) {
                throw new IllegalStateException(ex.toString());
            }
            if (!table.isWritable()) {
                throw new IllegalStateException("Table not writable: " + name);
            }
            nt_table.putValue(name, data);
        }
    }
}
