import java.io.*;

public class TraditionalIOExample {
    public static void main(String[] args) {
        System.out.println("Working dir = " + System.getProperty("user.dir"));

        File inputFile = new File("input.txt");           // ให้ชื่อไฟล์ตรงกับของจริง
        File outputFile = new File("copy_example.txt");

        System.out.println("Looking for: " + inputFile.getAbsolutePath());
        if (!inputFile.isFile()) {
            System.err.println("File not found. Put it in the working dir or use an absolute path.");
            System.exit(1);
        }

        try (FileInputStream fis = new FileInputStream(inputFile);
             FileOutputStream fos = new FileOutputStream(outputFile)) {

            byte[] buffer = new byte[4096];
            int bytesRead;
            while ((bytesRead = fis.read(buffer)) != -1) {
                fos.write(buffer, 0, bytesRead);
            }
            System.out.println("File copied using traditional IO.");

        } catch (IOException e) {
            e.printStackTrace();
            System.exit(1);
        }
    }
}
