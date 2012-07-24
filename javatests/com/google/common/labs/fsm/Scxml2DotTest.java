// Copyright 2012 Google Inc. All Rights Reserved.

package com.google.common.labs.fsm;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertTrue;

import com.google.common.labs.fsm.ScxmlDoc.ParseException;
import com.google.testing.util.TestUtil;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

/**
 * The Scxml2DotTest class provides unit tests of the functionality of the
 * {@link Scxml2Dot} graph file writer for {@link ScxmlDoc} document objects
 */
@RunWith(JUnit4.class)
public class Scxml2DotTest {

  private static final String TESTDATA_DIR =
      TestUtil.getSrcDir() + "/google3/javatests/com/google/common/labs/fsm/testdata/";
  private static final String TINY_SCXML_FILE = TESTDATA_DIR + "tiny.xml";
  private static final String TINY_DOT_FILE = TESTDATA_DIR + "tiny.dot";

  private static final String SMALL_SCXML_FILE = TESTDATA_DIR + "small.xml";
  private static final String SMALL_DOT_FILE = TESTDATA_DIR + "small.dot";
  private static final String MEDIUM_SCXML_FILE = TESTDATA_DIR + "medium.xml";
  private static final String MEDIUM_DOT_FILE = TESTDATA_DIR + "medium.dot";
  private static final String LARGE_SCXML_FILE = TESTDATA_DIR + "large.xml";
  private static final String LARGE_DOT_FILE = TESTDATA_DIR + "large.dot";

  @Test
  public void outputIsNotNull() throws IOException, ParseException {
    ByteArrayOutputStream output = FsmTestUtil.redirectToByteStream();
    Scxml2Dot translator = Scxml2Dot.translatorForScxml(ScxmlDoc.createFromFile(TINY_SCXML_FILE));
    translator.outputDot();
    byte[] bytes = output.toByteArray();
    assertTrue(bytes.length > 0);
  }

  @Test
  public void tinyDotFileMatches() throws IOException, ParseException {
    ByteArrayOutputStream output = FsmTestUtil.redirectToByteStream();
    Scxml2Dot translator = Scxml2Dot.translatorForScxml(ScxmlDoc.createFromFile(TINY_SCXML_FILE));
    translator.outputDot();
    byte[] bytes = output.toByteArray();
    String s = new String(bytes);
    assertArrayEquals(bytes, FsmTestUtil.readFileBytes(TINY_DOT_FILE));
  }

  @Test
  public void smallDotFileMatches() throws IOException, ParseException {
    ByteArrayOutputStream output = FsmTestUtil.redirectToByteStream();
    Scxml2Dot translator = Scxml2Dot.translatorForScxml(ScxmlDoc.createFromFile(SMALL_SCXML_FILE));
    translator.outputDot();
    byte[] bytes = output.toByteArray();
    String s = new String(bytes);
    assertArrayEquals(bytes, FsmTestUtil.readFileBytes(SMALL_DOT_FILE));
  }

  @Test
  public void mediumDotFileMatches() throws IOException, ParseException {
    ByteArrayOutputStream output = FsmTestUtil.redirectToByteStream();
    Scxml2Dot translator = Scxml2Dot.translatorForScxml(ScxmlDoc.createFromFile(MEDIUM_SCXML_FILE));
    translator.outputDot();
    byte[] bytes = output.toByteArray();
    String s = new String(bytes);
    assertArrayEquals(bytes, FsmTestUtil.readFileBytes(MEDIUM_DOT_FILE));
  }

  @Test
  public void largeDotFileMatches() throws IOException, ParseException {
    ByteArrayOutputStream output = FsmTestUtil.redirectToByteStream();
    Scxml2Dot translator = Scxml2Dot.translatorForScxml(ScxmlDoc.createFromFile(LARGE_SCXML_FILE));
    translator.outputDot();
    byte[] bytes = output.toByteArray();
    String s = new String(bytes);
    assertArrayEquals(bytes, FsmTestUtil.readFileBytes(LARGE_DOT_FILE));
  }

  @Test
  public void mainPerformsTranslation() throws IOException, ParseException {
    ByteArrayOutputStream output = FsmTestUtil.redirectToByteStream();
    String[] args = new String[1];
    args[0] = TINY_SCXML_FILE;
    Scxml2Dot.main(args);
    byte[] bytes = output.toByteArray();
    String s = new String(bytes);
    assertArrayEquals(bytes, FsmTestUtil.readFileBytes(TINY_DOT_FILE));
  }

}
