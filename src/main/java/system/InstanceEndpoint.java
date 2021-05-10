package system;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;

public class InstanceEndpoint {
    public static void main(String[] args) throws IOException {
        ServerSocket ss = new ServerSocket(8080);
        System.out.println("Listening on port 8080");
        while (true) {
            Socket socket = ss.accept();
            if (socket.isConnected()) {
                try (DataInputStream in = new DataInputStream(socket.getInputStream());
                     DataOutputStream out = new DataOutputStream(socket.getOutputStream())) {
                    out.writeUTF("{\"type\":\"trade\",\"trade\":{\"profit\":0.1,\"profitable\":true,\"name\":\"big cocka\"}}");
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
