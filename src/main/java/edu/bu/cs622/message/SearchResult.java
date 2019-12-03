package edu.bu.cs622.message;

import edu.bu.cs622.user.User;

import java.util.ArrayList;
import java.util.Objects;

public class SearchResult {
  private SearchType searchType;
  private int resultNumber;
  private String result;
  private long timeConsuming;

  public SearchResult(SearchType searchType) {
    this.searchType = searchType;
  }

  public SearchResult(SearchType searchType, int resultNumber, String results, long timeConsuming) {
    this.searchType = searchType;
    this.resultNumber = resultNumber;
    this.result = results;
    this.timeConsuming = timeConsuming;
  }

  public SearchType getSearchType() {
    return searchType;
  }

  public void setResultNumber(int resultNumber) {
    this.resultNumber = resultNumber;
  }

  public void setResults(String results) {
    this.result = results;
  }

  public void setTimeConsuming(long timeConsuming) {
    this.timeConsuming = timeConsuming;
  }

  public int getResultNumber() {
    return resultNumber;
  }

  public String getResults() {
    return result;
  }

  public long getTimeConsuming() {
    return timeConsuming;
  }

  @Override
  public int hashCode() {
    return Objects.hash(searchType,resultNumber,timeConsuming);
  }

  @Override
  public boolean equals(Object o) {
    if (o == this) return true;
    if(null == o || o.getClass() != this.getClass()) return false;

    SearchResult other = (SearchResult) o;

    return Objects.equals(this.searchType, other.searchType) && Objects.equals(this.resultNumber, other.resultNumber)
        &&Objects.equals(this.timeConsuming,other.timeConsuming);
  }
}
