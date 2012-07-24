// Copyright 2012 Google Inc. All Rights Reserved.

package com.google.common.labs.fsm;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import com.google.common.labs.fsm.ScxmlDoc.ParseException;
import com.google.common.labs.fsm.Source.SourceException;
import com.google.testing.util.TestUtil;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.tools.JavaCompiler;
import javax.tools.ToolProvider;

/**
 * Tests form the Scxml2Java class
 *
 */
@RunWith(JUnit4.class)
public class Scxml2JavaTest {

  private static final String TESTDATA_DIR =
      TestUtil.getSrcDir() + "/google3/javatests/com/google/common/labs/fsm/testdata/";
  private static final String TINY_SCXML_FILE = TESTDATA_DIR + "tiny.xml";
  private static final String SMALL_SCXML_FILE = TESTDATA_DIR + "small.xml";
  private static final String LARGE_SCXML_FILE = TESTDATA_DIR + "code_entry.xml";
  private static final String MEDIUM_SCXML_FILE = TESTDATA_DIR + "medium.xml";
  private static final String DEBUG_SCXML_FILE = TESTDATA_DIR + "debug.xml";

  @Test
  public void outputIsNotNull() throws IOException, ParseException {
    ByteArrayOutputStream output = FsmTestUtil.redirectToByteStream();
    Scxml2Java translator = Scxml2Java.translatorForScxml(ScxmlDoc.createFromFile(TINY_SCXML_FILE));
    translator.outputJava();
    byte[] bytes = output.toByteArray();
    assertTrue(bytes.length > 0);
  }

  @Test
  public void findsClassName() throws IOException, ParseException {
    FsmTestUtil.redirectToNull();
    Scxml2Java translator = Scxml2Java.translatorForScxml(ScxmlDoc.createFromFile(TINY_SCXML_FILE));
    assertEquals("tiny", translator.getClassName());
  }

  @Test
  public void canChangeClassName() throws IOException, ParseException {
    FsmTestUtil.redirectToNull();
    Scxml2Java translator = Scxml2Java.translatorForScxml(ScxmlDoc.createFromFile(TINY_SCXML_FILE));
    String newClassName = "SomeNewClass";
    translator.changeClassName(newClassName);
    assertEquals(newClassName, translator.getClassName());
  }

  @Test
  public void outputCompiles() throws SourceException {
    assertEquals(0, Source.fromScxmlFile(TINY_SCXML_FILE).mCompilerResult);
  }

  @Test
  public void canLoadCompiledClass() throws SourceException {
    assertNotNull((Source.fromScxmlFile(TINY_SCXML_FILE)).mClass);
  }

  @Test
  public void classHasCorrectName() throws SourceException {
    Source s = Source.fromScxmlFile(TINY_SCXML_FILE);
    assertEquals(s.mClassName, s.mClass.getCanonicalName());
  }

  @Test
  public void classHasNoPackage() throws SourceException {
    Source s = Source.fromScxmlFile(TINY_SCXML_FILE);
    Package pkg = s.mClass.getPackage();
    assertNull(pkg);
    assertNull(s.mTranslator.getPackageName());
  }

  @Test
  public void classHasCorrectPackage() throws SourceException {
    Source s = Source.fromScxmlFile(LARGE_SCXML_FILE);
    Package pkg = s.mClass.getPackage();
    assertEquals(s.mTranslator.getPackageName(), pkg.getName());
  }

  @Test
  public void classHasEventEnums() throws SourceException {
    Source s = Source.fromScxmlFile(SMALL_SCXML_FILE);
    Class<?> eventEnum = s.getNamedEnum("Event");
    assertNotNull(eventEnum);
  }

  @Test
  public void classHasCorrectEvents() throws SourceException {
    Source s = Source.fromScxmlFile(LARGE_SCXML_FILE);
    Class<?> eventEnum = s.getNamedEnum("Event");
    Set<String> expected = s.mTranslator.getDoc().getEventSet();
    for (Object o : eventEnum.getEnumConstants()) {
      assertTrue(expected.contains(o.toString()));
      expected.remove(o.toString());
    }
    assertTrue(expected.isEmpty());
  }

