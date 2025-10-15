void main() throws  IOException {
    Socket socket = new Socket();
    socket.connect(new InetSocketAddress("127.0.0.1", 5001), 1000);
    //คือคำสั่ง ฝั่งไคลเอนต์ (TCP) ให้ Socket พยายาม เชื่อมต่อ ไปที่ IP 127.0.0.1 (เครื่องตัวเอง/loopback) พอร์ต 5001
    //
    //ตัวเลข 1000 คือ connect timeout = 1 วินาที (ถ้า 3-way handshake ยังไม่เสร็จภายใน 1s จะโยน SocketTimeoutException)
    //
    //ถ้าเชื่อมต่อสำเร็จ บรรทัดถัดไป System.out.println("Connection Successful!"); ก็จะพิมพ์ข้อความนี้ออกมา
    System.out.println("Connection Successful!");
    
}