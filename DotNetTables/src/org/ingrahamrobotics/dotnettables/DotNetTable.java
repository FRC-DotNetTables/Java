package org.ingrahamrobotics.dotnettables;

import edu.wpi.first.wpilibj.networktables2.type.StringArray;
import edu.wpi.first.wpilibj.tables.ITable;
import edu.wpi.first.wpilibj.tables.ITableListener;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Timer;
import java.util.TimerTask;

/**
 * A named and published or subscribed DotNetTable. DotNetTables provide
 * network-synchronized key-value pairs. If published, DotNetTables are
 * read-write and available for subscription by other NetworkTables endpoints.
 * If subscribed DotNetTables are read-only. This limitation is intentional, to
 * avoid usage conflicts where more than one endpoint attempts to update the
 * same key-value pair. If you wish to use bi-directional tables, use the
 * underlying NetworkTables functionality rather than the DotNetTables
 * abstraction.
 *
 * @author FRC Team 4030
 */
@SuppressWarnings("UseOfObsoleteCollectionType")
public class DotNetTable implements ITableListener {

    /**
     * The scaling fudge-factor applied when calculating staleness -- actual
     * update intervals may exceed the specified interval by this factor before
     * being declared "stale".
     */
    public static final double STALE_FACTOR = 2.1;
    /**
     * The reserved key name used to publish the update interval to subscribers.
     */
    public static final String UPDATE_INTERVAL = "_UPDATE_INTERVAL";
    private String name;
    private int updateInterval;
    private boolean writable;
    private Timer timer;
    /**
     * The underlying local data store for this table. This is converted to a
     * StringArray when published to the network (or from a StringArray when
     * received from the network)
     */
    public Hashtable data;
    private DotNetTableEvents changeCallback;
    private DotNetTableEvents staleCallback;
    private long lastUpdate;

    /**
     * Create a new DotNetTable with the specified name and ro/rw designation.
     *
     * @param name User-provided table name
     * @param writable True if the table is published, false if it's subscribed
     */
    protected DotNetTable(String name, boolean writable) {
        this.lastUpdate = 0;
        this.name = name;
        this.writable = writable;
        this.updateInterval = -1;
        this.changeCallback = null;
        this.staleCallback = null;
        this.data = new Hashtable();
        this.timer = null;
    }

    /**
     * @return The user-provided name for this table
     */
    public String name() {
        return this.name;
    }

    /**
     * @return True if this table has not been updated within the last update
     * interval
     */
    public boolean isStale() {
        // Tables with no update interval are never stale
        if (this.updateInterval <= 0) {
            return false;
        }

        // Tables are stale when we miss STALE_FACTOR update intervals
        double age = System.currentTimeMillis() - this.lastUpdate;
        if (age > (this.updateInterval * STALE_FACTOR)) {
            return true;
        }

        // Otherwise we're fresh
        return false;
    }

    /**
     * @return Timestamp of the last update to this table
     */
    public double lastUpdate() {
        return this.lastUpdate;
    }

    /**
     * @return True if the table is writable (i.e. published). False if the
     * table is subscribed.
     */
    public boolean isWritable() {
        return this.writable;
    }

    private void throwIfNotWritable() throws IllegalStateException {
        if (!this.writable) {
            throw new IllegalStateException("Table is read-only: " + this.name);
        }
    }

    private class DotNetTableTimer extends TimerTask {

        private DotNetTable table;

        public DotNetTableTimer(DotNetTable table) {
            this.table = table;
        }

        // In newer java we would annote with @Override, but not for the cRIO
        public void run() {
            if (table.isWritable()) {
                table.send();
            } else {
                if (table.staleCallback != null) {
                    table.staleCallback.stale(table);
                }
            }
        }
    }

    private void resetTimer() {
        if (this.timer != null) {
            this.timer.cancel();
        }
        if (this.updateInterval >= 0) {
            this.timer = new Timer();
            TimerTask timerTask = new DotNetTable.DotNetTableTimer(this);
            long delay = this.updateInterval;
            if (!this.isWritable()) {
                delay *= STALE_FACTOR;
            }
            this.timer.schedule(timerTask, delay);
        }
    }

    /**
     * @return The expected update interval for this table, in seconds
     */
    public int getInterval() {
        return this.updateInterval;
    }

    /**
     * Set the expected update interval for this table. This interval controls
     * what is considered "stale" by any subscribers, and sets the maximum time
     * between network sends() in milliseconds. Only published tables may set
     * their update interval; subscribers are informed of the interval and
     * calculate their "stale" indicator based on expectations set by the
     * publisher.
     *
     * @param update The desired update interval, in seconds
     * @throws IllegalStateException Thrown if this table is not writable (i.e.
     * is subscribed rather than published)
     */
    public void setInterval(int update) throws IllegalStateException {
        this.throwIfNotWritable();
        if (update <= 0) {
            update = -1;
        }
        this.updateInterval = update;
        this.resetTimer();
    }

    /**
     * Sets the callback method to be dispatched when data in this table is
     * updated.
     *
     * @param callback The method to be dispatched. Must implement
     * DotNetTableEvents.
     */
    public void onChange(DotNetTableEvents callback) {
        this.changeCallback = callback;
    }

    /**
     * Sets the callback method to be dispatched when the data in this table
     * goes "stale". This occurs when the period since the last update exceeds
     * the publisher-provided update interval.
     *
     * @param callback The method to be dispatched. Must implement
     * DotNetTableEvents.
     */
    public void onStale(DotNetTableEvents callback) {
        if (this.writable) {
            throw new IllegalStateException("Table is local: " + this.name);
        }
        this.staleCallback = callback;
    }

