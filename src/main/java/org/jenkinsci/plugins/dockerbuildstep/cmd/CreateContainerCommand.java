package org.jenkinsci.plugins.dockerbuildstep.cmd;

import hudson.Extension;
import hudson.model.AbstractBuild;

import org.jenkinsci.plugins.dockerbuildstep.action.EnvInvisibleAction;
import org.jenkinsci.plugins.dockerbuildstep.log.ConsoleLogger;
import org.jenkinsci.plugins.dockerbuildstep.util.Resolver;
import org.kohsuke.stapler.DataBoundConstructor;

import com.github.dockerjava.client.DockerClient;
import com.github.dockerjava.client.DockerException;
import com.github.dockerjava.client.command.CreateContainerCmd;
import com.github.dockerjava.client.model.ContainerCreateResponse;
import com.github.dockerjava.client.model.ContainerInspectResponse;
import java.lang.reflect.Field;
import java.util.Collections;
import java.util.Map;

/**
 * This command creates new container from specified image.
 *
 * @see
 * http://docs.docker.com/reference/api/docker_remote_api_v1.13/#create-a-container
 *
 * @author vjuranek
 *
 */
public class CreateContainerCommand extends DockerCommand {

	private final String image;
	private final String command;
	private final String hostName;
	private final String portDbNameVar;
	private final String portDbNameValue;
	private final String servDbNameVar;
	private final String servDbNameValue;

	@DataBoundConstructor
	public CreateContainerCommand(String image, String command, String hostName, String portDbNameVar, String portDbNameValue, String servDbNameVar, String servDbNameValue) {
		this.image = image;
		this.command = command;
		this.hostName = hostName;
		this.portDbNameVar = portDbNameVar;
		this.portDbNameValue = portDbNameValue;
		this.servDbNameVar = servDbNameVar;
		this.servDbNameValue = servDbNameValue;
	}

	public String getImage() {
		return image;
	}

	public String getCommand() {
		return command;
	}

	public String getHostName() {
		return hostName;
	}

	public String getPortDbNameVar() {
		return portDbNameVar;
	}

	public String getPortDbNameValue() {
		return portDbNameValue;
	}

	public String getServDbNameVar() {
		return servDbNameVar;
	}

	public String getServDbNameValue() {
		return servDbNameValue;
	}

	@Override
	public void execute(@SuppressWarnings("rawtypes") AbstractBuild build, ConsoleLogger console)
			throws DockerException {
		// TODO check it when submitting the form
		if (image == null || image.isEmpty()) {
			throw new IllegalArgumentException("At least one parameter is required");
		}

		String imageRes = Resolver.buildVar(build, image);
		String commandRes = Resolver.buildVar(build, command);
		String hostNameRes = Resolver.buildVar(build, hostName);

		DockerClient client = getClient();
		CreateContainerCmd cfgCmd = client.createContainerCmd(imageRes);
		if (!commandRes.isEmpty()) {
			cfgCmd.withCmd(new String[]{commandRes});
		}
		cfgCmd.withHostName(hostNameRes);
		cfgCmd.withEnv(portDbNameVar+"="+portDbNameValue,servDbNameVar+"="+servDbNameValue);
		ContainerCreateResponse resp = client.execute(cfgCmd);
		console.logInfo("created container id " + resp.getId() + " (from image " + imageRes + ")");

		/*
		 * if (resp.getWarnings() != null) { for (String warn : resp.getWarnings()) System.out.println("WARN: " + warn);
		 * }
		 */
		ContainerInspectResponse inspectResp = client.execute(client.inspectContainerCmd(resp.getId()));
		EnvInvisibleAction envAction = new EnvInvisibleAction(inspectResp);
		

		build.addAction(envAction);
	}

	@Extension
	public static class CreateContainerCommandDescriptor extends DockerCommandDescriptor {

		@Override
		public String getDisplayName() {
			return "Create container";
		}
	}
	
//	protected void addEnvVars(){
//		Map<String, String> env = System.getenv();
//		env.put(this.getPortDbNameVar(), this.getPortDbNameValue());
//		env.put(this.getServDbNameVar(), this.getServDbNameValue());
//		setEnv(env);
//	}
//	
//	protected static void setEnv(Map<String, String> newenv) {
//		try {
//			Class<?> processEnvironmentClass = Class.forName("java.lang.ProcessEnvironment");
//			Field theEnvironmentField = processEnvironmentClass.getDeclaredField("theEnvironment");
//			theEnvironmentField.setAccessible(true);
//			Map<String, String> env = (Map<String, String>) theEnvironmentField.get(null);
//			env.putAll(newenv);
//			Field theCaseInsensitiveEnvironmentField = processEnvironmentClass.getDeclaredField("theCaseInsensitiveEnvironment");
//			theCaseInsensitiveEnvironmentField.setAccessible(true);
//			Map<String, String> cienv = (Map<String, String>) theCaseInsensitiveEnvironmentField.get(null);
//			cienv.putAll(newenv);
//		} catch (NoSuchFieldException e) {
//			try {
//				Class[] classes = Collections.class.getDeclaredClasses();
//				Map<String, String> env = System.getenv();
//				for (Class cl : classes) {
//					if ("java.util.Collections$UnmodifiableMap".equals(cl.getName())) {
//						Field field = cl.getDeclaredField("m");
//						field.setAccessible(true);
//						Object obj = field.get(env);
//						Map<String, String> map = (Map<String, String>) obj;
//						map.clear();
//						map.putAll(newenv);
//					}
//				}
//			} catch (Exception e2) {
//				e2.printStackTrace();
//			}
//		} catch (Exception e1) {
//			e1.printStackTrace();
//		}
//	}

}
