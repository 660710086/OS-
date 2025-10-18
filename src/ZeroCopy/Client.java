package ZeroCopy;

import java.io.*;
import java.net.*;
import java.nio.channels.*;
import java.nio.file.*;
import static java.nio.file.StandardOpenOption.*;

public class Client {

    private final String host;
    private final int port;
    public Client(String host, int port) {
        this.host = host; this.port = port;
    }

    public static void main(String[] args) throws Exception {
        if (args.length < 3) {
            System.out.println("Usage: java Client <host> <port> <filePath> [BUFFER|ZEROCOPY]");
            return;
        }
        String host = args[0];
        int port = Integer.parseInt(args[1]);
        Path file = Paths.get(args[2]);
        String mode = args.length >= 4 ? args[3] : "BUFFER";

        if (!Files.isRegularFile(file)) {
            System.err.println("Not a file: " + file);
            return;
        }
        new Client(host, port).send(file, mode);
    }

    public void send(Path file, String mode) throws IOException {
        String filename = file.getFileName().toString();
        long fileSize = Files.size(file);

        try (SocketChannel sc = SocketChannel.open(new InetSocketAddress(host, port))) {
            sc.configureBlocking(true);
            System.out.println("Connected to server " + host + ":" + port);

            // ส่ง header ผ่าน DataOutputStream
            try (DataOutputStream dos = new DataOutputStream(sc.socket().getOutputStream())) {
                dos.writeUTF(mode);        // "BUFFER" หรือ "ZEROCOPY"
                dos.writeUTF(filename);    // ชื่อไฟล์
                dos.writeLong(fileSize);   // ขนาดไฟล์จริง
                dos.flush();

                if ("BUFFER".equalsIgnoreCase(mode)) {
                    sendBuffer(dos, file, fileSize);
                } else if ("ZEROCOPY".equalsIgnoreCase(mode)) {
                    sendZeroCopy(sc, file, fileSize);
                } else {
                    throw new IOException("Unknown mode: " + mode);
                }

                // โปรโตคอลพื้นฐาน: แจ้งจบฝั่งส่ง (optional แต่ช่วยให้ Server เห็น EOF ชัด)
                sc.socket().shutdownOutput();
                System.out.println("Client: send OK (" + mode + ")");
            }
        }
    }

    // โหมดปกติ: อ่านไฟล์ → เขียน network stream (จำกัดทีละเท่าที่เหลือ)
    private void sendBuffer(DataOutputStream out, Path file, long size) throws IOException {
        byte[] buf = new byte[64 * 1024];
        long sent = 0;
        try (BufferedInputStream in = new BufferedInputStream(Files.newInputStream(file, READ))) {
            while (sent < size) {
                int toRead = (int) Math.min(buf.length, size - sent);
                int n = in.read(buf, 0, toRead);
                if (n == -1) throw new EOFException("File shorter than declared at " + sent + "/" + size);
                out.write(buf, 0, n);
                sent += n;
            }
            out.flush();
        }
        if (sent != size) throw new EOFException("Size mismatch: " + sent + " vs " + size);
    }

    // โหมด zero-copy: FileChannel.transferTo → SocketChannel (วนจนครบ)
    private void sendZeroCopy(SocketChannel sc, Path file, long announced) throws IOException {
        try (FileChannel src = FileChannel.open(file, READ)) {
            long fileSize = src.size();
            long target   = Math.min(announced, fileSize); // กันประกาศผิด
            long pos = 0;
            while (pos < target) {
                long n = src.transferTo(pos, target - pos, sc);
                if (n > 0) { pos += n; continue; }
                // blocking mode คืน 0 = ชะงักชั่วคราว → spin-wait สั้น ๆ
                Thread.onSpinWait();
            }
            if (pos != target) throw new EOFException("transferTo incomplete: " + pos + "/" + target);
        }
    }
}
