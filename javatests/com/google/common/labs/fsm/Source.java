// Copyright 2012 Google Inc. All Rights Reserved.

package com.google.common.labs.fsm;

import com.google.common.labs.fsm.ScxmlDoc.ParseException;

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;

import javax.tools.JavaCompiler;
import javax.tools.ToolProvider;

/**
 * The Source class is a unit testing utility to perform compilation and java
 * source code generation for the @{link Scxml2JavaTest} group of tests.
 *
 */
public class Source {

  private static final boolean DEBUG_SOURCE = false;
  private final JavaCompiler compiler;
  public Scxml2Java mTranslator;
  public String mClassName;
  public String mJavaSourcePath;
  public String mPackageName;
  public String mClassLoaderDirectory;
  public String mFullClassName;
  public ClassLoader mClassLoader;
  public String mJavaSourceDirectory;
  public int mCompilerResult;
  public Class<?> mClass;

  /**
   * Encapsulates various exceptions that may occur during processing of scxml source
   *
   */
  public class SourceException extends Exception {
    public SourceException(Exception e) {
      super(e);
    }
  }

  private Source(String scxmlPath) throws SourceException {
    try {
      compiler = ToolProvider.getSystemJavaCompiler();
      generateJavaSource(scxmlPath);
      if (DEBUG_SOURCE) {
        FsmTestUtil.exec("grep -n -e $ " + mJavaSourcePath);
      }
      mCompilerResult =
          compiler.run(null, System.err, System.err, "-d", mClassLoaderDirectory, mJavaSourcePath);
      loadClass();
    } catch (Exception e) {
      throw new SourceException(e);
    }
  }

  public static Source fromScxmlFile(String scxmlPath) throws SourceException {
    return new Source(scxmlPath);
  }

  public File redirectToFile() throws IOException {
    File fileStream = File.createTempFile("Scxml2JavaTest", null);
    fileStream.deleteOnExit();

    mClassLoaderDirectory = fileStream.getPath() + ".d/";
    File tmpDirFile = new File(mClassLoaderDirectory);
    tmpDirFile.mkdir();
    tmpDirFile.deleteOnExit();

    fileStream = new File(mClassLoaderDirectory + mClassName + ".java");
    fileStream.createNewFile();
    System.setOut(new PrintStream(fileStream));
    fileStream.deleteOnExit();
    return fileStream;
  }

  private void generateJavaSource(String fileName) throws IOException, ParseException {
    mTranslator = Scxml2Java.translatorForScxml(ScxmlDoc.createFromFile(fileName));
    mClassName = mTranslator.getClassName();
    mClassName = mClassName + Long.toString(System.currentTimeMillis());
    mTranslator.changeClassName(mClassName);
    mPackageName = mTranslator.getPackageName();
    if (mPackageName == null) {
      mFullClassName = mClassName;
    } else {
      mFullClassName = mPackageName + "." + mClassName;
    }
    File javaSource = redirectToFile();
    mTranslator.outputJava();
    mJavaSourcePath = javaSource.getPath();
    int classNameIndex = mJavaSourcePath.lastIndexOf(mClassName + ".java");
    mJavaSourceDirectory = mJavaSourcePath.substring(0, classNameIndex);
  }

  private Class<?> loadClass() throws MalformedURLException, ClassNotFoundException {
    File directory = new File(mJavaSourceDirectory);
    URL[] urls = new URL[] {directory.toURI().toURL()};

    // System.err.print("loadClass() using URL: " + urls[0] + "\n");
    mClassLoader = new URLClassLoader(urls);
    if (mPackageName == null) {
      mClass = mClassLoader.loadClass(mClassName);
    } else {
      mClass = mClassLoader.loadClass(mTranslator.getPackageName() + "." + mClassName);
    }
    return mClass;
  }

  public Class<?> getNamedEnum(String enumName) {
    for (Class<?> innerCls : mClass.getClasses()) {
      if (innerCls.isEnum() && innerCls.getName().equals(mFullClassName + "$" + enumName)) {
        return innerCls;
      }
    }
    return null;
  }

}
