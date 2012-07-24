// Copyright 2012 Google Inc. All Rights Reserved.

package com.google.common.labs.fsm;

class FsmException extends Exception {
  public FsmException(Exception e) {
    super(e);
  }

  public FsmException(String message) {
    super(message);
  }
}
