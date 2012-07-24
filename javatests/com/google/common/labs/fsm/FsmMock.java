// Copyright 2012 Google Inc. All Rights Reserved.

package com.google.common.labs.fsm;

import com.google.common.labs.fsm.Source.SourceException;

import javassist.CannotCompileException;
import javassist.ClassPool;
import javassist.CtClass;
import javassist.CtConstructor;
import javassist.CtField;
import javassist.CtMethod;
import javassist.CtNewMethod;
import javassist.NotFoundException;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.security.ProtectionDomain;
import java.util.HashMap;
import java.util.Map;

/**
 * Facade to a finite state machine as a subclass of the abstract class generated by Scxml2Java
 *
 */
public class FsmMock {

  private CtClass mBaseCtClass;
  private CtClass mSubCtClass;
  public Object mObject;
  private Class<?> mSubClass;
  private Class<?> mBaseClass;
  private Method mStart;
  private Method mPushEvent;
  private Field mRecordedStateChange;
  public Source mSource;
  private Method mIsInTerminalState;
  private Class<?> mEventEnum;
  public Class<?> mStateException;
  private final Map<String, Integer> mActionCounts = new HashMap<String, Integer>();

  /**
   * Implemented by tests to listen for the occurrence of any action
   *
   */
  public interface ActionListener {
    public void onAction(String actionName) throws FsmException;
  }

  public ActionListener mActionListener = new ActionListener() {
    @Override
    public void onAction(String actionName) throws FsmException {
      if (mActionCounts.containsKey(actionName)) {
        mActionCounts.put(actionName, mActionCounts.get(actionName) + 1);
      } else {
        mActionCounts.put(actionName, 1);
      }
      if (mTestActionListener != null) {
        mTestActionListener.onAction(actionName);
      }
    }
  };

  public ActionListener mTestActionListener;
  private Class<?> mDebugLoggerInterface;
  private Method mSetDebugLogger;
  private ClassPool mCp;
  // public DebugLoggerListener mDebugLogger;
  private Field mLastStateMsg;
  private Field mLastEventMsg;
  private Field mLastActionMsg;
  private Object mDebugLogger;
  private ProtectionDomain mProtectionDomain;

  public FsmMock(String scxmlPath) throws FsmException, SourceException {
    this(Source.fromScxmlFile(scxmlPath));
  }


  FsmMock(Source source) throws FsmException {
    mSource = source;
    setupClassPool();
    buildSubclass();
    instantiateSubclass();
    extractReferences();
  }

  private void setupClassPool() throws FsmException {
    mCp = ClassPool.getDefault();
    try {
      mCp.appendClassPath(mSource.mJavaSourceDirectory);
      mCp.appendClassPath(mSource.mClassLoaderDirectory);
    } catch (NotFoundException e) {
      throw new FsmException(e);
    }
    mCp.appendSystemPath();
  }


  private void buildSubclass() throws FsmException {
    try {

      mBaseCtClass = mCp.get(mSource.mFullClassName);
      mSubCtClass = mCp.makeClass(mSource.mFullClassName + "Subclass", mBaseCtClass);

      // override abstract onStateChange()
      CtField recordedStateChange = CtField.make(
          "public " + mSource.mFullClassName + "$State mRecordedStateChange;", mSubCtClass);
      mSubCtClass.addField(recordedStateChange);
      CtMethod onStateChange = CtNewMethod.make("protected void onStateChange("
          + mSource.mFullClassName + "$State state) {" + "mRecordedStateChange = state;" + "}",
          mSubCtClass);
      mSubCtClass.addMethod(onStateChange);

      // add actions and action counters
      CtField f = CtField.make(
          "private com.google.common.labs.fsm.FsmMock.ActionListener mActionListener;",
          mSubCtClass);
      mSubCtClass.addField(f);
      for (String action : mSource.mTranslator.getDoc().getActionSet()) {
        CtMethod m = CtNewMethod.make("protected void onAction" + action + "() {"
            + "mActionListener.onAction(\"" + action + "\");" + "}", mSubCtClass);
        mSubCtClass.addMethod(m);
      }
      CtMethod ctSetActionListener = CtNewMethod.make(
          "public void setActionListener(com.google.common.labs.fsm.FsmMock.ActionListener " +
              "actionListener) { mActionListener = actionListener; }",
          mSubCtClass);
      mSubCtClass.addMethod(ctSetActionListener);

      // Freeze the base and subclass
      mBaseClass = mCp.toClass(mBaseCtClass);
      mProtectionDomain = mBaseClass.getProtectionDomain();
      mSubClass = mSubCtClass.toClass(mSource.mClassLoader, mProtectionDomain);

    } catch (NotFoundException e) {
      throw new FsmException(e);
    } catch (CannotCompileException e) {
      throw new FsmException(e);
    }
  }

