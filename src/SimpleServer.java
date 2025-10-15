import java.net.*;
import java.io.IOException;
void main() throws IOException {

    // Here, we create a Socket instance named socket
    ServerSocket serverSocket = new ServerSocket(5001);
    System.out.println("Listening for clients...");
    Socket clientSocket  = serverSocket.accept();
    /*ServerSocket = เบอร์โทรกลาง/พอร์ตที่คอย “รับสาย” (listen)

accept() = รอจนเกิดการเชื่อมต่อ (ServerSocket = เบอร์โทรกลาง/พอร์ตที่คอย “รับสาย” (listen)

accept() = รอจนเกิดการเชื่อมต่อ (TCP three-way handshake เสร็จ) เสร็จ)*/
    System.out.println("Client connected");
    String ClientSocketIP = clientSocket.getInetAddress().toString();
    int ClientSocketPort = clientSocket.getPort();
    System.out.println("Client IP: " + ClientSocketIP);
    System.out.println("Client Port: " + ClientSocketPort);

}

