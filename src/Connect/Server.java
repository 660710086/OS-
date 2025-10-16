void main() {
    int port = 8000;

    try (ServerSocket serverSocket = new ServerSocket(port)){
        System.out.println("Connect.Server started on port " + port);


        //รอ client connect
        Socket clientSocket = serverSocket.accept();
        System.out.println("Client connected on port " + clientSocket.getPort() + " IpAddress : "+ clientSocket.getInetAddress());

        // input OutputStream

        BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
        PrintWriter out = new PrintWriter(clientSocket.getOutputStream(), true);


        String message;
        while ((message = in.readLine()) != null) {
            System.out.println("Message from client: " + message);
            out.println("Hello from server! ");
        }

    } catch (IOException e) {
        throw new RuntimeException(e);
    }
}