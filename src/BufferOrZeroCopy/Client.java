package BufferOrZeroCopy;

import java.io.*;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.channels.FileChannel;
import java.nio.channels.SocketChannel;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Scanner;

class Client {
    private Socket socket;
    private DataInputStream fromServer;
    private DataOutputStream toServer;
    private SocketChannel socketChannel;
    private final String folder = "C:/Documents/os";
    private final String IPADDRESS = "192.168.56.1";
    private final int PORT = 3301;
    private final int PORTCHANNEL = 3302;

    public Client(){
        connection();
    }
    public final void connection() {
        try {
            socket = new Socket(IPADDRESS, PORT);
            fromServer = new DataInputStream(socket.getInputStream());
            toServer = new DataOutputStream(socket.getOutputStream());
            socketChannel = SocketChannel.open(new InetSocketAddress(IpAddress, portChannel));
        } catch (IOException e) {
            System.out.println("Error in connect");
        }
    }



}