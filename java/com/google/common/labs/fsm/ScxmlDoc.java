// Copyright 2012 Google Inc. All Rights Reserved.

package com.google.common.labs.fsm;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

/**
 * The ScxmlDoc class encapsulates the parameters of a finite state machine as
 * described by an XML file. The XML is derived from SCXML, with changes.
 *
 *
 *
 */
public class ScxmlDoc {

  private Document dom;
  private String mClassName;
  private String mPackageName;
  private String mInitialStateName;
  private State mBaseState;
  private final List<State> mStates = new ArrayList<State>();
  protected final List<State> mTerminalStates = new ArrayList<State>();
  protected final Set<String> mGlobalActions = new HashSet<String>();
  protected final Set<String> mEvents = new HashSet<String>();
  protected final List<String> mDeclaredStateNames = new ArrayList<String>();
  protected final Set<String> mTargets = new HashSet<String>();
  protected boolean mDebug;
  private static final Set<String> VALID_ELEMENTS =
      new HashSet<String>(Arrays.asList("scxml", "state", "transition", "action"));
  private static final Set<String> VALID_ATTRIBUTES = new HashSet<String>(Arrays.asList(
      "xmlns", "version", "initial", "base", "debug", "name", "id", "event", "action", "target"));

  /**
   * Exception class that indicates an error in parsing the XML
   *
   */
  public class ParseException extends Exception {
    public ParseException(String message) {
      super(message);
    }
  }

  class Transition {
    final String mEvent;
    String mTarget;
    List<String> mActions = new ArrayList<String>();


    private void addActionFromElement(Element e) throws ParseException {
      String id = e.getAttribute("id");
      if (id.isEmpty()) {
        throw new ParseException("All actions must have an id attribute");
      }
      mActions.add(id);
    }

    /**
     * @param e the element declaring the transition
     * @throws ParseException
     */
    public Transition(Element e) throws ParseException {

      mEvent = e.getAttribute("event");
      if (mEvent.isEmpty()) {
        throw new ParseException("All transitions must be triggered by an event");
      }

      mEvents.add(mEvent);
      mTarget = e.getAttribute("target");
      if (mTarget.isEmpty()) {
        throw new ParseException("All transitions must have a target");
      }
      mTargets.add(mTarget);

      String action = e.getAttribute("action");
      if (!action.isEmpty()) {
        mActions.add(action);
      }
      List<Element> actionElements = getElements(e.getElementsByTagName("action"));
      if (actionElements != null) {
        for (Element ae : actionElements) {
          addActionFromElement(ae);
        }
      }
      mGlobalActions.addAll(mActions);
    }

    public String getEvent() {
      return mEvent;
    }

    public List<String> getActions() {
      return mActions;
    }

    public String getTarget() {
      return mTarget;
    }

  }

  /**
   * The State class parses and stores information related to a state declared in the scxml file
   */
  protected class State {
    final String mId;
    final List<Transition> mTransitions = new ArrayList<Transition>();

    /**
     * @param el the scxml element for this state
     * @throws ParseException
     */
    public State(Element el) throws ParseException {
      mId = el.getAttribute("id");
      if (mId.isEmpty()) {
        throw new ParseException("Every state must have an id attribute");
      }
      mDeclaredStateNames.add(mId);
      List<Element> elements = getElements(el.getElementsByTagName("transition"));
      if (elements != null) {
        for (Element e : elements) {
          mTransitions.add(new Transition(e));
        }
      }
    }

    /**
     * @return the id (name) of the state
     */
    public String getId() {
      return mId;
    }

    public List<Transition> getTransitions() {
      return mTransitions;
    }
  }

  private ScxmlDoc() {}

  public static ScxmlDoc createFromFile(String filename) throws ParseException, IOException {
    ScxmlDoc doc = new ScxmlDoc();
    doc.parseXmlFile(filename);
    doc.parseDocument();
    return doc;
  }


  public String getInitialStateName() {
    return mInitialStateName;
  }

  private void parseXmlFile(String filename) throws IOException, ParseException {
    // get the factory
    DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();

    try {
      // Using factory get an instance of document builder
      DocumentBuilder db = dbf.newDocumentBuilder();

      // parse using builder to get DOM representation of the XML file
      dom = db.parse(filename);
    } catch (ParserConfigurationException pce) {
      pce.printStackTrace();
      throw new ParseException("ParserConfigurationException " + pce.getMessage());
    } catch (SAXException se) {
      se.printStackTrace();
      throw new ParseException("SAXException " + se.getMessage());
    }
  }

