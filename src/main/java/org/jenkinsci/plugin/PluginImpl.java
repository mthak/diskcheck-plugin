package org.jenkinsci.plugin;

import hudson.AbortException;
import hudson.Plugin;
import hudson.model.Descriptor.FormException;

import java.io.IOException;

import javax.servlet.ServletException;
import jenkins.model.Jenkins;

import net.sf.json.JSONObject;

import org.kohsuke.stapler.StaplerRequest;

public class PluginImpl extends Plugin {

    private boolean diskrecyclerenabled;
    private int spacecheck;
    private int clearuntil;

    /**
     * Returns the singleton instance.
     *
     * @return the one.
     * @throws hudson.AbortException
     * If Jenkins instance is not ready
     */
    public static PluginImpl getInstance() throws AbortException {
        // To remove warning. I don't think this would ever be null in our case.
        Jenkins instance = Jenkins.getInstance();
        if (instance == null) {
            throw new AbortException(
                    "Can't access Jenkins instance, it may not be ready.");
        }
        return instance.getPlugin(PluginImpl.class);
    }

    @Override
    public void start() throws Exception {
        super.start();
        load();
    }

    @Override
    public void stop() throws Exception {
        super.stop();
    }

    @Override
    public void configure(StaplerRequest req, JSONObject formData)
            throws IOException, ServletException, FormException {
        formData = formData.getJSONObject("disk-check");
        spacecheck = formData.getInt("spacecheck");
        clearuntil = formData.getInt("clearuntil");
        diskrecyclerenabled = formData.getBoolean("diskrecyclerenabled");

        save();
        super.configure(req, formData);

    }

    public void setDiskrecyclerenabled(boolean diskrecyclerenabled) {
        this.diskrecyclerenabled = diskrecyclerenabled;
    }

    public int getSpacecheck() {
        return spacecheck;
    }

    public void setSpacecheck(int spaceCheck) {
        this.spacecheck = spaceCheck;
    }

    public int getClearuntil() {
        return clearuntil;
    }

    public void setClearuntil(int clearUntil) {
        this.clearuntil = clearUntil;
    }

    public boolean isDiskrecyclerenabled() {
        return diskrecyclerenabled;
    }
}
