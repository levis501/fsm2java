// Copyright 2012 Google Inc. All Rights Reserved.

package com.google.common.labs.fsm;

import com.google.common.io.NullOutputStream;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintStream;

/**
 * static utilities for Fsm testing
 */
public class FsmTestUtil {
  /**
   * Utility function for debugging. Executes a command and pipes the result to
   * System.err
   *
   * @param command
   */
  static void exec(String command) {
    try {
      Process p = Runtime.getRuntime().exec(command);
      BufferedReader stdInput = new BufferedReader(new InputStreamReader(p.getInputStream()));
      BufferedReader stdError = new BufferedReader(new InputStreamReader(p.getErrorStream()));

      // read the output from the command
      while ((command = stdInput.readLine()) != null) {
        System.err.println(command);
      }

      // read any errors from the attempted command
      while ((command = stdError.readLine()) != null) {
        System.err.println(command);
      }
    } catch (IOException e) {
      System.err.println("exception happened - here's what I know: ");
      e.printStackTrace();
    }
  }

  static void redirectToNull() {
    System.setOut(new PrintStream(new NullOutputStream()));
  }

  static ByteArrayOutputStream redirectToByteStream() {
    ByteArrayOutputStream byteStream = new ByteArrayOutputStream();
    System.setOut(new PrintStream(byteStream));
    return byteStream;
  }

  static File redirectToTemporaryFile(String className) throws IOException {
    File fileStream = File.createTempFile("FsmTestUtil", null);
    fileStream.deleteOnExit();

    String tmpDir = fileStream.getPath() + ".d/";
    File tmpDirFile = new File(tmpDir);
    tmpDirFile.mkdir();
    tmpDirFile.deleteOnExit();

    fileStream = new File(tmpDir + className + ".java");
    fileStream.createNewFile();
    System.setOut(new PrintStream(fileStream));
    fileStream.deleteOnExit();
    return fileStream;
  }

  static byte[] readFileBytes(String filename) throws IOException {
    File file = new File(filename);
    InputStream is = new FileInputStream(file);
    byte[] result = new byte[(int) file.length()];
    int count = 0;
    while (count < result.length) {
      int r = is.read(result, count, result.length - count);
      if (r < 0) {
        break;
      }
      count += r;
    }
    is.close();
    return result;
  }

  static boolean isExceptionCausedBy(FsmException exception, Class<?> causeClass) {
    Throwable cause = exception;
    while (cause != null) {
      if (causeClass.isInstance(cause)) {
        return true;
      }
      cause = cause.getCause();
    }
    return false;
  }

}
