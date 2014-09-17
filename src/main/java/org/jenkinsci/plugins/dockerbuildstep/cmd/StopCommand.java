package org.jenkinsci.plugins.dockerbuildstep.cmd;

import hudson.Extension;
import hudson.model.AbstractBuild;

import java.util.Arrays;
import java.util.List;

import org.jenkinsci.plugins.dockerbuildstep.log.ConsoleLogger;
import org.jenkinsci.plugins.dockerbuildstep.util.Resolver;
import org.kohsuke.stapler.DataBoundConstructor;

import com.github.dockerjava.client.DockerClient;
import com.github.dockerjava.client.DockerException;
import com.github.dockerjava.client.command.StopContainerCmd;
import hudson.EnvVars;
import hudson.model.ParameterValue;
import hudson.model.ParametersAction;
import hudson.model.StringParameterValue;
import java.io.File;
import java.util.ArrayList;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * This command stops one or more Docker containers.
 * 
 * @see http://docs.docker.com/reference/api/docker_remote_api_v1.13/#stop-a-container
 * 
 * @author vjuranek
 * 
 */
public class StopCommand extends DockerCommand {

    private final String containerIds;

    @DataBoundConstructor
    public StopCommand(String containerIds) {
        this.containerIds = containerIds;
    }

    public String getContainerIds() {
        return containerIds;
    }

    @Override
    public void execute(@SuppressWarnings("rawtypes") AbstractBuild build, ConsoleLogger console)
            throws DockerException {
        if (containerIds == null || containerIds.isEmpty()) {
            throw new IllegalArgumentException("At least one parameter is required");
        }
		EnvVars environment = null;
		try {
			environment = build.getEnvironment(console.getListener());
		} catch (Exception ex) {
			throw new DockerException("Cannot read env variables");
		} 
		
		List<String> ids = new ArrayList<String>();
		File folder = new File(environment.get("PWD") + File.separator + "docker" + File.separator +"run"+ File.separator + environment.get("JOB_NAME") + File.separator + environment.get("GIT_BRANCH") + File.separator);
		File[] listOfFiles = folder.listFiles();
		if(containerIds.equals("${LAST}")){
			if(listOfFiles != null && listOfFiles.length > 0){
				for (int i = 0; i < listOfFiles.length; i++) {
					File listOfFile = listOfFiles[i];
					ids.add(listOfFile.getName());
					console.logInfo("Container id file found: " + listOfFile.getName());
				}
			}
		} else {
			String containerIdsRes = Resolver.buildVar(build, containerIds);
			ids = Arrays.asList(containerIdsRes.split(","));
		}
        DockerClient client = getClient();
        //TODO check, if container is actually running
        for (String id : ids) {
            id = id.trim();
			client.stopContainerCmd(id).exec();
            console.logInfo("Stopped container id " + id);
			File file = new File(folder + File.separator + id);
			if(file.delete()){
				console.logInfo("File deleted: " + file.getPath());
			} else {
				console.logError("Cannot delete file: " + file.getPath());
			}
        }
    }

    @Extension
    public static class StopCommandDescriptor extends DockerCommandDescriptor {
        @Override
        public String getDisplayName() {
            return "Stop container(s)";
        }
    }

}
