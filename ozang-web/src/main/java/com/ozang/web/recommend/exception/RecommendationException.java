package com.ozang.web.recommend.exception;

import com.fourthread.ozang.module.common.exception.ErrorDetails;
import com.fourthread.ozang.module.common.exception.GlobalException;

public class RecommendationException extends GlobalException  {

  public RecommendationException(String exceptionName, String message,
      ErrorDetails details) {
    super(exceptionName, message, details);
  }
}
