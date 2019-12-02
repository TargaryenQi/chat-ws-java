package edu.bu.cs622.chat;


import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import edu.bu.cs622.db.MongoDB;
import edu.bu.cs622.fileprocessor.MergeFile;
import edu.bu.cs622.fileprocessor.ParseFile;
import edu.bu.cs622.message.Message;
import edu.bu.cs622.message.MessageType;
import edu.bu.cs622.message.SearchResult;
import edu.bu.cs622.message.SearchType;
import edu.bu.cs622.search.BruteForce;
import edu.bu.cs622.search.Lucene;
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

    /**
     * On receive different type message and react with different actions.
     *      USER_JOINED: add user and connection;
     *      USER_LEFT: remove user and connection;
     *      TEXT_MESSAGE: broadcast msg, search msg and broadcast search result.
     *
     * @param conn connection between client and server.
     * @param message message received from client.
     */
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

    /**
     * Search msg with different type search and broadcast the msg with search result.
     * @param msg msg to be searched.
     */
    private void processAndBroadcastMessage(Message msg) {
        ObjectMapper mapper = new ObjectMapper();
        try {
            User user = msg.getUser();
            // Get the name of the user who make the query.
            String userName = msg.getUser().getName();
            // Note the title to show which user ask for the query.
            user.setName("Answer for " + userName);
            // Search message with different type search.
            Message messageProcessed = messageProcessor(msg);

            String messageJson = mapper.writeValueAsString(messageProcessed);
            for (WebSocket sock : connections) {
                sock.send(messageJson);
            }

            user.setName(userName);
        } catch (JsonProcessingException e) {
            logger.error("Cannot convert message to json.");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Search with different type search.
     * @param msg msg to search.
     * @return msg with search results from different type search.
     * @throws Exception exceptions.
     */
    private Message messageProcessor(Message msg) throws Exception {
        // Brute force search.
        SearchResult bruteForceSearchResult = BruteForce.search(msg.getData());
        msg.setSearchResults(bruteForceSearchResult);

        // Lucene Search.
        SearchResult luceneSearchResult = new Lucene("MergedData/allDaysData.txt").query(msg.getData());
        msg.setSearchResults(luceneSearchResult);

        return msg;
    }

    /**
     * Broadcast msg before process it.
     * @param msg msg to broadcast.
     */
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

    /**
     * Add user.
     * @param user user to add.
     * @param conn connection to create.
     * @throws JsonProcessingException exception.
     */
    private void addUser(User user, WebSocket conn) throws JsonProcessingException {
        users.put(conn, user);

        acknowledgeUserJoined(user, conn);
        broadcastUserActivityMessage(MessageType.USER_JOINED);
    }

    /**
     * Remove user.
     * @param conn connection to remove.
     * @throws JsonProcessingException exception.
     */
    private void removeUser(WebSocket conn) throws JsonProcessingException {
        users.remove(conn);
        broadcastUserActivityMessage(MessageType.USER_LEFT);
    }

    /**
     * Inform the client that the User has been added to server.
     * @param user user that added.
     * @param conn connection that established.
     * @throws JsonProcessingException exceptions.
     */
    private void acknowledgeUserJoined(User user, WebSocket conn) throws JsonProcessingException {
        Message message = new Message();
        message.setType(MessageType.USER_JOINED_ACK);
        message.setUser(user);
        conn.send(new ObjectMapper().writeValueAsString(message));
    }

    /**
     * Broadcast the user activity.
     * @param messageType messageType which represent the User activity.
     * @throws JsonProcessingException exception.
     */
    private void broadcastUserActivityMessage(MessageType messageType) throws JsonProcessingException {
        Message newMessage = new Message();

        ObjectMapper mapper = new ObjectMapper();
        String data = mapper.writeValueAsString(users.values());
        newMessage.setData(data);
        newMessage.setType(messageType);
        broadcastMessage(newMessage);
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
    public void onError(WebSocket conn, Exception ex) {
        if (conn != null) {
            connections.remove(conn);
        }
        assert conn != null;
        System.out.println("ERROR from " + conn.getRemoteSocketAddress().getAddress().getHostAddress());
    }
}
