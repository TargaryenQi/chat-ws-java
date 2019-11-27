package edu.bu.cs622.chat;


import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import edu.bu.cs622.db.MongoDB;
import edu.bu.cs622.fileprocessor.MergeFile;
import edu.bu.cs622.fileprocessor.ParseFile;
import edu.bu.cs622.message.Message;
import edu.bu.cs622.message.MessageType;
import edu.bu.cs622.search.BruteForce;
import edu.bu.cs622.user.User;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.java_websocket.WebSocket;
import org.java_websocket.handshake.ClientHandshake;
import org.java_websocket.server.WebSocketServer;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

public class ChatServer extends WebSocketServer {

    private final static Logger logger = LogManager.getLogger(ChatServer.class);

    private HashMap<WebSocket, User> users;

    private Set<WebSocket> connections;

    private ChatServer(int port) {
        super(new InetSocketAddress(port));
        connections = new HashSet<>();
        users = new HashMap<>();
    }

    public static void main(String[] args) {
        // Merge all the data to "merged.txt".
        MergeFile.mergeDirectoryToSingleFile("SampleUserSmartwatch", "MergedData/allDaysData.txt");

        //Preparse the data to a dictionary.
        //      Key: Sensor Name.
        //      Value: an arrayList of the JSON string of particular Sensor
        HashMap<String, ArrayList<String>> sensorDictionary = ParseFile.preParseFile("MergedData/allDaysData.txt");

        // Create a mongoDB
        MongoDB mongoDB = new MongoDB("smartwatch");
        // Transfer all the data to the mongoDB
        mongoDB.transferDataToDatabase(sensorDictionary);

        int port;
        try {
            port = Integer.parseInt(System.getenv("PORT"));
        } catch (NumberFormatException nfe) {
            port = 9000;
        }
        new ChatServer(port).start();
    }

    @Override
    public void onOpen(WebSocket webSocket, ClientHandshake clientHandshake) {
        connections.add(webSocket);

        logger.info("Connection established from: " + webSocket.getRemoteSocketAddress().getHostString());
        System.out.println("New connection from " + webSocket.getRemoteSocketAddress().getAddress().getHostAddress());
    }

    @Override
    public void onClose(WebSocket conn, int code, String reason, boolean remote) {
        connections.remove(conn);
        // When connection is closed, remove the user.
        try {
            removeUser(conn);
        } catch (JsonProcessingException e) {
            e.printStackTrace();
        }

        logger.info("Connection closed to: " + conn.getRemoteSocketAddress().getHostString());
        System.out.println("Closed connection to " + conn.getRemoteSocketAddress().getAddress().getHostAddress());
    }

    @Override
    public void onMessage(WebSocket conn, String message) {
        ObjectMapper mapper = new ObjectMapper();
        try {
            Message msg = mapper.readValue(message, Message.class);

            switch (msg.getType()) {
                case USER_JOINED:
                    addUser(new User(msg.getUser().getName()), conn);
                    break;
                case USER_LEFT:
                    removeUser(conn);
                    break;
                case TEXT_MESSAGE:
                    broadcastMessage(msg);
                    processAndBroadcastMessage(msg);
            }

            System.out.println("Message from user: " + msg.getUser() + ", text: " + msg.getData() + ", type:" + msg.getType());
            logger.info("Message from user: " + msg.getUser() + ", text: " + msg.getData());
        } catch (IOException e) {
            logger.error("Wrong message format.");
            // return error message to user
        }
    }

    @Override
    public void onError(WebSocket conn, Exception ex) {

        if (conn != null) {
            connections.remove(conn);
        }
        assert conn != null;
        System.out.println("ERROR from " + conn.getRemoteSocketAddress().getAddress().getHostAddress());
    }

    private void processAndBroadcastMessage(Message msg) {
        ObjectMapper mapper = new ObjectMapper();
        try {
            User user = msg.getUser();
            String userName = msg.getUser().getName();
            user.setName("Answer for " + userName);
            Message messageProcessed = messageProcessor(msg);
            String messageJson = mapper.writeValueAsString(messageProcessed);
            for (WebSocket sock : connections) {
                sock.send(messageJson);
            }
            user.setName(userName);
        } catch (JsonProcessingException e) {
            logger.error("Cannot convert message to json.");
        }
    }

    private Message messageProcessor(Message msg) {
        ArrayList<String> result = BruteForce.search(msg.getData());
        msg.setData(result.toString());
        return msg;
    }

    private void broadcastMessage(Message msg) {
        ObjectMapper mapper = new ObjectMapper();
        try {
            String messageJson = mapper.writeValueAsString(msg);
            for (WebSocket sock : connections) {
                sock.send(messageJson);
            }
        } catch (JsonProcessingException e) {
            logger.error("Cannot convert message to json.");
        }
    }

    private void addUser(User user, WebSocket conn) throws JsonProcessingException {
        users.put(conn, user);

        acknowledgeUserJoined(user, conn);
        broadcastUserActivityMessage(MessageType.USER_JOINED);
    }

    private void removeUser(WebSocket conn) throws JsonProcessingException {
        users.remove(conn);
        broadcastUserActivityMessage(MessageType.USER_LEFT);
    }

    private void acknowledgeUserJoined(User user, WebSocket conn) throws JsonProcessingException {
        Message message = new Message();
        message.setType(MessageType.USER_JOINED_ACK);
        message.setUser(user);
        conn.send(new ObjectMapper().writeValueAsString(message));
    }

    private void broadcastUserActivityMessage(MessageType messageType) throws JsonProcessingException {

        Message newMessage = new Message();

        ObjectMapper mapper = new ObjectMapper();
        String data = mapper.writeValueAsString(users.values());
        newMessage.setData(data);
        newMessage.setType(messageType);
        broadcastMessage(newMessage);
    }
}
