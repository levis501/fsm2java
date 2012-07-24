// Copyright 2012 Google Inc. All Rights Reserved.

package com.google.common.labs.fsm;

import com.google.common.labs.fsm.ScxmlDoc.ParseException;
import com.google.common.labs.fsm.ScxmlDoc.State;
import com.google.common.labs.fsm.ScxmlDoc.Transition;

import java.io.IOException;

/**
 * The Scxml2Dot produces a .dot graph description file from an
 * @{link ScxmlDoc} object
 *
 *  The output can be converted to a .png with one of the command line features of GraphViz
 *
 *  for example:
 *
 *  $ java com.google.common.labs.fsm.Scxml2Dot myfsm.xml > myfsm.dot
 *  $ dot -Tpng -o myfsm.png myfsm.dot
 */
public class Scxml2Dot {

  private final ScxmlDoc mDoc;

  private Scxml2Dot(ScxmlDoc doc) {
    mDoc = doc;
  }

  public static Scxml2Dot translatorForScxml(ScxmlDoc doc) {
    Scxml2Dot translator = new Scxml2Dot(doc);
    return translator;
  }

  /**
   * @param args The scxml input file
   * @throws IOException
   * @throws ParseException
   */
  public static void main(String[] args) throws IOException, ParseException {
    ScxmlDoc doc = ScxmlDoc.createFromFile(args[0]);
    Scxml2Dot translator = Scxml2Dot.translatorForScxml(doc);
    translator.outputDot();
  }

  private void outputTransition(int i, State s, Transition t) {
    String label = t.mEvent;
    if (t.mActions.size() > 0) {
      label = label + "\\n(" + t.mActions.get(0);
      for (int index = 1; index < t.mActions.size(); index++) {
        label = label + ",\\n" + t.mActions.get(index);
      }
      label = label + ")";
    }
    out(1, s.mId + " -> " + t.mTarget + " [fontsize=10, label=\"" + label + "\"];");
  }

  public void outputDot() {
    out(0, "digraph " + mDoc.getClassName() + " {");

    String label;
    for (State s : mDoc.getStates()) {
      if (s == mDoc.getBaseState()) {
        continue;
      }

      if ((s.mId.equals(mDoc.getInitialStateName())) || mDoc.mTerminalStates.contains(s)) {
        out(1, s.mId + " [shape=ellipse]");
      } else {
        out(1, s.mId + " [shape=circle]");
      }
      for (Transition t : s.mTransitions) {
        outputTransition(1, s, t);
      }
    }

    // add base transitions
    if (mDoc.getBaseState() != null) {
      for (Transition t : mDoc.getBaseState().mTransitions) {
        outputTransition(1, mDoc.getBaseState(), t);
      }
      out(1, mDoc.getBaseState().mId + "[shape=doublecircle, color=blue]");
    }
    out(0, "}");
  }

  private void out(String s) {
    System.out.print(s + "\n");
  }

  private void out(int tabs, String s) {
    for (int i = 0; i < tabs; i++) {
      s = "    " + s;
    }
    out(s);
  }

  /**
   * @return a parsed version of the Scxml document
  public ScxmlDoc getDoc() {
    return mDoc;
  }
   */
}
