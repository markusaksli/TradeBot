package system;

import data.config.Config;
import data.config.ConfigException;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;

public class InstanceEndpoint {
    public static void main(String[] args) throws IOException {
        Config config = null;
        try {
            config = new Config("configs/full.yaml");
        } catch (ConfigException e) {
            e.printStackTrace();
        }

        ServerSocket ss = new ServerSocket(8080);
        System.out.println("Listening on port 8080");
        while (true) {
            Socket socket = ss.accept();
            if (socket.isConnected()) {
                try (DataInputStream in = new DataInputStream(socket.getInputStream());
                     DataOutputStream out = new DataOutputStream(socket.getOutputStream())) {
                    out.writeUTF(config.toJson());
                    while (true) {
                        System.out.println(in.readUTF());
                    }
                } catch (IOException e) {
                    System.out.println("connection aborted with client " + socket.toString());
                    ;
                }
            }
        }
    }
}
