package com.browserstack.bamboo.ci.action;

import com.atlassian.bamboo.variable.VariableContext;
import com.atlassian.bamboo.variable.VariableDefinitionContext;
import com.atlassian.bamboo.variable.VariableType;
import com.atlassian.bamboo.build.CustomPreBuildAction;
import com.atlassian.bamboo.v2.build.BaseConfigurableBuildPlugin;
import com.atlassian.bamboo.v2.build.BuildContext;
import com.atlassian.bamboo.ww2.actions.build.admin.create.BuildConfiguration;
import com.atlassian.bamboo.configuration.AdministrationConfiguration;
import com.atlassian.bamboo.configuration.AdministrationConfigurationAccessor;
import com.browserstack.bamboo.ci.BStackEnvVars;
import com.atlassian.spring.container.ContainerManager;
import org.apache.commons.lang.StringUtils;
import com.atlassian.bamboo.plan.Plan;
import com.browserstack.bamboo.ci.BStackConfigManager;
import com.browserstack.bamboo.ci.singletons.BrowserStackLocalSingleton;
import com.browserstack.bamboo.ci.local.BambooBrowserStackLocal;
import com.atlassian.bamboo.build.BuildLoggerManager;
import com.atlassian.bamboo.build.logger.BuildLogger;
import java.util.Arrays;
import java.util.Map;


public class BuildConfigurator extends BaseConfigurableBuildPlugin implements CustomPreBuildAction {

    private AdministrationConfigurationAccessor administrationConfigurationAccessor;
    private BStackConfigManager configManager;



    @Override
    public BuildContext call() {
      this.configManager = new BStackConfigManager(administrationConfigurationAccessor.getAdministrationConfiguration(), buildContext.getBuildDefinition().getCustomConfiguration());
      
      // System.out.println(Arrays.asList(buildContext.getBuildDefinition().getCustomConfiguration()));

      if(configManager.hasCredentials()) {
        System.out.println("FOUND BS CREDENTIALS"); 
        if(configManager.localEnabled()) {
          startLocal();
        }
      } else {
        System.out.println("BS CREDENTIALS NOT FOUND");  
      }

      return buildContext;
    }

    private void startLocal() {
      BuildLoggerManager buildLoggerManager = (BuildLoggerManager) ContainerManager.getComponent("buildLoggerManager");
      final BuildLogger buildLogger = buildLoggerManager.getLogger(buildContext.getResultKey());
      
      //Setting BambooBrowserStackLocal instance to null because updated configuration was not updated in the Singleton instance(because it was already in memory ??)
      BrowserStackLocalSingleton.reset();
      String accessKey = configManager.get(BStackEnvVars.BSTACK_ACCESS_KEY);
      String localPath = configManager.get(BStackEnvVars.BSTACK_LOCAL_PATH);
      String localArgs = configManager.get(BStackEnvVars.BSTACK_LOCAL_ARGS);
      BambooBrowserStackLocal browserStackLocal = BrowserStackLocalSingleton.getBrowserStackLocal(accessKey, localPath, localArgs);

      if(StringUtils.isNotBlank(localPath)) {
        localPath = localPath + " ";
      }

      if(StringUtils.isNotBlank(localArgs)) {
        localArgs = "with args " + localArgs;
      }


      try {
        buildLogger.addBuildLogEntry("Starting BrowserStackLocal Binary " + localPath + localArgs);
        browserStackLocal.start();
        //Add Sleep Here ? Got 'browserstack.local is set to true but BrowserStackLocal binary is not connected error.'
        buildLogger.addBuildLogEntry("BrowserStackLocal Binary started successfully. LocalIdentifier: " + browserStackLocal.getLocalIdentifier());
        injectVariable(buildContext, BStackEnvVars.BSTACK_LOCAL_IDENTIFIER,browserStackLocal.getLocalIdentifier());
      } catch (Exception e) {
        buildLogger.addBuildLogEntry("Exception while starting the BrowserStackLocal Binary : " + e.toString());
        throw new RuntimeException("Exception while starting the BrowserStackLocal Binary : " + e.toString());
      }

    }

    private void injectVariable(BuildContext buildContext, String key, String value) {
        System.out.println("Injecting " + key + ": " + value);
        VariableContext variableContext =  buildContext.getVariableContext();
        variableContext.addLocalVariable(key, value);
        VariableDefinitionContext variableDefinitionContext = variableContext.getEffectiveVariables().get(key);
        if (variableDefinitionContext != null)
        {
          variableDefinitionContext.setVariableType(VariableType.ENVIRONMENT);
        }
    }

    @Override
    protected void populateContextForEdit(final Map<String, Object> context, final BuildConfiguration buildConfiguration, final Plan build) {
      String contextPrefix = "custom.browserstack.";

      context.put("browserstack_username_key", contextPrefix + BStackEnvVars.BSTACK_USERNAME);
      context.put("browserstack_access_key_key", contextPrefix + BStackEnvVars.BSTACK_ACCESS_KEY);
      context.put("browserstack_local_enabled_key", contextPrefix + BStackEnvVars.BSTACK_LOCAL_ENABLED);
      context.put("browserstack_local_path_key", contextPrefix + BStackEnvVars.BSTACK_LOCAL_PATH);
      context.put("browserstack_local_args_key", contextPrefix + BStackEnvVars.BSTACK_LOCAL_ARGS); 
    }

    public AdministrationConfigurationAccessor getAdministrationConfigurationAccessor() {
        return administrationConfigurationAccessor;
    }

    public void setAdministrationConfigurationAccessor(AdministrationConfigurationAccessor administrationConfigurationAccessor) {
        this.administrationConfigurationAccessor = administrationConfigurationAccessor;
    }
}
