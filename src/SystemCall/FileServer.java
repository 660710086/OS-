package SystemCall;

import java.io.*;
import java.net.*;
import java.nio.channels.Channels;
import java.nio.channels.FileChannel;
import java.nio.file.StandardOpenOption;

public class FileServer {

    private static final int PORT = 5000;

    public static void main(String[] args) throws IOException {
        ServerSocket serverSocket = new ServerSocket(PORT);
        System.out.println("Server started on port " + PORT);
        try {
            while (true) {
                Socket clientSocket = serverSocket.accept();
                System.out.println("Client connected.");
                handleClient(clientSocket);
            }
        }catch (IOException e){
            e.printStackTrace();
        }

    }

    private static void handleClient(Socket clientSocket) {
        try (
                BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
                DataOutputStream out = new DataOutputStream(clientSocket.getOutputStream())
        ) {
            String fileName = in.readLine();
            String mode = in.readLine(); // "normal" or "zerocopy"

            File file = new File(fileName);
            if (!file.exists()) {
                out.writeBytes("NOT_FOUND\n");
                System.out.println("Requested file not found: " + fileName);
                return;
            }

            out.writeBytes("FOUND\n");

            if ("normal".equalsIgnoreCase(mode)) {
                sendFileNormal(file, clientSocket.getOutputStream());
            } else if ("zerocopy".equalsIgnoreCase(mode)) {
                sendFileZeroCopy(file, clientSocket);
            } else {
                out.writeBytes("INVALID_MODE\n");
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void sendFileNormal(File file, OutputStream outputStream) {
        try (BufferedInputStream fileIn = new BufferedInputStream(new FileInputStream(file))) {
            byte[] buffer = new byte[4096];
            int bytesRead;
            while ((bytesRead = fileIn.read(buffer)) != -1) {
                outputStream.write(buffer, 0, bytesRead);
            }
            System.out.println("File sent using NORMAL method.");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void sendFileZeroCopy(File file, Socket socket) {
        try (
                FileChannel fileChannel = FileChannel.open(file.toPath(), StandardOpenOption.READ);
                OutputStream out = socket.getOutputStream()
        ) {
            long position = 0;
            long transferred;
            while (position < fileChannel.size()) {
                // FileChannel.transferTo will use sendfile() under the hood if supported
                transferred = fileChannel.transferTo(position, fileChannel.size() - position,
                        Channels.newChannel(out));
                position += transferred;
            }
            System.out.println("File sent using ZERO-COPY method.");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}