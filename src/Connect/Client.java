void main() {
    String serverIp = "127.0.0.1";
    int serverPort = 8000;

    try(Socket socket = new Socket(serverIp, serverPort)) {
        System.out.println("Connected to server");


        BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        PrintWriter out = new PrintWriter(socket.getOutputStream(), true);


        out.println("Hello from client!");

        String response = in.readLine();
        System.out.println("Server says: " + response);
    } catch (IOException e) {
        throw new RuntimeException(e);
    }
}