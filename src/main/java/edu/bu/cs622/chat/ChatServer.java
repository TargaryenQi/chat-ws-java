package edu.bu.cs622.chat;


import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import edu.bu.cs622.db.BruteForce;
import edu.bu.cs622.db.DBSearch;
import edu.bu.cs622.db.MongoDB;
import edu.bu.cs622.db.MysqlDAO;
import edu.bu.cs622.message.Message;
import edu.bu.cs622.message.MessageType;
import edu.bu.cs622.message.SearchResult;
import edu.bu.cs622.message.SearchType;
import edu.bu.cs622.user.User;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.java_websocket.WebSocket;
import org.java_websocket.handshake.ClientHandshake;
import org.java_websocket.server.WebSocketServer;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

public class ChatServer extends WebSocketServer {

    private final static Logger logger = LogManager.getLogger(ChatServer.class);

//    private HashMap<WebSocket, User> users;
    private HashMap<User,WebSocket> users;

    private Set<WebSocket> connections;

    private User simpleBot;

    private ChatServer(int port) {
        super(new InetSocketAddress(port));
        connections = new HashSet<>();
        users = new HashMap<>();
        simpleBot = createSimpleBot();
    }

    private User createSimpleBot() {
        if(simpleBot == null) {
            simpleBot = new User("Simple Bot");
        }
        return simpleBot;
    }

    public static void main(String[] args) throws IOException {
        // Merge all the data to "merged.txt".
//        MergeFile.mergeDirectoryToSingleFile("SampleUserSmartwatch", "MergedData/allDaysData.txt");
//
//        //Preparse the data to a dictionary.
//        //      Key: Sensor Name.
//        //      Value: an arrayList of the JSON string of particular Sensor
//        HashMap<String, ArrayList<String>> sensorDictionary = ParseFile
//            .preParseFile("MergedData/allDaysData.txt");
//
//        // Create a mongoDB
//        MongoDB mongoDB = new MongoDB("smartwatch");
//        // Transfer all the data to the mongoDB
//        mongoDB.transferDataToDatabase(sensorDictionary);

        int port;
        try {
            port = Integer.parseInt(System.getenv("PORT"));
        } catch (NumberFormatException nfe) {
            port = 9000;
        }
        new ChatServer(port).start();
        System.out.println("Server start...");
    }

