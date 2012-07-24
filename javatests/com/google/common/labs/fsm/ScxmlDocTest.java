// Copyright 2012 Google Inc. All Rights Reserved.

package com.google.common.labs.fsm;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import com.google.common.io.NullOutputStream;
import com.google.common.labs.fsm.ScxmlDoc.ParseException;
import com.google.common.labs.fsm.ScxmlDoc.State;
import com.google.common.labs.fsm.ScxmlDoc.Transition;
import com.google.testing.util.TestUtil;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import java.io.IOException;
import java.io.PrintStream;
import java.util.List;
import java.util.Set;

/**
 * Tests for Scxml document class
 *
 */

@RunWith(JUnit4.class)
public class ScxmlDocTest {

  private static final String TESTDATA_DIR =
      TestUtil.getSrcDir() + "/google3/javatests/com/google/common/labs/fsm/testdata/";
  private static final String TINY_SCXML_FILE = TESTDATA_DIR + "tiny.xml";
  private static final String NO_STATES_SCXML_FILE = TESTDATA_DIR + "no_states.xml";
  private static final String BAD_DOC_TYPE_FILE = TESTDATA_DIR + "bad_doc_type.xml";
  private static final String BAD_ELEMENT_NAME_FILE = TESTDATA_DIR + "bad_element_name.xml";
  private static final String BAD_ATTRIBUTE_NAME_FILE = TESTDATA_DIR + "bad_attribute_name.xml";
  private static final String BAD_XML_FILE = TESTDATA_DIR + "bad_xml.xml";
  private static final String BAD_DOC_ATTRIBUTE_NAME_FILE =
      TESTDATA_DIR + "bad_doc_attribute_name.xml";
  private static final String NO_SUCH_FILE = TESTDATA_DIR + "nosuchfile";
  private static final String MISSING_NAME_FILE = TESTDATA_DIR + "missing_name.xml";
  private static final String BAD_NAME_FILE = TESTDATA_DIR + "bad_name.xml";
  private static final String BAD_INITIAL_STATE_FILE = TESTDATA_DIR + "bad_initial_state.xml";
  private static final String SMALL_SCXML_FILE = TESTDATA_DIR + "small.xml";
  private static final String DEBUG_FILE = TESTDATA_DIR + "debug.xml";
  private static final String DEBUG_FALSE_FILE = TESTDATA_DIR + "debug_false.xml";
  private static final String NO_STATE_ID_FILE = TESTDATA_DIR + "no_state_id.xml";
  private static final String NO_ACTION_ID_FILE = TESTDATA_DIR + "no_action_id.xml";
  private static final String LARGE_SCXML_FILE = TESTDATA_DIR + "large.xml";
  private static final String BAD_BASE_FILE = TESTDATA_DIR + "bad_base.xml";
  private static final String MISSING_EVENT_FILE = TESTDATA_DIR + "missing_event.xml";
  private static final String MISSING_ACTION_ID_FILE = TESTDATA_DIR + "missing_action_id.xml";
  private static final String MISSING_TRANSITION_TARGET_FILE =
      TESTDATA_DIR + "missing_transition_target.xml";
  private static final String UNDECLARED_TARGET_FILE = TESTDATA_DIR + "undeclared_target.xml";

  private State getStateByName(List<State> states, String name) {
    for (State s : states) {
      if (s.getId().equals(name)) {
        return s;
      }
    }
    return null;
  }

  private Transition getTransitionByEvent(List<Transition> transitions, String event) {
    for (Transition t : transitions) {
      if (t.getEvent().equals(event)) {
        return t;
      }
    }
    return null;
  }

  @Test(expected = IOException.class)
  public void throwsOnNoSuchFile() throws IOException, ParseException {
    ScxmlDoc.createFromFile(NO_SUCH_FILE);
  }

  @Test
  public void canParseScxml() throws IOException, ParseException {
    ScxmlDoc.createFromFile(TINY_SCXML_FILE);
  }

  @Test(expected = ParseException.class)
  public void throwsOnBadDocType() throws IOException, ParseException {
    ScxmlDoc.createFromFile(BAD_DOC_TYPE_FILE);
  }

  @Test(expected = ParseException.class)
  public void throwsOnBadElementName() throws IOException, ParseException {
    ScxmlDoc.createFromFile(BAD_ELEMENT_NAME_FILE);
  }

  @Test(expected = ParseException.class)
  public void throwsOnBadDocAttributeName() throws IOException, ParseException {
    ScxmlDoc.createFromFile(BAD_DOC_ATTRIBUTE_NAME_FILE);
  }

