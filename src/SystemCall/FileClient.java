package SystemCall;

import java.io.*;
import java.net.*;
import java.util.Scanner;

public class FileClient {

    private static final String SERVER_IP = "localhost";
    private static final int SERVER_PORT = 5000;

    static void main(String[] args) {
        try (
                Socket socket = new Socket(SERVER_IP, SERVER_PORT);
                BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                DataOutputStream out = new DataOutputStream(socket.getOutputStream())
        ) {
            Scanner scanner = new Scanner(System.in);

            System.out.print("Enter file name to download: ");
            String fileName = scanner.nextLine();

            System.out.print("Choose mode (normal/zerocopy): ");
            String mode = scanner.nextLine();

            // ส่งชื่อไฟล์และโหมดการดาวน์โหลด
            out.writeBytes(fileName + "\n");
            out.writeBytes(mode + "\n");

            // อ่านการตอบกลับ
            String response = in.readLine();
            if ("NOT_FOUND".equals(response)) {
                System.out.println("File not found on server.");
                return;
            } else if (!"FOUND".equals(response)) {
                System.out.println("Unexpected response from server.");
                return;
            }

            // รับไฟล์และบันทึก
            receiveFile(socket.getInputStream(), "downloaded_" + fileName);

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void receiveFile(InputStream inputStream, String saveAs) {
        try (FileOutputStream fileOut = new FileOutputStream(saveAs)) {
            byte[] buffer = new byte[4096];
            int bytesRead;
            while ((bytesRead = inputStream.read(buffer)) != -1) {
                fileOut.write(buffer, 0, bytesRead);
            }
            System.out.println("File downloaded and saved as " + saveAs);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
