package SystemCall;

import java.io.*;
import java.net.*;
import java.nio.channels.*;
import java.util.Scanner;

public class FileClient {
    String host;
    int port;

    public FileClient(String host, int port) {
        this.host = host;
        this.port = port;
    }

    public void sendFile(String localFilePath, boolean zeroCopy) throws IOException {
        File file = new File(localFilePath);
        String mode = zeroCopy ? "ZEROCOPY" : "COPY";

        try (SocketChannel sc = SocketChannel.open(new InetSocketAddress(host, port))) {
            Socket socket = sc.socket();

            DataOutputStream dos = new DataOutputStream(socket.getOutputStream());
            DataInputStream dis = new DataInputStream(socket.getInputStream());

            dos.writeUTF(mode);
            dos.writeUTF(file.getName());
            dos.writeLong(file.length());
            long startNs = System.nanoTime();
            if (zeroCopy) {
                sendFileZeroCopy(sc, file);
                long elapsedNs = System.nanoTime() - startNs;
                System.out.println("TYPE : ZEROCOPY ");
                System.out.printf("| total: %.3f ms%n", elapsedNs / 1_000_000.0);
            } else {
                sendFileCopy(dos, file);
                long elapsedNs = System.nanoTime() - startNs;
                System.out.println("TYPE : COPY ");
                System.out.printf("| total: %.3f ms%n", elapsedNs / 1_000_000.0);
            }

            String response = dis.readUTF();
            System.out.println("Server response: " + response);
            System.out.println("โหลดเสร็จแล้วจร้า");
        }
    }

    private void sendFileCopy(DataOutputStream dos, File file) throws IOException {
        try (FileInputStream fis = new FileInputStream(file)) {
            byte[] buffer = new byte[8192];
            int read;
            while ((read = fis.read(buffer)) != -1) {
                dos.write(buffer, 0, read);
            }
        }
    }

    private void sendFileZeroCopy(SocketChannel sc, File file) throws IOException {
        try (FileChannel fc = new FileInputStream(file).getChannel()) {
            long position = 0;
            long size = fc.size();
            while (position < size) {
                long transferred = fc.transferTo(position, size - position, sc);
                if (transferred <= 0) break;
                position += transferred;
            }
        }
    }

    public static void main(String[] args) throws IOException {
        String host = "localhost";
        int port = 9000;
        String filePath = "B:/3Y/OS/เอกสารประกอบการสอน OS บท1-6.pdf";
        File file = new File(filePath);
        if (!file.exists()) {
            System.out.println("ไม่พบไฟล์: " + file.getAbsolutePath());
            return;
        }

        Scanner scanner = new Scanner(System.in);
        System.out.println("1:zerocopy | 2.copy");
        int choice = scanner.nextInt();
        boolean zeroCopy = false;// false = Copy ปกติ
        if (choice != 1 && choice != 2) {
            System.out.println("Error: invalid choice");
        } else if (choice == 1) {
            zeroCopy = true; // true = ใช้ ZeroCopy, false = Copy ปกติ
            new FileClient(host, port).sendFile(filePath, zeroCopy);
        }else {
            new FileClient(host, port).sendFile(filePath, zeroCopy);
        }



    }
}