    /**
     * Clear all data from this table
     */
    public void clear() {
        data.clear();
    }

    /**
     * @return A set of all keys in this table
     */
    public Enumeration keys() {
        return data.keys();
    }

    /**
     * @param key The key in question
     * @return True if the key exists in the table, otherwise false
     */
    public boolean exists(String key) {
        return data.containsKey(key);
    }

    /**
     * Add or replace the specified key-value pair.
     *
     * @param key The key to be added or replaced
     * @param value The value to be added or replaced
     * @throws IllegalStateException Thrown if the table is not writable (i.e.
     * is subscribed)
     */
    public void setValue(String key, String value) throws IllegalStateException {
        this.throwIfNotWritable();
        data.put(key, value);
        this.lastUpdate = System.currentTimeMillis();
    }

    /**
     * Add or replace the specified key-value pair.
     *
     * @param key The key to be added or replaced
     * @param value The value to be added or replaced
     * @throws IllegalStateException Thrown if the table is not writable (i.e.
     * is subscribed)
     */
    public void setValue(String key, double value) throws IllegalStateException {
        this.setValue(key, Double.toString(value));
    }

    /**
     * Add or replace the specified key-value pair.
     *
     * @param key The key to be added or replaced
     * @param value The value to be added or replaced
     * @throws IllegalStateException Thrown if the table is not writable (i.e.
     * is subscribed)
     */
    public void setValue(String key, int value) throws IllegalStateException {
        this.setValue(key, Integer.toString(value));
    }

    /**
     * Remove the specified key (and its related value) from this table
     *
     * @param key The key to be removed. No error is thrown if the key does not
     * exist.
     * @throws IllegalStateException Thrown if the table is not writable (i.e.
     * is subscribed)
     */
    public void remove(String key) throws IllegalStateException {
        this.throwIfNotWritable();
        data.remove(key);
    }

    /**
     * @param key The key of the value to be retrieved from the table
     * @return The related value
     */
    public String getValue(String key) {
        return (String) data.get(key);
    }

    /**
     * @param key The key of the value to be retrieved from the table
     * @return The related value
     */
    public double getDouble(String key) {
        return Double.parseDouble(getValue(key));
    }

    /**
     * @param key The key of the value to be retrieved from the table
     * @return The related value
     */
    public int getInt(String key) {
        return Integer.parseInt(getValue(key));
    }

    private void recv(StringArray value) {
        // Unpack the new data
        data = SAtoHM(value);
        this.lastUpdate = System.currentTimeMillis();

        // Note the published update interval
        if (this.exists(UPDATE_INTERVAL)) {
            this.updateInterval = this.getInt(UPDATE_INTERVAL);
            data.remove(UPDATE_INTERVAL);
            this.resetTimer();
        }

        // Dispatch our callback, if any
        if (changeCallback != null) {
            changeCallback.changed(this);
        }
    }

    /**
     * Publish this table to all subscribers.
     *
     * @throws IllegalStateException Thrown if the table is not writable (i.e.
     * is subscribed)
     */
    public void send() throws IllegalStateException {
        throwIfNotWritable();
        setValue(UPDATE_INTERVAL, getInterval());
        if (exists("_bump")) {
            remove("_bump");
        } else {
            setValue("_bump", System.currentTimeMillis());
        }
        DotNetTables.push(name, HMtoSA(data));
        this.resetTimer();

        // Dispatch our callback, if any
        if (changeCallback != null) {
            changeCallback.changed(this);
        }
    }

    private StringArray HMtoSA(Hashtable data) {
        StringArray out = new StringArray();
        for (Enumeration it = data.keys(); it.hasMoreElements();) {
            String key = (String) it.nextElement();
            out.add(key);
        }

        /*Use the output list of keys as the iterator to ensure correct value ordering*/
        int size = out.size();
        for (int i = 0; i < size; i++) {
            out.add((String) data.get(out.get(i)));
        }
        return out;
    }

    private Hashtable SAtoHM(StringArray data) throws ArrayIndexOutOfBoundsException {
        Hashtable out;
        out = new Hashtable();
        if (data.size() % 2 != 0) {
            throw new ArrayIndexOutOfBoundsException("StringArray contains an odd number of elements");
        }
        int setSize = data.size() / 2;
        for (int i = 0; i < setSize; i++) {
            out.put(data.get(i), data.get(i + setSize));
        }
        return out;
    }

    /**
     * Update with new data from a remote subscribed table
     *
     * @param itable The underlying NetworkTable table
     * @param key The array name -- must match our name to trigger an update
     * @param val The new or updated array
     * @param isNew True if the array did not previous exist
     */
    // In newer java we would annote with @Override, but not for the cRIO
    public void valueChanged(ITable itable, String key, Object val, boolean isNew) {
        // Skip updates for other tables
        if (!this.name.equals(key)) {
            return;
        }

        // Store the new data
        StringArray value = new StringArray();
        itable.retrieveValue(key, value);
        recv(value);
    }

    /**
     * The interface necessary to provide callback handling of "change" and
     * "stale" events
     */
    public interface DotNetTableEvents {

        /**
         * The method to be dispatched when data in the table is updated.
         *
         * @param table The table containing updated data.
         */
        public void changed(DotNetTable table);

        /**
         * The method to be dispatched when a table goes "stale". (i.e. exceeds
         * the update interval without an update)
         *
         * @param table The table that has gone "stale".
         */
        public void stale(DotNetTable table);
    }
}