  @Test
  public void classHasStateEnums() throws SourceException {
    Source s = Source.fromScxmlFile(SMALL_SCXML_FILE);
    Class<?> stateEnum = s.getNamedEnum("State");
    assertNotNull(stateEnum);
  }

  @Test
  public void classHasCorrectStates() throws SourceException {
    Source s = Source.fromScxmlFile(LARGE_SCXML_FILE);
    Class<?> stateEnum = s.getNamedEnum("State");
    List<String> expected = s.mTranslator.getDoc().mDeclaredStateNames;
    for (Object o : stateEnum.getEnumConstants()) {
      assertTrue(expected.contains(o.toString()));
      expected.remove(o.toString());
    }
    assertTrue(expected.isEmpty());
  }

  @Test
  public void classHasDebugLoggerInterface() throws SourceException {
    Source s = Source.fromScxmlFile(LARGE_SCXML_FILE);
    for (Class<?> innerCls : s.mClass.getClasses()) {
      if (innerCls.isInterface()
          && innerCls.getCanonicalName().equals(s.mFullClassName + ".DebugLogger")) {
        return;
      }
    }
    fail();
  }

  @Test
  public void classHasCorrectFields() throws SourceException {
    Source s = Source.fromScxmlFile(LARGE_SCXML_FILE);
    Set<String> expected =
        new HashSet<String>(Arrays.asList("mCurrentState", "mPushingEvent", "mDebugLogger"));
    for (Field field : s.mClass.getDeclaredFields()) {
      assertTrue(expected.contains(field.getName()));
      expected.remove(field.getName());
    }
    assertTrue(expected.isEmpty());
  }

  @Test
  public void classHasConstructor()
      throws SecurityException, NoSuchMethodException, SourceException {
    Source s = Source.fromScxmlFile(LARGE_SCXML_FILE);
    Constructor<?> c = s.mClass.getConstructor();
    assertEquals(s.mFullClassName, c.getName());
  }

  @Test
  public void classHasConcreteMethods() throws SourceException {
    Source s = Source.fromScxmlFile(LARGE_SCXML_FILE);
    List<String> expectedConcreteMethods = new ArrayList<String>(Arrays.asList(
        "transitionToState", "isInTerminalState", "setDebugLogger", "pushEvent", "start", "start",
        "getCurrentState", "handleBaseEvent"));
    for (Method method : s.mClass.getDeclaredMethods()) {
      if (!Modifier.isAbstract(method.getModifiers())) {
        assertTrue(expectedConcreteMethods.contains(method.getName()));
        expectedConcreteMethods.remove(method.getName());
      }
    }
    if (!expectedConcreteMethods.isEmpty()) {
      for (String remaining : expectedConcreteMethods) {
        System.err.print(remaining + "\n");
      }
    }
    assertTrue(expectedConcreteMethods.isEmpty());
  }

  @Test
  public void classHasAbstractMethods() throws SourceException {
    Source s = Source.fromScxmlFile(LARGE_SCXML_FILE);
    List<String> expected = new ArrayList<String>();
    expected.add("onStateChange");
    for (String action : s.mTranslator.getDoc().getActionSet()) {
      expected.add("onAction" + action);
    }
    for (Method method : s.mClass.getDeclaredMethods()) {
      if (Modifier.isAbstract(method.getModifiers())) {
        assertTrue(expected.contains(method.getName()));
        expected.remove(method.getName());
      }
    }
    if (!expected.isEmpty()) {
      for (String remaining : expected) {
        System.err.print(remaining + "\n");
      }
    }
    assertTrue(expected.isEmpty());
  }

  @Test
  public void canInstantiateClass() throws FsmException, SourceException {
    FsmMock fsm = new FsmMock(TINY_SCXML_FILE);
  }

