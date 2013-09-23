package org.ingrahamrobotics.dotnettables.server;

import java.io.IOException;
import java.util.Iterator;
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
            server.set("ServerKey-" + i, "ServerVal-" + i);
            server.send();
            i++;
        }
    }

    @Override
    public void changed(DotNetTable table) {
        for (Iterator<String> it = table.keys().iterator(); it.hasNext();) {
            String key = it.next();
            System.out.println(key + " => " + table.get(key));
        }
        System.out.println();
    }

    @Override
    public void stale(DotNetTable table) {
        throw new UnsupportedOperationException("Not supported yet.");
    }
}
