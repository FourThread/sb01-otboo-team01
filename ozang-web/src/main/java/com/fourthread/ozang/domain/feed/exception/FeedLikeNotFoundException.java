package com.fourthread.ozang.domain.feed.exception;

import com.fourthread.ozang.common.exception.ErrorDetails;
import com.fourthread.ozang.common.exception.GlobalException;

public class FeedLikeNotFoundException extends GlobalException {

  public FeedLikeNotFoundException(String exceptionName, String message, ErrorDetails details) {
    super(exceptionName, message, details);
  }
}