  @Test
  public void canStartFsm() throws FsmException, SourceException {
    FsmMock fsm = new FsmMock(TINY_SCXML_FILE);
    fsm.start();
  }

  @Test
  public void fsmStartsInFirstStateWithNoInitialStateDeclaration()
      throws FsmException, SourceException {
    FsmMock fsm = new FsmMock(TINY_SCXML_FILE);
    fsm.start();
    assertEquals("StateOne", fsm.getCurrentStateName());
  }

  @Test
  public void fsmStartsInDeclaredInitialState() throws FsmException, SourceException {
    FsmMock fsm = new FsmMock(LARGE_SCXML_FILE);
    fsm.start();
    assertEquals("Ready", fsm.getCurrentStateName());
  }

  @Test
  public void fsmTransitionsFromInitialState() throws FsmException, SourceException {
    FsmMock fsm = new FsmMock(TINY_SCXML_FILE);
    fsm.start();
    fsm.pushEvent("EventA");
    assertEquals("StateTwo", fsm.getCurrentStateName());
  }

  @Test
  public void fsmInitialStateHandlesBaseTransitions() throws FsmException, SourceException {
    FsmMock fsm = new FsmMock(LARGE_SCXML_FILE);
    fsm.start();
    fsm.pushEvent("ResetPressed");
    assertEquals("Ready", fsm.getCurrentStateName());
  }

  @Test
  public void fsmSecondStateHandlesBaseTransitions() throws FsmException, SourceException {
    FsmMock fsm = new FsmMock(LARGE_SCXML_FILE);
    fsm.start();
    fsm.pushEvent("DigitPressed");
    fsm.pushEvent("ResetPressed");
    assertEquals("Ready", fsm.getCurrentStateName());
  }

  @Test
  public void fsmThrowOnUnexpectedEventWithoutBaseState() throws FsmException, SourceException {
    FsmMock fsm = new FsmMock(SMALL_SCXML_FILE);
    fsm.start();
    try {
      fsm.pushEvent("EventB");
      // Exception expected
      fail();
    } catch (FsmException e) {
      assertTrue(FsmTestUtil.isExceptionCausedBy(e, fsm.mStateException));
    }
  }

  @Test
  public void fsmThrowOnUnexpectedEventWithBaseState() throws FsmException, SourceException {
    FsmMock fsm = new FsmMock(LARGE_SCXML_FILE);
    fsm.start();
    try {
      fsm.pushEvent("EntryValid");
      // Exception expected
      fail();
    } catch (FsmException e) {
      assertTrue(FsmTestUtil.isExceptionCausedBy(e, fsm.mStateException));
    }
  }

  @Test
  public void fsmFindsTerminalState() throws FsmException, SourceException {
    FsmMock fsm = new FsmMock(MEDIUM_SCXML_FILE);
    fsm.start();
    assertFalse(fsm.isInTerminalState());
    fsm.pushEvent("AlarmRings");
    assertFalse(fsm.isInTerminalState());
    fsm.pushEvent("GetUp");
    assertTrue(fsm.isInTerminalState());
  }

  @Test
  public void fsmHasNoTerminalStates() throws FsmException, SourceException {
    FsmMock fsm = new FsmMock(SMALL_SCXML_FILE);
    fsm.start();
    assertFalse(fsm.isInTerminalState());
    fsm.pushEvent("EventA");
    assertFalse(fsm.isInTerminalState());
    fsm.pushEvent("EventB");
    assertFalse(fsm.isInTerminalState());
  }

  @Test
  public void fsmCantStartTwice() throws FsmException, SourceException {
    FsmMock fsm = new FsmMock(TINY_SCXML_FILE);
    fsm.start();
    try {
      fsm.start();
      // Exception expected
      fail();
    } catch (FsmException e) {
      assertTrue(FsmTestUtil.isExceptionCausedBy(e, fsm.mStateException));
    }
  }

