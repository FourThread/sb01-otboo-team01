package com.fourthread.ozang.app.domain.feed.exception;

import com.fourthread.ozang.app.common.exception.ErrorDetails;
import com.fourthread.ozang.app.common.exception.GlobalException;

public class FeedLikeNotFoundException extends GlobalException {

  public FeedLikeNotFoundException(String exceptionName, String message, ErrorDetails details) {
    super(exceptionName, message, details);
  }
}
