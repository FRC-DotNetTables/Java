package org.ingrahamrobotics.dotnettables.server;

import java.io.IOException;
import java.util.Enumeration;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.ingrahamrobotics.dotnettables.DotNetTable;
import org.ingrahamrobotics.dotnettables.DotNetTable.DotNetTableEvents;
import org.ingrahamrobotics.dotnettables.DotNetTables;

public class Server implements DotNetTableEvents {

    public static void main(String[] args) {
        new Server().run();
    }

    public void run() {
        // Start NetworkTables
        try {
            DotNetTables.startServer();
        } catch (IOException ex) {
            Logger.getLogger(Server.class.getName()).log(Level.SEVERE, null, ex);
            System.exit(-1);
        }
        
        // Publish and subscribe to a table
        DotNetTable server = DotNetTables.publish("FromServer");
        DotNetTable client = DotNetTables.subscribe("FromClient");
        
        // Register for updates from the subscribed table
        client.onChange(this);
        client.onStale(this);
        
        // Set an update intervale for our published table
        server.setInterval(5000);

        // Put new data into our published table every second
        int i = 0;
        while (true) {
            try {
                Thread.sleep(1000);
            } catch (InterruptedException ex) {
                Logger.getLogger(Server.class.getName()).log(Level.SEVERE, null, ex);
            }

            if (i % 10 == 0) {
                server.clear();
            }
            server.setValue("ServerKey-" + i, "ServerVal-" + i);
            i++;
        }
    }

    @Override
    public void changed(DotNetTable table) {
        String key;
        for (Enumeration it = table.keys(); it.hasMoreElements();) {
            key = (String) it.nextElement();
            System.out.println(key + " => " + table.getValue(key));
        }
        System.out.println();
    }

    @Override
    public void stale(DotNetTable table) {
        System.out.print("\nTable expired: " + table.name() + "\n\n");
    }
}
