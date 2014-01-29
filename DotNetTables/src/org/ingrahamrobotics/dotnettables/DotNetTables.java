package org.ingrahamrobotics.dotnettables;

import edu.wpi.first.wpilibj.networktables.NetworkTable;
import java.io.IOException;
// I'm aware this is obsolete, but it's also compatible with the cRIO's squawk JVM
import java.util.Hashtable;
import java.util.Enumeration;

/**
 * A wrapper for FRC NetworkTables that provides enforced directionality, a
 * unified view of subscribed and published tables, and periodic updates and
 * staleness detection
 *
 * This project also exposes all of the underlying NetworkTables classes,
 * methods, and data types, for easy inclusion in both Java and .NET projects
 *
 * @author FRC Team 4030
 */
public class DotNetTables {

    /**
     * The table name used for the underlying NetworkTable
     */
    public static final String TABLE_NAME = "DotNet";
    private static NetworkTable nt_table;
    private static boolean client = false;
    private static boolean connected = false;
    private static Hashtable tables;
    private static final Object syncLock = new Object();

    static private void init() throws IOException {
        synchronized (syncLock) {
            tables = new Hashtable();

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

    /**
     * Initialize a NetworkTables server. In standard FRC usage this is done on
     * the robot. A server can both publish and subscribe to tables; server vs.
     * client mode only controls whether the process listens for inbound network
     * connections.
     *
     * @throws IOException Thrown if the underlying network bind() operations
     * fail
     */
    static public void startServer() throws IOException {
        init();
    }

    /**
     * Initialize a NetworkTables client. In standard FRC usage this is done on
     * the driver's station or other remote computers. A client and both publish
     * and subscribe to tables; server vs. client mode only controls whether the
     * process listens for inbound network connections.
     *
     * @param IP The IP address of the NetworkTables server. If using standard
     * FRC IP addresses, you may provide your team number instead of the IP
     * address.
     * @throws IOException Thrown if the underlying network bind() operations
     * fail
     */
    static public void startClient(String IP) throws IOException {
        NetworkTable.setClientMode();

        boolean ipSet = false;

        // If the input parses as a interger, assume it's a team number
        try {
            NetworkTable.setTeam(Integer.parseInt(IP));
            ipSet = true;
        } catch (NumberFormatException ex) {
        }

        // This is not a complete check for a vald IP address, but we don't have regex and it's not worth much work
        if (!ipSet) {
            int i = IP.indexOf('.');
            if (i > 0 && i < 4 && IP.length() >= 7) {
                NetworkTable.setIPAddress(IP);
                ipSet = true;
            }
        }

        if (!ipSet) {
            throw new IllegalArgumentException("Invalid IP address or team number: " + IP);
        }

        client = true;
        init();
    }

    /**
     * @return True if this device is configured as a NetworkTables client,
     * false if configured as a server
     */
    public static boolean isClient() {
        return client;
    }

    /**
     * @return True if the NetworkTables connection has been successfully
     * initialized
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
        for (Enumeration it = tables.keys(); it.hasMoreElements();) {
            String tableName = (String) it.nextElement();
            if (tableName.equals(name)) {
                return (DotNetTable) tables.get(tableName);
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
                tables.put(table.name(), table);

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