  @Test(expected = ParseException.class)
  public void throwsOnMissingName() throws IOException, ParseException {
    ScxmlDoc.createFromFile(MISSING_NAME_FILE);
  }

  @Test(expected = ParseException.class)
  public void throwsOnBadName() throws IOException, ParseException {
    ScxmlDoc.createFromFile(BAD_NAME_FILE);
  }

  @Test
  public void separatesPackageAndClassNames() throws IOException, ParseException {
    ScxmlDoc doc = ScxmlDoc.createFromFile(SMALL_SCXML_FILE);
    assertEquals("Small", doc.getClassName());
    assertEquals("dummy.pkg", doc.getPackageName());
  }

  @Test
  public void parsesNullPackageName() throws IOException, ParseException {
    ScxmlDoc doc = ScxmlDoc.createFromFile(TINY_SCXML_FILE);
    assertEquals("tiny", doc.getClassName());
    assertNull(doc.getPackageName());
  }

  @Test(expected = ParseException.class)
  public void throwsOnNoStates() throws IOException, ParseException {
    ScxmlDoc doc = ScxmlDoc.createFromFile(NO_STATES_SCXML_FILE);
  }

  @Test
  public void parsesStates() throws IOException, ParseException {
    ScxmlDoc doc = ScxmlDoc.createFromFile(TINY_SCXML_FILE);
    List<State> states = doc.getStates();
    assertNotNull(getStateByName(states, "StateOne"));
    assertNotNull(getStateByName(states, "StateTwo"));
  }

  @Test
  public void initialStateInferred() throws IOException, ParseException {
    ScxmlDoc doc = ScxmlDoc.createFromFile(TINY_SCXML_FILE);
    assertEquals("StateOne", doc.getInitialStateName());
  }

  @Test
  public void initialStateParsed() throws IOException, ParseException {
    ScxmlDoc doc = ScxmlDoc.createFromFile(LARGE_SCXML_FILE);
    assertEquals("Sleeping", doc.getInitialStateName());
  }

  @Test(expected = ParseException.class)
  public void throwsOnBadInitialStateName() throws IOException, ParseException {
    ScxmlDoc doc = ScxmlDoc.createFromFile(BAD_INITIAL_STATE_FILE);
  }

  @Test
  public void setsDebugModeTrue() throws IOException, ParseException {
    ScxmlDoc doc = ScxmlDoc.createFromFile(DEBUG_FILE);
    assertTrue(doc.getDebugFlag());
  }

  @Test
  public void setsDebugModeFalse() throws IOException, ParseException {
    ScxmlDoc doc = ScxmlDoc.createFromFile(DEBUG_FALSE_FILE);
    assertFalse(doc.getDebugFlag());
  }

  @Test
  public void setsDebugModeDefault() throws IOException, ParseException {
    ScxmlDoc doc = ScxmlDoc.createFromFile(TINY_SCXML_FILE);
    assertFalse(doc.getDebugFlag());
  }

  @Test
  public void terminalStatesParsed() throws IOException, ParseException {
    ScxmlDoc doc = ScxmlDoc.createFromFile(TINY_SCXML_FILE);
    assertEquals(1, doc.getTerminalStates().size());
  }

  @Test
  public void noTerminalStatesParsed() throws IOException, ParseException {
    ScxmlDoc doc = ScxmlDoc.createFromFile(SMALL_SCXML_FILE);
    assertEquals(0, doc.getTerminalStates().size());
  }

  @Test(expected = ParseException.class)
  public void statesMustHaveId() throws IOException, ParseException {
    ScxmlDoc doc = ScxmlDoc.createFromFile(NO_STATE_ID_FILE);
  }

  @Test(expected = ParseException.class)
  public void actionsMustHaveId() throws IOException, ParseException {
    ScxmlDoc doc = ScxmlDoc.createFromFile(NO_ACTION_ID_FILE);
  }

  @Test
  public void baseStateParsed() throws IOException, ParseException {
    ScxmlDoc doc = ScxmlDoc.createFromFile(LARGE_SCXML_FILE);
    State baseState = doc.getBaseState();
    assertEquals("BaseState", baseState.getId());
  }

  @Test
  public void noBaseStateParsed() throws IOException, ParseException {
    ScxmlDoc doc = ScxmlDoc.createFromFile(TINY_SCXML_FILE);
    assertNull(doc.getBaseState());
  }

  @Test(expected = ParseException.class)
  public void throwsOnUndeclaredBaseState() throws IOException, ParseException {
    ScxmlDoc doc = ScxmlDoc.createFromFile(BAD_BASE_FILE);
  }

