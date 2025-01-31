package edu.bu.cs622.message;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import edu.bu.cs622.user.User;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;

@JsonIgnoreProperties(ignoreUnknown = true)
public class Message implements Serializable{

    private User user;
    private MessageType type;
    private String data;
    private ArrayList<SearchResult> searchResults = new ArrayList<>();

    public MessageType getType() {
        return type;
    }

    public void setType(MessageType type) {
        this.type = type;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public void setData(String data) {
        this.data = data;
    }

    public String getData(){
        return data;
    }

    public ArrayList<SearchResult> getSearchResults() {
        return searchResults;
    }

    public void addSearchResult(SearchResult searchResult) {
        this.searchResults.add(searchResult);
    }
}
