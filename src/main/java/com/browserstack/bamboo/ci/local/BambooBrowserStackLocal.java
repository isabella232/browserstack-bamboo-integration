package com.browserstack.bamboo.ci.local;

import com.browserstack.local.Local;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.apache.commons.lang.StringUtils;
import java.util.Arrays;

/*
  Extends Local to support launch arguments.
  Inspired by https://github.com/jenkinsci/browserstack-integration-plugin/blob/master/src/main/java/com/browserstack/automate/ci/jenkins/local/JenkinsBrowserStackLocal.java
*/

/**
 * @author Shirish Kamath
 * @author Anirudha Khanna
 * @author Pulkit Sharma
 */
public class BambooBrowserStackLocal extends Local {
  private static final String OPTION_LOCAL_IDENTIFIER = "localIdentifier";

  private final String accesskey;
  private final String[] arguments;
  private String localIdentifier;

  public BambooBrowserStackLocal(String accesskey, String binaryArgs) {
      this.accesskey = accesskey;
      this.arguments = processLocalArguments((binaryArgs != null) ? binaryArgs.trim() : "");
  }

  private String[] processLocalArguments(final String argString) {
      String[] args = argString.split("\\s+");
      int localIdPos = 0;
      List<String> arguments = new ArrayList<String>();
      for (int i = 0; i < args.length; i++) {
          if (args[i].contains(OPTION_LOCAL_IDENTIFIER)) {
              localIdPos = i;
              if (i < args.length - 1 && args[i + 1] != null && !args[i + 1].startsWith("-")) {
                  localIdentifier = args[i + 1];
                  if (StringUtils.isNotBlank(localIdentifier)) {
                      return args;
                  }

                  // skip next, since already processed
                  i += 1;
              }

              continue;
          }

          arguments.add(args[i]);
      }

      localIdentifier = UUID.randomUUID().toString().replaceAll("\\-", "");
      arguments.add(localIdPos, localIdentifier);
      arguments.add(localIdPos, "-" + OPTION_LOCAL_IDENTIFIER);
      return arguments.toArray(new String[]{});
  }

  @Override
  protected LocalProcess runCommand(List<String> command) throws IOException {
      DaemonAction daemonAction = detectDaemonAction(command);
      if (daemonAction != null) {
          for (String arg : arguments) {
              if (StringUtils.isNotBlank(arg)) {
                  command.add(arg.trim());
              }
          }
      }
      return super.runCommand(command);
  }

  public void start() throws Exception {
      Map<String, String> localOptions = new HashMap<String, String>();
      localOptions.put("key", accesskey);

      super.start(localOptions);
  }


  public String getLocalIdentifier() {
      return localIdentifier;
  }

  private static DaemonAction detectDaemonAction(List<String> command) {
      if (command.size() > 2) {
          String action = command.get(2).toLowerCase();
          if (action.equals("start")) {
              return DaemonAction.START;
          } else if (action.equals("stop")) {
              return DaemonAction.STOP;
          }
      }

      return null;
  }

  private enum DaemonAction {
      START, STOP
  }

}
