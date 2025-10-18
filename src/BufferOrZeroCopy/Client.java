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
            socketChannel = SocketChannel.open(new InetSocketAddress(IPADDRESS, PORTCHANNEL));
        } catch (IOException e) {
            System.out.println("Error in connect");
        }
    }

    public final void requestServer() throws IOException {
        System.out.println("Enter file name to send: ");
        Scanner sc = new Scanner(System.in);
        String request = sc.next();
        System.out.println("File name: " + sc.nextLine());
        System.out.println("Select type");
        System.out.println("1.copy | 2.zero copy");
        System.out.println("Type: ");
        String type = sc.next();
        sc.nextLine();
        if (!type.equals("1")&&!type.equals("2")){
            System.out.println("Invalid type selected");
        }
        toServer.writeBytes(request);
        toServer.writeUTF(type);
        System.out.println("File request sent");
        long size = fromServer.readLong();
        long start = System.currentTimeMillis();
        if (type.equals("1")){
            copy(request,size);
        } else if (type.equals("2")) {
            zeroCopy(request,size);

        }
        long end = System.currentTimeMillis();

    }

    public void copy(String filePath , long size){
        try (var in = new BufferedInputStream(new FileInputStream(filePath))) {
            byte[] buf = new byte[64 * 1024];
            long sent = 0;
            while (sent < size) {
                int toRead = (int)Math.min(buf.length, size - sent);
                int n = in.read(buf, 0, toRead);
                if (n == -1) throw new EOFException("file shorter than size at " + sent + "/" + size);
                toServer.write(buf, 0, n);
                sent += n;
            }
            toServer.flush(); // เผื่อมีบัฟเฟอร์ฝั่งผู้ส่ง
        } catch (IOException e) {
            System.out.println("Error in copy");
        }


    }

    public void zeroCopy(String filePath, long size) throws IOException {
        try (var src = new FileInputStream(filePath).getChannel()) {
            long fileSize = src.size();
            long target   = Math.min(size, fileSize); // กันประกาศผิด
            long pos = 0;
            while (pos < target) {
                long n = src.transferTo(pos, target - pos, socketChannel);
                if (n > 0) { pos += n; continue; }
                if (!socketChannel.isBlocking()) { Thread.onSpinWait(); continue; }
                throw new EOFException("transferTo stalled at " + pos + "/" + target);
            }
            socketChannel.socket().shutdownOutput();
        }
    }


}