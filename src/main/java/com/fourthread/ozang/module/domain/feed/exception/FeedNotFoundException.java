package com.fourthread.ozang.module.domain.feed.exception;

public class FeedNotFoundException extends RuntimeException {

  public FeedNotFoundException(String message) {
    super(message);
  }

  public FeedNotFoundException() {
    super();
  }
}
