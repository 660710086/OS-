package BufferOrZeroCopy;

import java.io.*;
import java.nio.channels.*;
import java.net.*;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Scanner;

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

        try(FileOutputStream fos = new FileOutputStream(filePath)) {

            byte[] buffer = new byte[1024];
            int read;
            long currentRead = 0;

            while (currentRead < size && (read = fromServer.read(buffer))!= -1){
                fos.write(buffer,0,read);
                //เมธอด write(byte[] b, int off, int len) ของ OutputStream หมายถึง
                //“เขียนข้อมูลจากอาร์เรย์ b เริ่มที่ตำแหน่ง off ต่อเนื่องยาว len ไบต์”

                currentRead += read;
            }

        }catch (IOException e){
            System.out.println("Error in copy");

        }finally {
            disconnect();
        }
    }

    public void zeroCopy(String filePath, long announcedSize) throws IOException {
        try (FileChannel src = FileChannel.open(Path.of(filePath), StandardOpenOption.READ)) {
            //ได้ FileChannel ของไฟล์ที่จะส่ง
            long fileSize   = src.size();
            // ขนาดไฟล์จริง
            long targetSize = Math.min(announcedSize, fileSize);  // กันพลาด

            long pos = 0;
            while (pos < targetSize) {
                long n = src.transferTo(pos, targetSize - pos, socketChannel);
                //transferTo(position, count, target) ให้ OS โยนข้อมูลจาก page cache ของไฟล์ไปยัง socket โดยตรง (Linux → sendfile)
                //อาจส่งได้ “ไม่ครบ” ในครั้งเดียว ⇒ จึงต้อง ลูป และสะสมตำแหน่ง pos
                if (n > 0) {
                    pos += n;
                    continue;
                }
                // n == 0  => ชะงัก: จัดการตามโหมดของ socketChannel
                if (!socketChannel.isBlocking()) {
                    // ตัวเลือก: ใช้ Selector รอ OP_WRITE, หรือพักสั้นๆ
                    Thread.onSpinWait(); // หรือ Thread.sleep(1);
                    continue;
                }
                // ถ้าเป็น blocking แล้ว n==0 แบบผิดปกติ
                throw new EOFException("transferTo stalled at " + pos + " of " + targetSize);
                //ใน non-blocking โอกาสสูงที่ transferTo(...) จะคืน 0 (หมายถึง “ยังเขียนไม่ได้ตอนนี้”) ⇒ จึงต้อง ลูป และอาจใช้ Selector รอ OP_WRITE ก่อนลองใหม่
                //ใน blocking โค้ดเรียบกว่า แต่ว่ายังคงต้องลูปเพราะระบบปฏิบัติการอาจส่งได้ไม่ครบในครั้งเดียว
            }
        }finally {
            socketChannel.socket().shutdownOutput();
            disconnect();

        }
        // อย่าลืมปิด/flush ฝั่งเขียนของ socket ตามโปรโตคอลถ้าจบไฟล์แล้ว:

    }

    public void disconnect(){
        try{
            if(fromServer != null)
                fromServer.close();
            if(toServer != null)
                toServer.close();
            if(socket != null)
                socket.close();
            if(socketChannel != null)
                socketChannel.close();
        } catch (IOException e){
            System.out.println("Error in disconnect");
        }
    }

    class ClientHandle implements Runnable {

        private final int clientNo;
        private final Socket socket;
        private final DataInputStream fromClient;
        private final DataOutputStream toClient;
        private final SocketChannel socketChannel;

        public ClientHandle(int clientNo, Socket socket) throws IOException {
            this.clientNo = clientNo;
            this.socket = socket;
            this.fromClient = new DataInputStream(socket.getInputStream());
            this.toClient = new DataOutputStream(socket.getOutputStream());
            this.socketChannel = SocketChannel.open(new InetSocketAddress(IPADDRESS, PORTCHANNEL));

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
                disconnect();
            }
        }
    }

}