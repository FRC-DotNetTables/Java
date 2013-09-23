package org.ingrahamrobotics.dotnettables.client;

import java.io.IOException;
import java.util.Iterator;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.ingrahamrobotics.dotnettables.DotNetTable;
import org.ingrahamrobotics.dotnettables.DotNetTable.DotNetTableEvents;
import org.ingrahamrobotics.dotnettables.DotNetTables;

public class Client implements DotNetTableEvents {

    public static void main(String[] args) {
        new Client().run();
    }

    public void run() {
        // Start NetworktTables
        try {
            DotNetTables.startClient("127.0.0.1");
        } catch (IOException ex) {
            Logger.getLogger(Client.class.getName()).log(Level.SEVERE, null, ex);
            System.exit(-1);
        }
        
        // Publish and subscribe a table
        DotNetTable client = DotNetTables.publish("FromClient");
        DotNetTable server = DotNetTables.subscribe("FromServer");
        
        // Register for updates from the subscribed table
        server.onChange(this);

        // Put new data into our published table every second
        int i = 0;
        while (true) {
            try {
                Thread.sleep(1000);
            } catch (InterruptedException ex) {
                Logger.getLogger(Client.class.getName()).log(Level.SEVERE, null, ex);
            }

            if (i % 10 == 0) {
                client.clear();
            }
            client.set("ClientKey-" + i, "ClientVal-" + i);
            client.send();
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
