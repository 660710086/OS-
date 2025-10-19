package SystemCall;

import java.io.*;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.channels.*;

public class FileServer {
    private int port;

    public FileServer(int port) {
        this.port = port;
    }

    public void startServer() throws IOException {
        try (ServerSocketChannel serverChannel = ServerSocketChannel.open()) {
            serverChannel.bind(new InetSocketAddress(port));
            System.out.println("Server listening on port " + port);

            while (true) {
                SocketChannel clientChannel = serverChannel.accept();
                System.out.println("Client connected: " + clientChannel.getRemoteAddress());
                handleClient(clientChannel);
            }
        }
    }

    private void handleClient(SocketChannel clientChannel) throws IOException {
        try {
            // ใช้ Socket สำหรับ stream
            Socket socket = clientChannel.socket();
            DataInputStream dis = new DataInputStream(socket.getInputStream());
            DataOutputStream dos = new DataOutputStream(socket.getOutputStream());

            String mode = dis.readUTF();
            String fileName = dis.readUTF();
            long fileSize = dis.readLong();
            System.out.println("Mode=" + mode + ", fileName=" + fileName + ", size=" + fileSize);

            if ("ZEROCOPY".equals(mode)) {
                receiveFileZeroCopy(clientChannel, fileName, fileSize);
            } else {
                receiveFileCopy(dis, fileName, fileSize);
            }

            dos.writeUTF("OK");
            clientChannel.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void receiveFileCopy(DataInputStream dis, String fileName, long fileSize) throws IOException {
        try (FileOutputStream fos = new FileOutputStream(fileName)) {
            byte[] buffer = new byte[8192];
            long remaining = fileSize;
            int read;
            while (remaining > 0 &&
                    (read = dis.read(buffer, 0, (int)Math.min(buffer.length, remaining))) != -1) {
                fos.write(buffer, 0, read);
                remaining -= read;
            }
        }
    }

    private void receiveFileZeroCopy(SocketChannel clientChannel, String fileName, long fileSize) throws IOException {
        try (FileChannel outChannel = new FileOutputStream(fileName).getChannel()) {
            long position = 0;
            while (position < fileSize) {
                long transferred = outChannel.transferFrom(clientChannel, position, fileSize - position);
                if (transferred <= 0) break;
                position += transferred;
            }
        }
    }

    public static void main(String[] args) throws IOException {
        int port = 9000;
        new FileServer(port).startServer();
    }
}