  private void instantiateSubclass() throws FsmException {
    try {
      // Instantiate the subclass
      mObject = mSubClass.newInstance();
      Method setActionListener = mSubClass.getMethod("setActionListener", ActionListener.class);
      setActionListener.invoke(mObject, mActionListener);
    } catch (InstantiationException e) {
      throw new FsmException(e);
    } catch (IllegalAccessException e) {
      throw new FsmException(e);
    } catch (NoSuchMethodException e) {
      throw new FsmException(e);
    } catch (InvocationTargetException e) {
      throw new FsmException(e);
    }
  }


  private void extractReferences() throws FsmException {
    // Extract references to enums, methods, fields, and exceptions
    mEventEnum = mSource.getNamedEnum("Event");
    try {
      mStart = mSubClass.getMethod("start");
      mPushEvent = mSubClass.getMethod("pushEvent", mEventEnum);
      mIsInTerminalState = mSubClass.getMethod("isInTerminalState");
      mRecordedStateChange = mSubClass.getField("mRecordedStateChange");
      for (Class<?> c : mSubClass.getClasses()) {
        if (c.getCanonicalName().equals(mSource.mFullClassName + ".StateException")) {
          mStateException = c;
        } else if (c.getCanonicalName().equals(mSource.mFullClassName + ".DebugLogger")) {
          mDebugLoggerInterface = c;
        }
      }
      mSetDebugLogger = mSubClass.getMethod("setDebugLogger", mDebugLoggerInterface);
    } catch (SecurityException e) {
      throw new FsmException(e);
    } catch (NoSuchMethodException e) {
      throw new FsmException(e);
    } catch (NoSuchFieldException e) {
      throw new FsmException(e);
    }
  }

  public void start() throws FsmException {
    try {
      mStart.invoke(mObject);
    } catch (IllegalArgumentException e) {
      throw new FsmException(e);
    } catch (IllegalAccessException e) {
      throw new FsmException(e);
    } catch (InvocationTargetException e) {
      throw new FsmException(e);
    }
  }

  public String getCurrentStateName() throws FsmException {
    String name;
    try {
      Method getCurrentState = mSubClass.getMethod("getCurrentState");
      name = getCurrentState.invoke(mObject).toString();
    } catch (SecurityException e) {
      throw new FsmException(e);
    } catch (NoSuchMethodException e) {
      throw new FsmException(e);
    } catch (IllegalArgumentException e) {
      throw new FsmException(e);
    } catch (IllegalAccessException e) {
      throw new FsmException(e);
    } catch (InvocationTargetException e) {
      throw new FsmException(e);
    }
    return name;
  }

  public String getRecordedStateChangeName() throws FsmException {
    String name;
    try {
      name = mRecordedStateChange.get(mObject).toString();
    } catch (IllegalArgumentException e) {
      throw new FsmException(e);
    } catch (IllegalAccessException e) {
      throw new FsmException(e);
    }
    return name;
  }

  public boolean pushEvent(String eventName) throws FsmException {
    Boolean result;
    try {
      for (Object o : mEventEnum.getEnumConstants()) {
        if (o.toString().equals(eventName)) {
          return ((Boolean) mPushEvent.invoke(mObject, o)).booleanValue();
        }
      }
      throw new FsmException("Event named " + eventName + " not found");
    } catch (IllegalArgumentException e) {
      throw new FsmException(e);
    } catch (IllegalAccessException e) {
      throw new FsmException(e);
    } catch (InvocationTargetException e) {
      throw new FsmException(e);
    }
  }

  public boolean isInTerminalState() throws FsmException {
    Boolean result;
    try {
      result = (Boolean) mIsInTerminalState.invoke(mObject);
    } catch (IllegalArgumentException e) {
      throw new FsmException(e);
    } catch (IllegalAccessException e) {
      throw new FsmException(e);
    } catch (InvocationTargetException e) {
      throw new FsmException(e);
    }
    return result.booleanValue();
  }

  public int getTotalActionCount() {
    int count = 0;
    for (String action : mSource.mTranslator.getDoc().getActionSet()) {
      count += getActionCount(action);
    }
    return count;
  }


