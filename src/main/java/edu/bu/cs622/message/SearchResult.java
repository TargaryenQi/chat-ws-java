package edu.bu.cs622.message;

import edu.bu.cs622.user.User;

import java.util.ArrayList;
import java.util.Objects;

public class SearchResult {
  private SearchType searchType;
  private String result;
  private long timeConsuming;

  public SearchResult(SearchType searchType) {
    this.searchType = searchType;
  }

  public SearchResult(SearchType searchType, String result, long timeConsuming) {
    this.searchType = searchType;
    this.result = result;
    this.timeConsuming = timeConsuming;
  }

  public SearchType getSearchType() {
    return searchType;
  }

  public void setResult(String result) {
    this.result = result;
  }

  public void setTimeConsuming(long timeConsuming) {
    this.timeConsuming = timeConsuming;
  }


  public String getResult() {
    return result;
  }

  public long getTimeConsuming() {
    return timeConsuming;
  }

  @Override
  public int hashCode() {
    return Objects.hash(searchType,timeConsuming,result);
  }

  @Override
  public boolean equals(Object o) {
    if (o == this) return true;
    if(null == o || o.getClass() != this.getClass()) return false;

    SearchResult other = (SearchResult) o;

    return Objects.equals(this.searchType, other.searchType) && Objects.equals(this.result, other.result)
        &&Objects.equals(this.timeConsuming,other.timeConsuming);
  }
}