  @Test
  public void onStateChangeCalledAtStart() throws FsmException, SourceException {
    FsmMock fsm = new FsmMock(TINY_SCXML_FILE);
    fsm.start();
    String recordedState = fsm.getRecordedStateChangeName();
    assertEquals("StateOne", recordedState);
  }

  @Test
  public void onStateChangeCalledAfterEvent() throws FsmException, SourceException {
    FsmMock fsm = new FsmMock(TINY_SCXML_FILE);
    fsm.start();
    fsm.pushEvent("EventA");
    String recordedState = fsm.getRecordedStateChangeName();
    assertEquals("StateTwo", recordedState);
  }

  @Test
  public void noActionPerformedBeforeEvents() throws FsmException, SourceException {
    FsmMock fsm = new FsmMock(MEDIUM_SCXML_FILE);
    fsm.start();
    assertEquals(0, fsm.getTotalActionCount());
  }

  @Test
  public void actionPerformedAfterFirstStateChange() throws FsmException, SourceException {
    FsmMock fsm = new FsmMock(MEDIUM_SCXML_FILE);
    fsm.start();
    fsm.pushEvent("Dog");
    assertEquals(1, fsm.getActionCount("Grunt"));
    assertEquals(1, fsm.getActionCount("Kick"));
    assertEquals(2, fsm.getTotalActionCount());
  }

  @Test
  public void actionPerformedAfterSecondStateChange() throws FsmException, SourceException {
    FsmMock fsm = new FsmMock(MEDIUM_SCXML_FILE);
    fsm.start();
    fsm.pushEvent("Dog");
    assertEquals(0, fsm.getActionCount("Scream"));
    fsm.pushEvent("Nightmare");
    assertEquals(1, fsm.getActionCount("Scream"));
    assertEquals(3, fsm.getTotalActionCount());
  }

  @Test
  public void throwOnPushEventDuringAction() throws FsmException, SourceException {
    final FsmMock fsm = new FsmMock(MEDIUM_SCXML_FILE);
    fsm.mTestActionListener = new FsmMock.ActionListener() {
      @Override
      public void onAction(String actionName) throws FsmException {
        fsm.pushEvent("HitSnooze");
      }
    };
    fsm.start();
    boolean expectedExceptionFound = false;
    try {
      fsm.pushEvent("AlarmRings");
    } catch (FsmException e) {
      // drill down to get the root cause
      Throwable cause = e.getCause();
      while (cause != null) {
        if (fsm.mStateException.isInstance(cause)) {
          expectedExceptionFound = true;
          break;
        }
        cause = cause.getCause();
      }
    }
    assertTrue(expectedExceptionFound);
  }

  @Test
  public void debugLoggingOnEvent() throws FsmException, SourceException {
    FsmMock fsm = new FsmMock(DEBUG_SCXML_FILE);
    fsm.setupDebugLogging();
    fsm.start();
    fsm.pushEvent("EventA");
    assertEquals("EventA", fsm.getLastEventMsg());
  }

  @Test
  public void debugLoggingOnAction() throws FsmException, SourceException {
    FsmMock fsm = new FsmMock(DEBUG_SCXML_FILE);
    fsm.setupDebugLogging();
    fsm.start();
    fsm.pushEvent("EventA");
    assertEquals("GoingToTwo", fsm.getLastActionMsg());
  }

  @Test
  public void debugLoggingOnState() throws FsmException, SourceException {
    FsmMock fsm = new FsmMock(DEBUG_SCXML_FILE);
    fsm.setupDebugLogging();
    fsm.start();
    fsm.pushEvent("EventA");
    assertEquals("StateTwo", fsm.getLastStateMsg());
  }

  @Test
  public void mainPerformsTranslation() throws IOException, ParseException {
    File javaSrcFile = FsmTestUtil.redirectToTemporaryFile("tiny");
    String[] args = new String[1];
    args[0] = TINY_SCXML_FILE;
    Scxml2Java.main(args);
    JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
    int compilerResult = compiler.run(null, System.err, System.err, javaSrcFile.getAbsolutePath());
    assertEquals(0, compilerResult);
  }
}
