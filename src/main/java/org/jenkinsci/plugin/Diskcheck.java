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
 * @author Manoj Thakkar
 * @author Frano Stanisic (8th Decemeber 2015, updated deletion to use Jenkins API/only delete files up to a threshhold)
 * 
 */
public class Diskcheck extends BuildWrapper {

    public final boolean failOnError;

    public static FilePath lastFileModified(PrintStream log, FilePath dir) throws UnknownHostException, IOException, InterruptedException {
        log.println("Determining file to delete ...");
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
     */
    @DataBoundConstructor
    public Diskcheck(boolean failOnError) {
        this.failOnError = failOnError;
    }

    /**
     * Overridden setup returns a noop class as we don't want to add anything
     * here.
     *
     * @return noop Environment class
     */
    @Override
    public Environment setUp(AbstractBuild build, Launcher launcher, BuildListener listener) throws IOException, InterruptedException {
        return new NoopEnv();
    }

    /**
     * Overridden precheckout step, this is where we do all the work.
     *
     * Checks to make sure we have some buildsteps set, and then calls the
     * prebuild and perform on all of them.
     *
     * TODO handle build steps failure in some sort of reasonable way
     *
     */
    @Override
    public Descriptor getDescriptor() {
        return (Descriptor) super.getDescriptor();
    }

    @Override
    public void preCheckout(AbstractBuild build, Launcher launcher,
            BuildListener listener) throws IOException, InterruptedException {
        PrintStream log = listener.getLogger();
        // Default value of disk space check is 1GB		
        int SpaceThreshold = PluginImpl.getInstance().getSpacecheck();

        log.println("Disk space upper threshold is set to :" + SpaceThreshold + "GB");
        log.println("Checking disk space now ...");

        // Touch workspace so that it is created on first time.
        FilePath workspacePath = build.getWorkspace();
        if (workspacePath == null) {
            return;
        }
        if (!workspacePath.exists()) {
            workspacePath.mkdirs();
        }

        Node node1 = build.getBuiltOn();
        if (node1 == null) {
            return;
        }
        Computer Comp = node1.toComputer();
        String NodeName = build.getBuiltOnStr();

        // This new implementation doesn't even touch this, but given I
        // am not 100% on what it does I have just left it here.
        if (DiskSpaceMonitor.DESCRIPTOR.get(Comp) == null) {
            log.println("No slave data available, waiting and retrying ...");
            Thread.sleep(10000);
            if (DiskSpaceMonitor.DESCRIPTOR.get(Comp) == null) {
                log.println("Could not retrieve slave data, exiting disk check for this slave.");
            }
            return;
        }
        // End unsure bit.

        long size = workspacePath.getParent().getUsableDiskSpace();
        int roundedSize = (int) (size / (1024 * 1024 * 1024));
        log.println("Total disk space available is: " + roundedSize + "GB");

        if ("".equals(NodeName)) {
            NodeName = "master";
        }

        log.println("Node Name: " + NodeName);
        boolean diskrecyclerenabled = PluginImpl.getInstance().isDiskrecyclerenabled();
        if (diskrecyclerenabled) {
            if (roundedSize < SpaceThreshold) {
                int clearUntil = PluginImpl.getInstance().getClearuntil();
                if (clearUntil == 0) {
                    log.println("clearUntil set to 0, clearing everything ...");
                    // Jenkins api recursive delete, errors will cause a build failure.
                    workspacePath.getParent().deleteRecursive();
                    log.println("All workspaces deleted successfully.");
                } else {
                    if (clearUntil < SpaceThreshold) {
                        throw new AbortException(
                                "Your space clearing thresholds are the wrong way around."
                                + " Edit them and run this job again.");
                    }
                    // Not enough room on slave, start clearing until threshhold met.
                    while (roundedSize < clearUntil) {
                        FilePath lastUsedDir = lastFileModified(log, workspacePath.getParent());
                        if (lastUsedDir == null) {
                            throw new AbortException(
                                    "There is nothing left in the workspace, but room still needs to be made."
                                    + " Either the thresholds are wrong, or there is an issue on the slave.");
                        }
                        log.println("Deleting workspace " + lastUsedDir.getRemote() + ".");
                        // Jenkins api recursive delete, errors will cause a build failure.
                        lastUsedDir.deleteRecursive();
                        log.println("Workspace deleted.");
                        size = workspacePath.getParent().getUsableDiskSpace();
                        roundedSize = (int) (size / (1024 * 1024 * 1024));
                        log.println("New disk space available is: " + roundedSize + "GB");
                    }
                }
            }
        }

        log.println("Running prebuild steps ...");
        if (roundedSize < SpaceThreshold
                && !diskrecyclerenabled) {
            throw new AbortException(
                    "Disk space is too low, investigate prior to starting another build.");

        }
    }

    @Extension
    public static final class DescriptorImpl extends Descriptor<BuildWrapper> {

        /**
         * This human readable name is used in the configuration screen.
         */
        @Override
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
