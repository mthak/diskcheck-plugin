package org.jenkinsci.plugin;

import hudson.AbortException;
import hudson.Extension;
import hudson.FilePath;
import hudson.Launcher;
import hudson.model.BuildListener;
import hudson.model.AbstractBuild;
import hudson.model.Descriptor;
import hudson.model.Computer;
import hudson.tasks.BuildWrapper;
import hudson.node_monitors.*;
import hudson.model.Node;

import org.kohsuke.stapler.DataBoundConstructor;

import java.io.IOException;
import java.io.PrintStream;
import java.net.UnknownHostException;
import java.util.List;



/**
 * Class to allow any build step to be performed before the SCM checkout occurs.
 * 
 * @author Manoj Thakkar edited by Frano Stanisic
 * 
 */


public class Diskcheck extends BuildWrapper {


	public final boolean failOnError;
        
        public static FilePath lastFileModified(PrintStream log, FilePath dir) throws UnknownHostException, IOException, InterruptedException {
            log.println("lastFileModified call made");
            log.println("dir passed was " + dir);
            List<FilePath> files = dir.listDirectories();
            long lastMod = Long.MAX_VALUE;
            FilePath choice = null;
            for (FilePath file : files) {
                if (file.lastModified() < lastMod) {
                    choice = file;
                    lastMod = file.lastModified();
                }
            }
            return choice;
        }        
	/**
	 * Constructor taking a list of buildsteps to use.
	 * 
	 * @param buildstep
	 *            list of build steps configured in the UI
	 */
	@DataBoundConstructor
	public Diskcheck(boolean failOnError) {
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
	 * Overridden precheckout step, this is where we do all the work.
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
		public Descriptor getDescriptor() {
	        return (Descriptor) super.getDescriptor();
	    }
                
	@Override
	public void preCheckout(AbstractBuild build, Launcher launcher,
			BuildListener listener) throws IOException, InterruptedException {
		PrintStream log = listener.getLogger();
// Default value of disk space check is 1Gb		
		int SpaceThreshold = PluginImpl.getInstance().getSpacecheck();
		

		log.println("Disk space threshold is set to :" + SpaceThreshold + "Gb");
		log.println("Checking disk space Now ");

		/* touch workspace so that it is created on first time */
		if (!build.getWorkspace().exists()) {
			build.getWorkspace().mkdirs();
		}

		Node node1 = build.getBuiltOn();
		Computer Comp = node1.toComputer();
		String NodeName = build.getBuiltOnStr();
    /*    if  (Comp.getChannel()==null)
        {
        	log.println("Can not get slave infomration wait for 10 sec \n");
        	Thread.sleep(10000);
        	 if (Comp.getChannel()==null)
        	 {
        		 log.println("Waited long enough to get slave information exiting discheck for now \n");
        	 System.exit(0);
        	 }
        	 
        }
        */
		
        if ( DiskSpaceMonitor.DESCRIPTOR.get(Comp)== null )
        {   log.println("No Slave Data available trying to get data from slave");
            Thread.sleep(10000);
            if ( DiskSpaceMonitor.DESCRIPTOR.get(Comp)== null )
            	
        	log.println(" Could not get Slave Information , Exiting Disk check for this slave");
        	System.exit(0);
        }
        
		long size = build.getWorkspace().getParent().getUsableDiskSpace();
		int roundedSize = (int) (size / (1024 * 1024 * 1024));
		log.println("Total Disk Space Available is: " + roundedSize + "Gb");

		if (build.getBuiltOnStr() == "") {
			NodeName = "master";
		}

		log.println(" Node Name: " + NodeName);
	
		if (PluginImpl.getInstance().isDiskrecyclerenabled()) {
			if (roundedSize < SpaceThreshold) {
                                int clearUntil = PluginImpl.getInstance().getClearuntil();
                                if (clearUntil < SpaceThreshold){
                                    throw new AbortException(
					"Your space clearing thresholds are the wrong way around."
                                                + " Edit them and run this job again.");
                                }
                                // Not enough room on slave, start clearing until threshhold met.
                                while(roundedSize < clearUntil){
                                FilePath lastUsedDir = lastFileModified(log, build.getWorkspace().getParent());
                                if (lastUsedDir == null){
                                    throw new AbortException(
					"There is nothing left in the workspace, but room still needs to be made."
                                                + " Either the thresholds are wrong, or there is a major issue on the slave.");
                                }
                                log.println("Workspace selected was " + lastUsedDir.getRemote() + ". Goodbye!");
				// Use Jenkins internal api to delete files recursively.
				// On any errors the exception will bubble up causing the
				// build to fail.
				lastUsedDir.deleteRecursive();    
                                size = build.getWorkspace().getParent().getUsableDiskSpace();
                                roundedSize = (int) (size / (1024 * 1024 * 1024));
                                log.println("New disk space available is: " + roundedSize);
                        }
			}
		}

		log.println("Running Prebuild steps");
		if (roundedSize < SpaceThreshold
				&& !(PluginImpl.getInstance().isDiskrecyclerenabled())) {
			throw new AbortException(
					"Disk Space is too low please look into it before starting a build");

		}
	}

	
	@Extension
	public static final class DescriptorImpl extends Descriptor<BuildWrapper> {
	
		/**
		 * This human readable name is used in the configuration screen.
		 */
		public String getDisplayName() {
				return "Check Disk Space";
		}
		
		
		
		public DescriptorImpl() {
            load();
        }
		

	}
	
	
	class NoopEnv extends Environment {
	}
}
