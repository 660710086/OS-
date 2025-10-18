package BufferOrZeroCopy;

import java.io.*;
import java.nio.channels.*;
import java.net.*;



public class Server {
    private Socket socket;
    private DataInputStream fromServer;
    private DataOutputStream toServer;
    private SocketChannel socketChannel;
    private final String folder = "C:/Documents/os";
    private final String IPADDRESS = "192.168.56.1";
    private final int PORT = 3301;
    private final int PORTCHANNEL = 3302;
    private final File fil = new File(folder);

    public Server(){
        connectToServer();
    }

    public final void connectToServer(){
        try {
            socket = new Socket(IPADDRESS, PORT);
            fromServer = new DataInputStream(socket.getInputStream());
            toServer = new DataOutputStream(socket.getOutputStream());
            socketChannel = SocketChannel.open(new InetSocketAddress(IPADDRESS, PORTCHANNEL));
        } catch (IOException e) {
            System.out.println("Error in connect");
        }
    }
    public void copy(String filePath, long size) throws IOException {
        try (var fos = new BufferedOutputStream(new FileOutputStream(filePath))) {
            byte[] buf = new byte[64 * 1024];
            long got = 0;
            while (got < size) {
                int toRead = (int)Math.min(buf.length, size - got);
                int n = fromServer.read(buf, 0, toRead);
                if (n == -1) throw new EOFException("early EOF at " + got + "/" + size);
                fos.write(buf, 0, n);
                got += n;
            }
            System.out.println("copy file : " + fil.getName() + " success froom Disk");

        } catch (IOException e) {
            System.out.println("Error in copy");
        }

    }



    public final void zeroCopy(String filePath, long size) throws IOException {
        try (var dest = new FileOutputStream(filePath).getChannel()) {
            long pos = 0;
            while (pos < size) {
                long n = dest.transferFrom(socketChannel, pos, size - pos);
                if (n > 0) { pos += n; continue; }
                if (!socketChannel.isBlocking()) { Thread.onSpinWait(); continue; }
                throw new EOFException("transferFrom stalled at " + pos + "/" + size);
            }
            dest.force(true);
        }
    }

    class ClientHandle implements Runnable {

        private final int clientNo;
        private final Socket socket;
        private final DataInputStream fromClient;
        private final DataOutputStream toClient;
        private final SocketChannel socketChannel;


        public ClientHandle(Socket socket, DataInputStream fromClient, DataOutputStream toClient,
                            SocketChannel socketChannel, int clientNo, File[] fileList) {
            this.socket = socket;
            this.fromClient = fromClient;
            this.toClient = toClient;
            this.socketChannel = socketChannel;
            this.clientNo = clientNo;

        }



        @Override
        public void run() {

            try {
                while (true) {
                    int index = fromClient.readInt();
                    String type = fromClient.readUTF();
                    String filePath = fil.getAbsolutePath();
                    long size = fil.length();
                    toClient.writeLong(size);
                    System.out.println("Client " + clientNo + " request "+(!type.equals("1") ? "zero " : "")+"copy file : " + fil.getName());
                    if(type.equals("1"))
                        copy(filePath, size);
                    else
                        zeroCopy(filePath, size);
                }
            } catch (IOException ex) {
                System.out.println("Client " + clientNo + " disconnected");
            }
        }

    }




}