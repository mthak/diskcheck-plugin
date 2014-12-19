package org.jenkinsci.plugin;

import hudson.AbortException;
import hudson.Extension;
import hudson.FilePath;
import hudson.Launcher;
import hudson.model.BuildListener;
import hudson.model.JobProperty;
import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.model.Descriptor;
import hudson.model.Cause.LegacyCodeCause;
import hudson.model.Computer;
import hudson.tasks.BuildWrapper;
import hudson.tasks.BuildTrigger;
import hudson.tasks.BuildStep;
import hudson.FilePath.FileCallable;
import hudson.slaves.OfflineCause;
import hudson.node_monitors.*;
import hudson.model.Node;
import hudson.model.Result;
import hudson.tasks.BatchFile;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.BuildWrapperDescriptor;
import hudson.tasks.BuildStepMonitor;
import hudson.tasks.CommandInterpreter;
import hudson.tasks.Publisher;
import hudson.tasks.Recorder;
import hudson.tasks.Shell;
import hudson.tasks.BuildWrapper.Environment;

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;

import org.kohsuke.stapler.DataBoundConstructor;

/**
 * Class to allow any build step to be performed before the SCM checkout occurs.
 * 
 * @author Manoj Thakkar
 * 
 */
public class Diskcheck extends BuildWrapper {
//	public boolean diskcheck;
	/**
	 * Stored build steps to run before the scm checkout is called
	 */
	// public final ArrayList<BuildStep> buildSteps;

//	public boolean diskspacecheck = true;

	public final boolean failOnError;

	/**
	 * Constructor taking a list of buildsteps to use.
	 * 
	 * @param buildstep
	 *            list of but steps configured in the UI
	 */
	@DataBoundConstructor
	public Diskcheck(boolean failOnError) {
		// ArrayList myCommand = new ArrayList();
		// myCommand.add("du-sh $WORKSPACE");
		// this.buildSteps=myCommand;
		// // this.buildSteps = buildstep;

	//	this.diskcheck = true;
		this.failOnError = failOnError;
	}

	/**
	 * Overridden setup returns a noop class as we don't want to add annything
	 * here.
	 * 
	 * @param build
	 * @param launcher
	 * @param listener
	 * @return noop Environment class
	 */
	@Override
	public Environment setUp(AbstractBuild build, Launcher launcher,
			BuildListener listener) throws IOException, InterruptedException {
		return new NoopEnv();
	}

	/**
	 * Overridden precheckout step, this is where wedo all the work.
	 * 
	 * Checks to make sure we have some buildsteps set, and then calls the
	 * prebuild and perform on all of them.
	 * 
	 * @todo handle build steps failure in some sort of reasonable way
	 * 
	 * @param build
	 * @param launcher
	 * @param listener
	 */
	@Override
	public void preCheckout(AbstractBuild build, Launcher launcher,
			BuildListener listener) throws IOException, InterruptedException {
		PrintStream log = listener.getLogger();
// Default value of disk space check is 1Gb		
		int SpaceThreshold=1;
		SpaceThreshold = PluginImpl.getInstance().spacecheck();
		

		log.println("Disk space threshold is set to :" + SpaceThreshold + "Gb");
		log.println("Checking disk space Now ");

		/* touch workspace so that it is created on first time */
		if (!build.getWorkspace().exists()) {
			build.getWorkspace().mkdirs();
		}

		Node node1 = build.getBuiltOn();
		Computer Comp = node1.toComputer();
		String NodeName = build.getBuiltOnStr();

		long size = DiskSpaceMonitor.DESCRIPTOR.get(Comp).size;
		int roundedSize = (int) (size / (1024 * 1024 * 1024));
		log.println("Total Disk Space Available is: " + roundedSize + "Gb");

		if (build.getBuiltOnStr() == "") {
			NodeName = "master";
		}

		log.println(" Node Name: " + NodeName);
	
		if (PluginImpl.getInstance().isDiskrecyclerenabled()) {
			if (roundedSize < SpaceThreshold) {
				log.println("Disk Recycler is Enabled so I am going to wipe off the workspace Directory Now ");
				String mycommand = "echo $WORKSPACE; rm -rf $WORKSPACE/../; df -k .";
				String mywincommand = "echo Deleting file from %WORKSPACE% && Del /R %WORKSPACE%";

				/**
				 * This method will return the command intercepter as per the
				 * node OS
				 * 
				 * @param launcher
				 * @param script
				 * @return CommandInterpreter
				 */
				CommandInterpreter runscript;
				if (launcher.isUnix())
					runscript = new Shell(mycommand);
				else
					runscript = new BatchFile(mywincommand);

				Result result = runscript.perform(build, launcher, listener) ? Result.SUCCESS
						: Result.FAILURE;

				if (result.toString() == "FAILURE") {
					throw new AbortException(
							"Something went wrong while deleting Files , Please check the error message above");
				}
			}
		}

		log.println("Running Prebuild steps");
		/*
		 * System.out .println(
		 * "build.getBuiltOnStr() (Built on Slave, if empty it is built on master): "
		 * + build.getBuiltOnStr());
		 * System.out.println("build.getDescription(): " +
		 * build.getDescription());
		 * System.out.println("build.getDisplayName(): " +
		 * build.getDisplayName()); System.out.println("build.getDuration(): " +
		 * build.getDuration());
		 * System.out.println("build.getStartTimeInMillis(): " +
		 * build.getStartTimeInMillis());
		 * System.out.println("build.getProject().getDisplayName(): " +
		 * build.getProject().getDisplayName());
		 * System.out.println("build.getProject().getFullDisplayName(): " +
		 * build.getProject().getFullDisplayName());
		 * System.out.println("build.getProject().getFullName(): " +
		 * build.getProject().getFullName());
		 * System.out.println("build.getUrl(): " + build.getUrl());
		 */
		if (roundedSize < SpaceThreshold
				&& !(PluginImpl.getInstance().isDiskrecyclerenabled())) {
			throw new AbortException(
					"Disk Space is too low please look into it before starting a build");

		}
	}

	// }

	@Extension
	public static final class DescriptorImpl extends Descriptor<BuildWrapper> {

		/**
		 * This human readable name is used in the configuration screen.
		 */
		public String getDisplayName() {
				return "Check Disk Space";
		}

	}

	class NoopEnv extends Environment {
	}
}