  private List<Element> getElements(NodeList nl) throws ParseException {
    if ((nl == null) || (nl.getLength() == 0)) {
      return null;
    }
    List<Element> result = new ArrayList<Element>();

    for (int i = 0; i < nl.getLength(); i++) {
      Element e = (Element) nl.item(i);
      validateElement(e);
      result.add(e);
    }
    return result;
  }

  private void validateAttributes(Element e) throws ParseException {
    NamedNodeMap nm = e.getAttributes();
    for (int i = 0; i < nm.getLength(); i++) {
      Node n = nm.item(i);
      if (!VALID_ATTRIBUTES.contains(n.getNodeName())) {
        throw new ParseException("attribute " + n.getNodeName() + " is not accepted");
      }
    }
  }

  private void validateChildElements(Element e) throws ParseException {
    NodeList nl = e.getChildNodes();
    for (int i = 0; i < nl.getLength(); i++) {
      Node n = nl.item(i);
      if (n instanceof Element) {
        if (!VALID_ELEMENTS.contains(n.getNodeName())) {
          throw new ParseException("element " + n.getNodeName() + " is not accepted");
        }
      }
    }
  }

  private void validateElement(Element e) throws ParseException {
    validateAttributes(e);
    validateChildElements(e);
  }

  private void parseDocument() throws ParseException {
    // get the root element
    Element docEle = dom.getDocumentElement();
    if (!docEle.getTagName().equals("scxml")) {
      throw new ParseException("Document must be of type scxml");
    }

    validateElement(docEle);

    String name = docEle.getAttribute("name");

    if (name.isEmpty()) {
      throw new ParseException("scxml tag must contain a name attribute to specify output class");
    }

    int lastDotIndex = name.lastIndexOf(".");
    if (lastDotIndex == name.length() - 1) {
      throw new ParseException("scxml name attribute must not end in a '.'");
    }

    if (lastDotIndex == -1) {
      mClassName = name;
      mPackageName = null;
    } else {
      mClassName = name.substring(lastDotIndex + 1);
      mPackageName = name.substring(0, lastDotIndex);
    }

    mInitialStateName = docEle.getAttribute("initial");


    mDebug = docEle.getAttribute("debug").equalsIgnoreCase("true");


    List<Element> elements = getElements(docEle.getElementsByTagName("state"));
    if (elements == null) {
      throw new ParseException("scxml must contain at least one state");
    }


    for (Element e : elements) {
      State s = new State(e);
      getStates().add(s);
      if (getInitialStateName().isEmpty()) {
        mInitialStateName = s.getId();
      }
      if (s.mTransitions.size() == 0) {
        mTerminalStates.add(s);
      }
    }

    String baseStateName = docEle.getAttribute("base");
    if (!baseStateName.isEmpty()) {
      for (State s : getStates()) {
        if (baseStateName.equals(s.mId)) {
          mBaseState = s;
        }
      }
      if (mBaseState == null) {
        throw new ParseException("referenced base state " + baseStateName + " is not declared");
      }
    }

    boolean validatedInitialStateName = false;
    for (State s : getStates()) {
      if (s.getId().equals(getInitialStateName())) {
        validatedInitialStateName = true;
      }
    }
    if (!validatedInitialStateName) {
      throw new ParseException("referenced initial state " + getInitialStateName()
          + " was not declared.");
    }

    if (!mDeclaredStateNames.containsAll(mTargets)) {
      mTargets.removeAll(mDeclaredStateNames);
      throw new ParseException("target " + mTargets.iterator().next()
          + " does not represent a state declared in this scxml file");
    }

  }

  public String getClassName() {
    return mClassName;
  }

  public String getPackageName() {
    return mPackageName;
  }

  public List<State> getStates() {
    return mStates;
  }

  public boolean getDebugFlag() {
    return mDebug;
  }

  public List<State> getTerminalStates() {
    return mTerminalStates;
  }

  public State getBaseState() {
    return mBaseState;
  }

  public Set<String> getEventSet() {
    return mEvents;
  }

  public Set<String> getActionSet() {
    return mGlobalActions;
  }

  public void changeClassName(String newClassName) {
    mClassName = newClassName;
  }



}