  public int getActionCount(String action) {
    if (mActionCounts.containsKey(action)) {
      return mActionCounts.get(action);
    }
    return 0;
  }

  /**
   * extend the generated DebugLogger interface to keep a copy of the last
   * message of each type (Event, Action, State)
   *
   */
  public void setupDebugLogging() throws FsmException {
    CtClass debugLoggerInterfaceCt = null;
    try {
      for (CtClass c : mBaseCtClass.getNestedClasses()) {
        if (c.getName().equals(mSource.mClassName + "$DebugLogger")) {
          debugLoggerInterfaceCt = c;
        }
      }
    } catch (NotFoundException e) {
      throw new FsmException(e);
    }

    CtClass debugLoggerCt = mCp.makeClass(mSource.mClassName + "DebugLoggerSubclass");
    CtClass interfaces[] = new CtClass[1];
    interfaces[0] = debugLoggerInterfaceCt;
    debugLoggerCt.setInterfaces(interfaces);
    CtConstructor contructor = new CtConstructor(null, debugLoggerCt);
    try {
      contructor.setBody("{}");
      debugLoggerCt.addConstructor(contructor);
    } catch (CannotCompileException e) {
      throw new FsmException(e);
    }

    try {
      // make mLastEventMsg, etc. each a CtField of debugLoggerCt
      CtField f = CtField.make("public String mLastEventMsg;", debugLoggerCt);
      debugLoggerCt.addField(f);
      f = CtField.make("public String mLastStateMsg;", debugLoggerCt);
      debugLoggerCt.addField(f);
      f = CtField.make("public String mLastActionMsg;", debugLoggerCt);
      debugLoggerCt.addField(f);
    } catch (CannotCompileException e) {
      throw new FsmException(e);
    }

    try {
      // override onEvent, etc. to write the message to mLastEventMsg, etc.
      CtMethod m =
          CtMethod.make("public void onState(String msg) { mLastStateMsg = msg; }", debugLoggerCt);
      debugLoggerCt.addMethod(m);
      m = CtMethod.make(
          "public void onAction(String msg) { mLastActionMsg = msg; }", debugLoggerCt);
      debugLoggerCt.addMethod(m);
      m = CtMethod.make("public void onEvent(String msg) { mLastEventMsg = msg; }", debugLoggerCt);
      debugLoggerCt.addMethod(m);
    } catch (CannotCompileException e) {
      throw new FsmException(e);
    }

    // for each mLastEventMsg, etc. extract a Field
    Class<?> c;
    try {
      c = debugLoggerCt.toClass(mSource.mClassLoader, mProtectionDomain);
      mLastStateMsg = c.getField("mLastStateMsg");
      mLastEventMsg = c.getField("mLastEventMsg");
      mLastActionMsg = c.getField("mLastActionMsg");
    } catch (SecurityException e) {
      throw new FsmException(e);
    } catch (NoSuchFieldException e) {
      throw new FsmException(e);
    } catch (CannotCompileException e) {
      e.printStackTrace();
      throw new FsmException(e);
    }
    try {
      mDebugLogger = c.newInstance();
    } catch (InstantiationException e) {
      throw new FsmException(e);
    } catch (IllegalAccessException e) {
      throw new FsmException(e);
    }
    try {
      mSetDebugLogger.invoke(mObject, mDebugLogger);
    } catch (IllegalArgumentException e) {
      e.printStackTrace();
      throw new FsmException(e);
    } catch (IllegalAccessException e) {
      throw new FsmException(e);
    } catch (InvocationTargetException e) {
      throw new FsmException(e);
    }
  }

  public String getLastEventMsg() throws FsmException {
    try {
      return (String) mLastEventMsg.get(mDebugLogger);
    } catch (IllegalArgumentException e) {
      throw new FsmException(e);
    } catch (IllegalAccessException e) {
      throw new FsmException(e);
    }
  }

  public String getLastActionMsg() throws FsmException {
    try {
      return (String) mLastActionMsg.get(mDebugLogger);
    } catch (IllegalArgumentException e) {
      throw new FsmException(e);
    } catch (IllegalAccessException e) {
      throw new FsmException(e);
    }
  }

  public String getLastStateMsg() throws FsmException {
    try {
      return (String) mLastStateMsg.get(mDebugLogger);
    } catch (IllegalArgumentException e) {
      throw new FsmException(e);
    } catch (IllegalAccessException e) {
      throw new FsmException(e);
    }
  }

}