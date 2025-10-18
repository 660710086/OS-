package ZeroCopy;
import java.io.*;
import java.net.*;
import java.nio.channels.*;
import java.nio.file.*;
import static java.nio.file.StandardOpenOption.*;

public class Server {

    private final int port;
    private final Path saveDir; // โฟลเดอร์ปลายทาง
    public Server(int port, String saveDir) {
        this.port = port;
        this.saveDir = Paths.get(saveDir);
    }

    public static void main(String[] args) throws Exception {
        if (args.length < 2) {
            System.out.println("Usage: java Server <port> <saveDir>");
            return;
        }
        int port = Integer.parseInt(args[0]);
        String saveDir = args[1];
        Files.createDirectories(Paths.get(saveDir));
        new Server(port, saveDir).runOnce(); // ตัวอย่าง: รับ 1 การเชื่อมต่อ
    }

    public void runOnce() throws IOException {
        try (ServerSocketChannel ssc = ServerSocketChannel.open()) {
            ssc.bind(new InetSocketAddress(port));
            System.out.println("Server listening on port " + port);

            try (SocketChannel sc = ssc.accept()) {
                sc.configureBlocking(true);
                System.out.println("Client connected: " + sc.getRemoteAddress());

                // อ่าน header ผ่าน DataInputStream บน socket ของ channel
                try (DataInputStream dis = new DataInputStream(sc.socket().getInputStream())) {
                    String mode = dis.readUTF();        // "BUFFER" หรือ "ZEROCOPY"
                    String filename = dis.readUTF();    // ชื่อไฟล์
                    long size = dis.readLong();         // ขนาดไฟล์

                    Path outPath = saveDir.resolve(filename);
                    System.out.printf("Receiving file: %s (%d bytes) mode=%s%n", filename, size, mode);

                    if ("BUFFER".equalsIgnoreCase(mode)) {
                        receiveBuffer(dis, outPath, size);
                    } else if ("ZEROCOPY".equalsIgnoreCase(mode)) {
                        receiveZeroCopy(sc, outPath, size);
                    } else {
                        throw new IOException("Unknown mode: " + mode);
                    }
                    System.out.println("Server: receive OK → " + outPath.toAbsolutePath());
                }
            }
        }
    }

    // โหมดปกติ: อ่านจาก network stream → เขียนไฟล์ (จำกัดทีละเท่าที่เหลือ)
    private void receiveBuffer(DataInputStream in, Path outPath, long size) throws IOException {
        byte[] buf = new byte[64 * 1024];
        long got = 0;
        try (BufferedOutputStream out = new BufferedOutputStream(
                Files.newOutputStream(outPath, CREATE, TRUNCATE_EXISTING, WRITE))) {
            while (got < size) {
                int toRead = (int) Math.min(buf.length, size - got);
                int n = in.read(buf, 0, toRead);
                if (n == -1) throw new EOFException("Early EOF at " + got + "/" + size);
                out.write(buf, 0, n);
                got += n;
            }
            // out.close() จะ flush ให้อัตโนมัติ
        }
        if (got != size) throw new EOFException("Size mismatch: " + got + " vs " + size);
    }

    // โหมด zero-copy: รับจาก SocketChannel → FileChannel ด้วย transferFrom (วนจนครบ)
    private void receiveZeroCopy(SocketChannel sc, Path outPath, long size) throws IOException {
        try (FileChannel dest = FileChannel.open(outPath, CREATE, TRUNCATE_EXISTING, WRITE)) {
            long pos = 0;
            while (pos < size) {
                long n = dest.transferFrom(sc, pos, size - pos);
                if (n > 0) { pos += n; continue; }
                // ใน blocking mode การคืน 0 ถือว่าชะงักชั่วคราว → spin-wait สั้น ๆ
                Thread.onSpinWait();
            }
            if (pos != size) throw new EOFException("transferFrom incomplete: " + pos + "/" + size);
            // dest.force(true); // ถ้าต้องการความทนทานลงดิสก์จริง
        }
    }
}