package org.ingrahamrobotics.dotnettables;

import edu.wpi.first.wpilibj.networktables2.type.StringArray;
import edu.wpi.first.wpilibj.tables.ITable;
import edu.wpi.first.wpilibj.tables.ITableListener;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Set;

public class DotNetTable implements ITableListener {

    public static final double STALE_FACTOR = 2.5;
    public static final String UPDATE_INTERVAL = "_UPDATE_INTERVAL";
    private String name;
    private int updateInterval;
    private boolean writable;
    public HashMap<String, String> data;
    private DotNetTableEvents changeCallback;
    private DotNetTableEvents staleCallback;
    private long lastUpdate;

    public DotNetTable(String name, boolean writable) {
        this.lastUpdate = 0;
        this.name = name;
        this.writable = writable;
        this.updateInterval = -1;
        this.changeCallback = null;
        this.staleCallback = null;
        data = new HashMap<String, String>();
    }

    public String name() {
        return this.name;
    }

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

    public double lastUpdate() {
        return this.lastUpdate;
    }

    public boolean isWritable() {
        return this.writable;
    }

    private void throwIfNotWritable() throws IllegalStateException {
        if (!this.writable) {
            throw new IllegalStateException("Table is read-only: " + this.name);
        }
    }

    public int getInterval() {
        return this.updateInterval;
    }

    public void setInterval(int update) throws IllegalStateException {
        this.throwIfNotWritable();
        if (update <= 0) {
            update = -1;
        }
        this.updateInterval = update;
    }

    public void onChange(DotNetTableEvents callback) {
        this.changeCallback = callback;
    }

    public void onStale(DotNetTableEvents callback) {
        if (this.writable) {
            throw new IllegalStateException("Table is local: " + this.name);
        }
        this.staleCallback = callback;
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public void clear() {
        data.clear();
    }

    public Set<String> keys() {
        return data.keySet();
    }

    public boolean exists(String key) {
        return data.containsKey(key);
    }

    public void set(String key, String value) throws IllegalStateException {
        this.throwIfNotWritable();
        data.put(key, value);
        this.lastUpdate = System.currentTimeMillis();
    }

    public void set(String key, double value) throws IllegalStateException {
        this.set(key, Double.toString(value));
    }

    public void set(String key, int value) throws IllegalStateException {
        this.set(key, Integer.toString(value));
    }

    public void remove(String key) throws IllegalStateException {
        this.throwIfNotWritable();
        data.remove(key);
    }

    public String get(String key) {
        return data.get(key);
    }

    public double getDouble(String key) {
        return Double.valueOf(data.get(key));
    }

    public int getInt(String key) {
        return Integer.valueOf(data.get(key));
    }

    private void recv(StringArray value) {
        // Unpack the new data
        data = SAtoHM(value);
        this.lastUpdate = System.currentTimeMillis();

        // Note the published update interval
        if (this.exists(UPDATE_INTERVAL)) {
            this.updateInterval = this.getInt(UPDATE_INTERVAL);
        }

        // Dispatch our callback, if any
        if (changeCallback != null) {
            changeCallback.changed(this);
        }
    }

    public void send() throws IllegalStateException {
        throwIfNotWritable();
        set(UPDATE_INTERVAL, getInterval());
        DotNetTables.push(name, HMtoSA(data));

        // Dispatch our callback, if any
        if (changeCallback != null) {
            changeCallback.changed(this);
        }
    }

    private StringArray HMtoSA(HashMap<String, String> data) {
        StringArray out = new StringArray();
        for (Iterator<String> it = data.keySet().iterator(); it.hasNext();) {
            String key = it.next();
            out.add(key);
        }
        
        // Use the output list of keys as the iterator to ensure correct value ordering
        int size = out.size();
        for (int i = 0; i < size; i++) {
            out.add(data.get(out.get(i)));
        }
        return out;
    }

    private HashMap<String, String> SAtoHM(StringArray data) throws ArrayIndexOutOfBoundsException {
        HashMap<String, String> out = new HashMap<String, String>();
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
     * @param value The new or updated array
     * @param isNew True if the array did not previous exist
     */
    @Override
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

    public interface DotNetTableEvents {

        public void changed(DotNetTable table);

        public void stale(DotNetTable table);
    }
}
