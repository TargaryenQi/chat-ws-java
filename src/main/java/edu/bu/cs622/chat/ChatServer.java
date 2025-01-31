package edu.bu.cs622.chat;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import edu.bu.cs622.db.BruteForce;
import edu.bu.cs622.db.DBSearch;
import edu.bu.cs622.db.LuceneHelper;
import edu.bu.cs622.db.LuceneSearch;
import edu.bu.cs622.db.MongoDB;
import edu.bu.cs622.db.MysqlDAO;
import edu.bu.cs622.fileprocessor.MergeFile;
import edu.bu.cs622.fileprocessor.ParseFile;
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
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ChatServer extends WebSocketServer {

	private final static Logger logger = LogManager.getLogger(ChatServer.class);
	private static MongoDB mongoDB;

	private HashMap<User, WebSocket> users;

	private Set<WebSocket> connections;

	private User simpleBot;

	private ChatServer(int port) {
		super(new InetSocketAddress(port));
		connections = new HashSet<>();
		users = new HashMap<>();
		simpleBot = createSimpleBot();
		mongoDB = new MongoDB("smartwatch");
		LuceneHelper.createIndex();
	}

	private User createSimpleBot() {
		if (simpleBot == null) {
			simpleBot = new User("Simple Bot");
		}
		return simpleBot;
	}

	public static void main(String[] args) throws IOException {
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
	 * On receive different type message and react with different actions.
	 * USER_JOINED: add user and connection; USER_LEFT: remove user and connection;
	 * TEXT_MESSAGE: broadcast msg, search msg and broadcast search result.
	 *
	 * @param conn    connection between client and server.
	 * @param message message received from client.
	 */
	@Override
	public void onMessage(WebSocket conn, String message) {
		ObjectMapper mapper = new ObjectMapper();
		try {
			Message msg = mapper.readValue(message, Message.class);
			// Log message from user to MongoDB.
			mongoDB.addToUserHistory(msg, msg.getUser());
			switch (msg.getType()) {
			case USER_JOINED:
				User user = new User(msg.getUser().getName());
				addUserAndSendWelcomeMessage(user, conn);
				break;
			case USER_LEFT:
				removeUser(conn);
				break;
			case TEXT_MESSAGE:
				sendMessage(msg, conn);
				// Most process happened here.
				// To validate input, extract usefully info, make the query and send back msg
				// with search result.
				// Also log the processed message to MongoDB.
				processAndSendAndLogMessage(msg, conn, msg.getUser());
			}

			System.out.println(
					"Message from user: " + msg.getUser() + ", text: " + msg.getData() + ", type:" + msg.getType());
			logger.info("Message from user: " + msg.getUser() + ", text: " + msg.getData());
		} catch (IOException e) {
			logger.error("Wrong message format.");
			// return error message to user
		}
	}

	/**
	 * Validate user input. Extract useful data. Make query. Send back message.
	 *
	 * @param msg msg to be searched.
	 */
	private void processAndSendAndLogMessage(Message msg, WebSocket conn, User user) {
		ObjectMapper mapper = new ObjectMapper();
		try {
			msg.setUser(simpleBot);
			msg.setType(MessageType.BOT_RESPONSE);
			// Search message with different type search.
			Message messageProcessed = messageProcessor(msg);
			mongoDB.addToUserHistory(messageProcessed, user);
			String messageJson = mapper.writeValueAsString(messageProcessed);
			conn.send(messageJson);
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
		String str = msg.getData();
		String QType = null;
		String date;
		if (str.contains("How many") || str.contains("How much") ) {
			if (str.contains("step"))
				QType = "Activity";
			else
				QType = "HeartRate";
		} else if (str.contains("Did") || str.contains("Do")) {
			QType = "ActivFit";
		} else
			QType = "Wrong";
		if (QType.equals("Wrong")) {
			msg.setData("Invalid input.");
			return msg;
		} else {
			Pattern pattern = Pattern.compile("[0-9]{4}[-][0-9]{1,2}[-][0-9]{1,2}");
			Matcher matcher = pattern.matcher(str);
			if (matcher.find()) {
				date = matcher.group();
			} else {
				date = "";
			}
			// date = str.substring(str.indexOf("'") + 1,
			// str.indexOf("'",str.indexOf("'")+1));
			date = isValidDate(date);
			if (date.equals("")) {
				msg.setData("Invalid input.");
				return msg;
			}
			if (QType.equals("Activity")) {
				// brute force search result
				BruteForce bfSearch = new BruteForce();
				SearchResult bfst = new SearchResult(SearchType.BRUTE_FORCE);
				startTime = System.currentTimeMillis();
				int number = bfSearch.howManyStepsOnOneDay(date);
				endTime = System.currentTimeMillis();
				elapsedTime = endTime - startTime;
				bfst.setTimeConsuming(elapsedTime);
				if (number > 0)
					bfst.setResult(String.valueOf(number));
				else
					bfst.setResult("0");
				msg.addSearchResult(bfst);

    			// lucene search result
    			DBSearch luceneSearch = new LuceneSearch();
    			SearchResult lucenest = new SearchResult(SearchType.LUCENE);
    			startTime = System.currentTimeMillis();
    			number = luceneSearch.howManyStepsOnOneDay(date);
    			endTime = System.currentTimeMillis();
    			elapsedTime = endTime - startTime;
    			lucenest.setTimeConsuming(elapsedTime);
    			if(number > 0)
    				lucenest.setResult(String.valueOf(number));
    			else
    				lucenest.setResult("0");
    			msg.addSearchResult(lucenest);

				// mongoDb search result
				DBSearch mongoDbSearch = new MongoDB("smartwatch");
				SearchResult mongoDbst = new SearchResult(SearchType.MONGODB);
				startTime = System.currentTimeMillis();
				number = mongoDbSearch.howManyStepsOnOneDay(date);
				endTime = System.currentTimeMillis();
				elapsedTime = endTime - startTime;
				mongoDbst.setTimeConsuming(elapsedTime);
				if (number > 0)
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
				if (number > 0)
					mysqlst.setResult(String.valueOf(number));
				else
					mysqlst.setResult("0");
				msg.addSearchResult(mysqlst);
			} else if (QType.equals("HeartRate")) {
				// brute force search result
				BruteForce bfSearch = new BruteForce();
				SearchResult bfst = new SearchResult(SearchType.BRUTE_FORCE);
				startTime = System.currentTimeMillis();
				int number = bfSearch.howManyHeartRateRecords(date);
				endTime = System.currentTimeMillis();
				elapsedTime = endTime - startTime;
				bfst.setTimeConsuming(elapsedTime);
				if (number > 0)
					bfst.setResult(String.valueOf(number));
				else
					bfst.setResult("0");
				msg.addSearchResult(bfst);

    			// lucene search result
				DBSearch luceneSearch = new LuceneSearch();
    			SearchResult lucenest = new SearchResult(SearchType.LUCENE);
    			startTime = System.currentTimeMillis();
    			number = luceneSearch.howManyHeartRateRecords(date);
    			endTime = System.currentTimeMillis();
    			elapsedTime = endTime - startTime;
    			lucenest.setTimeConsuming(elapsedTime);
    			if(number > 0)
    				lucenest.setResult(String.valueOf(number));
    			else
    				lucenest.setResult("0");
    			msg.addSearchResult(lucenest);

				// mongoDb search result
				DBSearch mongoDbSearch = new MongoDB("smartwatch");
				SearchResult mongoDbst = new SearchResult(SearchType.MONGODB);
				startTime = System.currentTimeMillis();
				number = mongoDbSearch.howManyHeartRateRecords(date);
				endTime = System.currentTimeMillis();
				elapsedTime = endTime - startTime;
				mongoDbst.setTimeConsuming(elapsedTime);
				if (number > 0)
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
				if (number > 0)
					mysqlst.setResult(String.valueOf(number));
				else
					mysqlst.setResult("0");
				msg.addSearchResult(mysqlst);
			} else if (QType.equals("ActivFit")) {
				// brute force search result
				BruteForce bfSearch = new BruteForce();
				SearchResult bfst = new SearchResult(SearchType.BRUTE_FORCE);
				startTime = System.currentTimeMillis();
				boolean number = bfSearch.AreThereRunningEvent(date);
				endTime = System.currentTimeMillis();
				elapsedTime = endTime - startTime;
				bfst.setTimeConsuming(elapsedTime);
				if (number)
					bfst.setResult("Yes, you ran.");
				else
					bfst.setResult("No");
				msg.addSearchResult(bfst);

    			// lucene search result
				DBSearch luceneSearch = new LuceneSearch();
    			SearchResult lucenest = new SearchResult(SearchType.LUCENE);
    			startTime = System.currentTimeMillis();
    			number = luceneSearch.AreThereRunningEvent(date);
    			endTime = System.currentTimeMillis();
    			elapsedTime = endTime - startTime;
    			lucenest.setTimeConsuming(elapsedTime);
    			if(number)
    				lucenest.setResult("Yes, you ran.");
    			else
    				lucenest.setResult("No");
    			msg.addSearchResult(lucenest);

				// mongoDb search result
				DBSearch mongoDbSearch = new MongoDB("smartwatch");
				SearchResult mongoDbst = new SearchResult(SearchType.MONGODB);
				startTime = System.currentTimeMillis();
				number = mongoDbSearch.AreThereRunningEvent(date);
				endTime = System.currentTimeMillis();
				elapsedTime = endTime - startTime;
				mongoDbst.setTimeConsuming(elapsedTime);
				if (number)
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
				if (number)
					mysqlst.setResult("Yes, you ran.");
				else
					mysqlst.setResult("No");
				msg.addSearchResult(mysqlst);
			}
		}
		return msg;
	}

	/**
	 * Add user both to the server and client. Send welcome message to the user.
	 *
	 * @param user user to add.
	 * @param conn connection to create.
	 * @throws JsonProcessingException exception.
	 */
	private void addUserAndSendWelcomeMessage(User user, WebSocket conn) throws JsonProcessingException {
		users.put(user, conn);
		connections.add(conn);
		acknowledgeUserJoined(user, conn);
		sendWelcomeMessage(user, conn);
	}

	/**
	 * Remove user.
	 *
	 * @param conn connection to remove.
	 * @throws JsonProcessingException exception.
	 */
	private void removeUser(WebSocket conn) throws JsonProcessingException {
		Iterator iterator = users.entrySet().iterator();
		while (iterator.hasNext()) {
			Map.Entry entry = (Map.Entry) iterator.next();
			if (conn.equals(entry.getValue())) {
				iterator.remove();
				break;
			}
		}
		connections.remove(conn);
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
	 * The Simple Bot will sent an welcome message when a new User join.
	 *
	 * @param user new User.
	 * @throws JsonProcessingException exceptions.
	 */
	private void sendWelcomeMessage(User user, WebSocket conn) throws JsonProcessingException {
		Message message = new Message();
		message.setData("Welcome " + user.getName() + "! What can I do for you?");
		message.setUser(simpleBot);
		message.setType(MessageType.WELCOME);
		conn.send(new ObjectMapper().writeValueAsString(message));
	}

	private void sendMessage(Message msg, WebSocket conn) {
		ObjectMapper mapper = new ObjectMapper();
		try {
			String messageJson = mapper.writeValueAsString(msg);
			conn.send(messageJson);
		} catch (JsonProcessingException e) {
			logger.error("Cannot convert message to json.");
		}
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

	public static String isValidDate(String str) {
		SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
		try {
			str = dateFormat.format(dateFormat.parse(str));
		} catch (ParseException e1) {
			// TODO Auto-generated catch block
			return "";
		}
		DateFormat formatter = new SimpleDateFormat("yyyy-MM-dd");
		try {
			formatter.setLenient(false);
			Date date = (Date) formatter.parse(str);
			if (str.equals(formatter.format(date)))
				return str;
			return "";
		} catch (Exception e) {
			return "";
		}
	}
}
