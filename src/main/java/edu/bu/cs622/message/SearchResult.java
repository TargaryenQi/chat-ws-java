package edu.bu.cs622.message;

import java.util.ArrayList;

public class SearchResult {
  private SearchType searchType;
  private int resultNumber;
  private ArrayList<String> results;
  private long timeConsuming;

  public SearchResult(SearchType searchType) {
    this.searchType = searchType;
  }

  public SearchResult(SearchType searchType, int resultNumber, ArrayList<String> results, long timeConsuming) {
    this.searchType = searchType;
    this.resultNumber = resultNumber;
    this.results = results;
    this.timeConsuming = timeConsuming;
  }

  public SearchType getSearchType() {
    return searchType;
  }

  public void setResultNumber(int resultNumber) {
    this.resultNumber = resultNumber;
  }

  public void setResults(ArrayList<String> results) {
    this.results = results;
  }

  public void setTimeConsuming(long timeConsuming) {
    this.timeConsuming = timeConsuming;
  }
}
