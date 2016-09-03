package com.pr.gradle.task;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;

import javax.inject.Inject;

import org.gradle.api.Action;
import org.gradle.api.DefaultTask;
import org.gradle.api.GradleException;
import org.gradle.api.Project;
import org.gradle.api.Task;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.file.FileCollection;
import org.gradle.api.tasks.TaskAction;
import org.gradle.process.JavaExecSpec;
import org.gradle.process.internal.ExecActionFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.pr.gradle.daemon.JavaExecForkServer;
import com.pr.gradle.util.PortUtils;

public class JavaExecFork extends DefaultTask {
  protected static final Logger log = LoggerFactory.getLogger(JavaExecFork.class);
  
  public FileCollection classpath;
  public String main;
  public List<String> jvmArgs = new ArrayList<>();
  public List<String> args = new ArrayList<>();
  public Map<String, ?> systemProperties = new HashMap<>();
  public Map<String, ?> environment = new HashMap<>();
  private Callable<OutputStream> standardOutput = () -> new ByteArrayOutputStream();
  private Callable<OutputStream> errorOutput = () -> new ByteArrayOutputStream();
  public JavaExecJoin joinTask;
  public Task stopAfter;
  public Integer controlPort = PortUtils.findOpenPort();
  public Integer waitForPort;

  @Inject
  protected ExecActionFactory getExecActionFactory() {
      throw new UnsupportedOperationException();
  }

  @TaskAction
  public void exec() {
    if (main == null) {
      throw new GradleException(JavaExecFork.class.getSimpleName() + " task '" + getName() + "' must specify a main class");
    }

    if (classpath == null) {
      throw new GradleException(JavaExecFork.class.getSimpleName() + " task '" + getName() + "' must specify a classpath");
    }

    if (stopAfter == null) {
      throw new GradleException(JavaExecFork.class.getSimpleName() + " task '" + getName() + "' must specify a stopAfter task");
    }

    Thread t = new Thread(() -> {
      getProject().javaexec(new Action<JavaExecSpec>() {
        @Override
        public void execute(JavaExecSpec spec) {
          log.info("Starting main method {}", main);
          log.info("using args {}", args);
          log.info("using jvmArgs {}", jvmArgs);
          log.info("using systemProperties {}", systemProperties);
          log.info("using environment {}", environment);
          
          FileCollection buildScriptClasspath = getBuildscriptClasspath(getProject());
          spec.setMain(JavaExecForkServer.class.getName());
          spec.setClasspath(classpath.plus(buildScriptClasspath));

          if (args == null)
            args = new ArrayList<>();

          args.add(0, main);
          args.add(1, Integer.toString(controlPort));
          spec.args(args);
          spec.jvmArgs(jvmArgs);
          spec.systemProperties(systemProperties);
          spec.environment(environment);
          spec.setStandardOutput(call(standardOutput));
          spec.setErrorOutput(call(errorOutput));
          
          log.info("using JavaExecSpec {}", spec);
        }
      });
    });
    t.start();
    log.info("done executing {}!", main);

    if (waitForPort != null)
      PortUtils.waitForPortOpen(waitForPort, 60, TimeUnit.SECONDS);
  }

  private OutputStream call(Callable<OutputStream> callable) {
    try {
      return callable.call();
    } catch (Exception e) {
      throw new GradleException("Error getting OutputStream", e);
    }
  }

  private FileCollection getBuildscriptClasspath(Project project) {
    Configuration classpath = project.getBuildscript().getConfigurations().getByName("classpath");
    if (classpath.isEmpty() && project.getParent() != null) {
      return getBuildscriptClasspath(project.getParent());
    }
    return classpath;
  }

  public void setStopAfter(Task stopAfter) {
    if (joinTask == null) {
      throw new GradleException(JavaExecFork.class.getSimpleName() + " task '" + getName() + "' did not have a joinTask associated. Make sure you have \"apply plugin: 'gradle-javaexecfork-plugin'\" somewhere in your gradle file");
    }

    log.info("Adding {} as a finalizing task to {}", joinTask.getName(), stopAfter.getName());
    stopAfter.finalizedBy(joinTask);
    this.stopAfter = stopAfter;
  }

  public void setStandardOutput(Object output) throws FileNotFoundException {
    if (output instanceof OutputStream) {
      this.standardOutput = () -> (OutputStream) output;
    } else if (output instanceof File) {
      this.standardOutput = () -> new FileOutputStream((File) output);
    } else {
      this.standardOutput = () -> new FileOutputStream(output.toString());
    }
  }

  public void setErrorOutput(Object output) throws FileNotFoundException {
    if (output instanceof OutputStream) {
      this.errorOutput = () -> (OutputStream) output;
    } else if (output instanceof File) {
      this.errorOutput = () -> new FileOutputStream((File) output);
    } else {
      this.errorOutput = () -> new FileOutputStream(output.toString());
    }
  }
}