  @Test(expected = ParseException.class)
  public void throwsOnTransitionMissingEvent() throws IOException, ParseException {
    ScxmlDoc doc = ScxmlDoc.createFromFile(MISSING_EVENT_FILE);
  }

  @Test
  public void transitionsParsed() throws IOException, ParseException {
    ScxmlDoc doc = ScxmlDoc.createFromFile(LARGE_SCXML_FILE);
    State s = getStateByName(doc.getStates(), "Sleeping");
    List<Transition> transitions = s.getTransitions();
    assertNotNull(getTransitionByEvent(transitions, "AlarmRings"));
    assertNotNull(getTransitionByEvent(transitions, "Dog"));
    assertNotNull(getTransitionByEvent(transitions, "Nightmare"));
  }

  @Test
  public void actionAttributeParsed() throws IOException, ParseException {
    ScxmlDoc doc = ScxmlDoc.createFromFile(LARGE_SCXML_FILE);
    State s = getStateByName(doc.getStates(), "BaseState");
    Transition t = getTransitionByEvent(s.getTransitions(), "RuntimeFailure");
    List<String> actions = t.getActions();
    assertTrue(t.getActions().contains("WakeUp"));
  }

  @Test
  public void actionElementParsed() throws IOException, ParseException {
    ScxmlDoc doc = ScxmlDoc.createFromFile(LARGE_SCXML_FILE);
    State s = getStateByName(doc.getStates(), "Sleeping");
    Transition t = getTransitionByEvent(s.getTransitions(), "Dog");
    List<String> actions = t.getActions();
    assertTrue(t.getActions().contains("Grunt"));
    assertTrue(t.getActions().contains("Kick"));
  }

  @Test(expected = ParseException.class)
  public void throwsOnActionMissingId() throws IOException, ParseException {
    ScxmlDoc doc = ScxmlDoc.createFromFile(MISSING_ACTION_ID_FILE);
  }

  @Test
  public void transitionTargetParsed() throws IOException, ParseException {
    ScxmlDoc doc = ScxmlDoc.createFromFile(LARGE_SCXML_FILE);
    State s = getStateByName(doc.getStates(), "Sleeping");
    List<Transition> transitions = s.getTransitions();
    Transition t = getTransitionByEvent(transitions, "AlarmRings");
    assertEquals("AnnoyedState", t.getTarget());
  }

  @Test(expected = ParseException.class)
  public void throwsOnTransactionMissingTarget() throws IOException, ParseException {
    ScxmlDoc doc = ScxmlDoc.createFromFile(MISSING_TRANSITION_TARGET_FILE);
  }

  @Test(expected = ParseException.class)
  public void throwsOnTargetStatesNotDeclared() throws IOException, ParseException {
    ScxmlDoc doc = ScxmlDoc.createFromFile(UNDECLARED_TARGET_FILE);
  }

  @Test
  public void canGetEvents() throws IOException, ParseException {
    ScxmlDoc doc = ScxmlDoc.createFromFile(LARGE_SCXML_FILE);
    Set<String> events = doc.getEventSet();
    assertTrue(events.contains("RuntimeFailure"));
    assertTrue(events.contains("AlarmRings"));
    assertTrue(events.contains("Dog"));
    assertTrue(events.contains("Nightmare"));
    assertTrue(events.contains("HitSnooze"));
  }

  @Test
  public void canGetActions() throws IOException, ParseException {
    ScxmlDoc doc = ScxmlDoc.createFromFile(LARGE_SCXML_FILE);
    Set<String> actions = doc.getActionSet();
    assertTrue(actions.contains("WakeUp"));
    assertTrue(actions.contains("Grunt"));
    assertTrue(actions.contains("Kick"));
    assertTrue(actions.contains("Scream"));
  }

  @Test(expected = ParseException.class)
  public void throwsOnBadAttributeName() throws IOException, ParseException {
    ScxmlDoc doc = ScxmlDoc.createFromFile(BAD_ATTRIBUTE_NAME_FILE);
  }

  @Test(expected = ParseException.class)
  public void throwsOnBadXml() throws IOException, ParseException {
    System.setErr(new PrintStream(new NullOutputStream()));
    ScxmlDoc doc = ScxmlDoc.createFromFile(BAD_XML_FILE);
  }

  @Test
  public void canChangeClassName() throws IOException, ParseException {
    ScxmlDoc doc = ScxmlDoc.createFromFile(LARGE_SCXML_FILE);
    String newClassName = "LargeNew";
    doc.changeClassName(newClassName);
    assertEquals(newClassName, doc.getClassName());
  }

}
