package com.ozang.web.feed.exception;

import com.fourthread.ozang.module.common.exception.ErrorDetails;
import com.fourthread.ozang.module.common.exception.GlobalException;

public class FeedLikeNotFoundException extends GlobalException {

  public FeedLikeNotFoundException(String exceptionName, String message, ErrorDetails details) {
    super(exceptionName, message, details);
  }
}