    /**
     * On receive different type message and react with different actions. USER_JOINED: add user and
     * connection; USER_LEFT: remove user and connection; TEXT_MESSAGE: broadcast msg, search msg
     * and broadcast search result.
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
                    User user = new User(msg.getUser().getName());
                    addUser(user, conn);
//                    broadcastWelcomeForUserJoin(user);
                    break;
                case USER_LEFT:
                    removeUser(conn);
                    break;
                case TEXT_MESSAGE:
//                    broadcastMessage(msg);
                    sendMessage(msg,conn);
//                    processAndBroadcastMessage(msg);
                    processAndSendMessage(msg,conn);
            }

            System.out.println(
                "Message from user: " + msg.getUser() + ", text: " + msg.getData() + ", type:" + msg
                    .getType());
            logger.info("Message from user: " + msg.getUser() + ", text: " + msg.getData());
        } catch (IOException e) {
            logger.error("Wrong message format.");
            // return error message to user
        }
    }

    /**
     * Search msg with different type search and broadcast the msg with search result.
     *
     * @param msg msg to be searched.
     */
    private void processAndBroadcastMessage(Message msg) {
        ObjectMapper mapper = new ObjectMapper();
        try {
            msg.setUser(simpleBot);
            // Search message with different type search.
            Message messageProcessed = messageProcessor(msg);

            String messageJson = mapper.writeValueAsString(messageProcessed);
            for (WebSocket sock : connections) {
                sock.send(messageJson);
            }
        } catch (JsonProcessingException e) {
            logger.error("Cannot convert message to json.");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Search msg with different type search and broadcast the msg with search result.
     *
     * @param msg msg to be searched.
     */
    private void processAndSendMessage(Message msg,WebSocket socket) {
        ObjectMapper mapper = new ObjectMapper();
        try {
            msg.setUser(simpleBot);
            // Search message with different type search.
            Message messageProcessed = messageProcessor(msg);

            String messageJson = mapper.writeValueAsString(messageProcessed);

            socket.send(messageJson);
        } catch (JsonProcessingException e) {
            logger.error("Cannot convert message to json.");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Search with different type search.
     *
     * @param msg msg to search.
     * @return msg with search results from different type search.
     * @throws Exception exceptions.
     */
    private Message messageProcessor(Message msg) throws Exception {
		long startTime, endTime, elapsedTime;
		msg.setType(MessageType.TEXT_MESSAGE);
    	String str = msg.getData();
    	String QType = null;
    	String date;
    	if(str.contains("How many")) {
    		if(str.contains("steps"))
    			QType = "Activity";
    		else
    			QType = "HeartRate";
    	}
    	else if(str.contains("Did") || str.contains("Do")) {
    		QType = "ActivFit";
    	}
    	else
    		QType = "Wrong";
    	if(QType.equals("Wrong")) {
        msg.setData("Invalid input.");
        return msg;
      }

    	else {
        date = str.substring(str.indexOf("'") + 1, str.indexOf("'",str.indexOf("'")+1));
        if(!isValidDate(date)) {
          msg.setData("Invalid input.");
          return msg;
        }
        if(QType.equals("Activity")) {
    			// brute force search result
    			BruteForce bfSearch = new BruteForce();
    			SearchResult bfst = new SearchResult(SearchType.BRUTE_FORCE);
    			startTime = System.currentTimeMillis();
    			int number = bfSearch.howManyStepsOnOneDay(date);
    			endTime = System.currentTimeMillis();
    			elapsedTime = endTime - startTime;
    			bfst.setTimeConsuming(elapsedTime);
    			if(number > 0)
    				bfst.setResult(String.valueOf(number));
    			else
    				bfst.setResult("0");
    			msg.addSearchResult(bfst);
    			
//    			// lucene search result
//    			Lucene luceneSearch = new Lucene("filename");
//    			SearchResult lucenest = new SearchResult(SearchType.LUCENE);
//    			startTime = System.currentTimeMillis();
//    			number = luceneSearch.(date);
//    			endTime = System.currentTimeMillis();
//    			elapsedTime = endTime - startTime;
//    			lucenest.setTimeConsuming(elapsedTime);
//    			if(number > 0)
//    				lucenest.setResults(String.valueOf(number));
//    			else
//    				lucenest.setResults("0");
//    			msg.addSearchResult(lucenest);
    			
    			// mongoDb search result
    			DBSearch mongoDbSearch = new MongoDB("smartwatch");
    			SearchResult mongoDbst = new SearchResult(SearchType.MONGODB);
    			startTime = System.currentTimeMillis();
    			number = mongoDbSearch.howManyStepsOnOneDay(date);
    			endTime = System.currentTimeMillis();
    			elapsedTime = endTime - startTime;
    			mongoDbst.setTimeConsuming(elapsedTime);
    			if(number > 0)
    				mongoDbst.setResult(String.valueOf(number));
    			else
    				mongoDbst.setResult("0");
    			msg.addSearchResult(mongoDbst);
    			
    			// mysql search result
    			DBSearch mysqlSearch = new MysqlDAO();
    			SearchResult mysqlst = new SearchResult(SearchType.MYSQL);
    			startTime = System.currentTimeMillis();
    			number = mysqlSearch.howManyStepsOnOneDay(date);
    			endTime = System.currentTimeMillis();
    			elapsedTime = endTime - startTime;
    			mysqlst.setTimeConsuming(elapsedTime);
    			if(number > 0)
    				mysqlst.setResult(String.valueOf(number));
    			else
    				mysqlst.setResult("0");
    			msg.addSearchResult(mysqlst);
    		}
    		else if(QType.equals("HeartRate"))
    		{
    			// brute force search result
    			BruteForce bfSearch = new BruteForce();
    			SearchResult bfst = new SearchResult(SearchType.BRUTE_FORCE);
    			startTime = System.currentTimeMillis();
    			int number = bfSearch.howManyHeartRateRecords(date);
    			endTime = System.currentTimeMillis();
    			elapsedTime = endTime - startTime;
    			bfst.setTimeConsuming(elapsedTime);
    			if(number > 0)
    				bfst.setResult(String.valueOf(number));
    			else
    				bfst.setResult("0");
    			msg.addSearchResult(bfst);
    			
//    			// lucene search result
//    			Lucene luceneSearch = new Lucene("filename");
//    			SearchResult lucenest = new SearchResult(SearchType.LUCENE);
//    			startTime = System.currentTimeMillis();
////    			number = luceneSearch.howManyHeartRateRecords(date);
//    			endTime = System.currentTimeMillis();
//    			elapsedTime = endTime - startTime;
//    			lucenest.setTimeConsuming(elapsedTime);
//    			if(number > 0)
//    				lucenest.setResults(String.valueOf(number));
//    			else
//    				lucenest.setResults("0");
//    			msg.addSearchResult(lucenest);
    			
    			// mongoDb search result
    			DBSearch mongoDbSearch = new MongoDB("smartwatch");
    			SearchResult mongoDbst = new SearchResult(SearchType.MONGODB);
    			startTime = System.currentTimeMillis();
    			number = mongoDbSearch.howManyHeartRateRecords(date);
    			endTime = System.currentTimeMillis();
    			elapsedTime = endTime - startTime;
    			mongoDbst.setTimeConsuming(elapsedTime);
    			if(number > 0)
    				mongoDbst.setResult(String.valueOf(number));
    			else
    				mongoDbst.setResult("0");
    			msg.addSearchResult(mongoDbst);
    			
    			// mysql search result
    			DBSearch mysqlSearch = new MysqlDAO();
    			SearchResult mysqlst = new SearchResult(SearchType.MYSQL);
    			startTime = System.currentTimeMillis();
    			number = mysqlSearch.howManyHeartRateRecords(date);
    			endTime = System.currentTimeMillis();
    			elapsedTime = endTime - startTime;
    			mysqlst.setTimeConsuming(elapsedTime);
    			if(number > 0)
    				mysqlst.setResult(String.valueOf(number));
    			else
    				mysqlst.setResult("0");
    			msg.addSearchResult(mysqlst);
    		}
    		else if(QType.equals("ActivFit"))
    		{
    			// brute force search result
    			BruteForce bfSearch = new BruteForce();
    			SearchResult bfst = new SearchResult(SearchType.BRUTE_FORCE);
    			startTime = System.currentTimeMillis();
    			boolean number = bfSearch.AreThereRunningEvent(date);
    			endTime = System.currentTimeMillis();
    			elapsedTime = endTime - startTime;
    			bfst.setTimeConsuming(elapsedTime);
    			if(number)
    				bfst.setResult("Yes, you ran.");
    			else
    				bfst.setResult("No");
    			msg.addSearchResult(bfst);
    			
//    			// lucene search result
//    			Lucene luceneSearch = new Lucene("filename");
//    			SearchResult lucenest = new SearchResult(SearchType.LUCENE);
//    			startTime = System.currentTimeMillis();
//    			number = luceneSearch.AreThereRunningEvent(date);
//    			endTime = System.currentTimeMillis();
//    			elapsedTime = endTime - startTime;
//    			lucenest.setTimeConsuming(elapsedTime);
//    			if(number)
//    				lucenest.setResults("Yes, you ran.");
//    			else
//    				lucenest.setResults("No");
//    			msg.addSearchResult(lucenest);
    			
    			// mongoDb search result
    			DBSearch mongoDbSearch = new MongoDB("smartwatch");
    			SearchResult mongoDbst = new SearchResult(SearchType.MONGODB);
    			startTime = System.currentTimeMillis();
    			number = mongoDbSearch.AreThereRunningEvent(date);
    			endTime = System.currentTimeMillis();
    			elapsedTime = endTime - startTime;
    			mongoDbst.setTimeConsuming(elapsedTime);
    			if(number)
    				mongoDbst.setResult("Yes, you ran.");
    			else
    				mongoDbst.setResult("No");
    			msg.addSearchResult(mongoDbst);
    			
    			// mysql search result
    			DBSearch mysqlSearch = new MysqlDAO();
    			SearchResult mysqlst = new SearchResult(SearchType.MYSQL);
    			startTime = System.currentTimeMillis();
    			number = mysqlSearch.AreThereRunningEvent(date);
    			endTime = System.currentTimeMillis();
    			elapsedTime = endTime - startTime;
    			mysqlst.setTimeConsuming(elapsedTime);
    			if(number)
    				mysqlst.setResult("Yes, you ran.");
    			else
    				mysqlst.setResult("No");
    			msg.addSearchResult(mysqlst);
    		}
    	}
    	return msg;
    }

    /**
     * Add user.
     *
     * @param user user to add.
     * @param conn connection to create.
     * @throws JsonProcessingException exception.
     */
    private void addUser(User user, WebSocket conn) throws JsonProcessingException {
        users.put(user, conn);
        acknowledgeUserJoined(user, conn);
        broadcastUserActivityMessage(MessageType.USER_JOINED);
    }

    /**
     * Remove user.
     *
     * @param conn connection to remove.
     * @throws JsonProcessingException exception.
     */
    private void removeUser(WebSocket conn) throws JsonProcessingException {
        users.remove(conn);
        broadcastUserActivityMessage(MessageType.USER_LEFT);
    }

    /**
     * Inform the client that the User has been added to server.
     *
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
     *
     * @param messageType messageType which represent the User activity.
     * @throws JsonProcessingException exception.
     */
    private void broadcastUserActivityMessage(MessageType messageType)
        throws JsonProcessingException {
        Message newMessage = new Message();

        ObjectMapper mapper = new ObjectMapper();
        String data = mapper.writeValueAsString(users.values());
        newMessage.setData(data);
        newMessage.setType(messageType);
        broadcastMessage(newMessage);
    }

    /**
     * The Simple Bot will sent an welcome message when a new User join.
     *
     * @param user new User.
     * @throws JsonProcessingException exceptions.
     */
    private void broadcastWelcomeForUserJoin(User user) throws JsonProcessingException {
        Message message = new Message();
        message.setData("Welcome " + user.getName() + "! What do you want to search?");
        message.setUser(simpleBot);
        message.setType(MessageType.WELCOME);
        broadcastMessage(message);
    }

    /**
     * Broadcast msg before process it.
     *
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

    private void sendMessage(Message msg, WebSocket socket) {
        ObjectMapper mapper = new ObjectMapper();
        try {
            String messageJson = mapper.writeValueAsString(msg);
            socket.send(messageJson);
        } catch (JsonProcessingException e) {
            logger.error("Cannot convert message to json.");
        }
    }

    @Override
    public void onOpen(WebSocket webSocket, ClientHandshake clientHandshake) {
        connections.add(webSocket);

        logger.info(
            "Connection established from: " + webSocket.getRemoteSocketAddress().getHostString());
        System.out.println("New connection from " + webSocket.getRemoteSocketAddress().getAddress()
            .getHostAddress());
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
        System.out.println(
            "Closed connection to " + conn.getRemoteSocketAddress().getAddress().getHostAddress());
    }

    @Override
    public void onError(WebSocket conn, Exception ex) {
        if (conn != null) {
            connections.remove(conn);
        }
        assert conn != null;
        System.out
            .println("ERROR from " + conn.getRemoteSocketAddress().getAddress().getHostAddress());
    }
    
	public static boolean isValidDate(String str) {
		DateFormat formatter = new SimpleDateFormat("yyyy-MM-dd");
		try{
			Date date = (Date)formatter.parse(str);
			return str.equals(formatter.format(date));
		}catch(Exception e){
			return false;
		}
	}
}
