package org.jenkinsci.plugin;

import hudson.Plugin;
import hudson.model.*;
import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.model.Descriptor.FormException;
import hudson.model.Hudson;

import java.io.IOException;

import javax.servlet.ServletException;

import net.sf.json.JSONObject;

import org.kohsuke.stapler.StaplerRequest;

//import com.innorium.crystalline.plugins.model.ScannerJobProperty;

public class PluginImpl extends Plugin{
	private boolean diskrecyclerenabled;
	private int spacecheck;
        private int clearuntil;
	
	 /**
     * Returns the singleton instance.
     *
     * @return the one.
     */
    public static PluginImpl getInstance() {
        return Hudson.getInstance().getPlugin(PluginImpl.class);
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
		 formData=formData.getJSONObject("disk-check");
		 spacecheck=formData.getInt("spacecheck");
                 clearuntil=formData.getInt("clearuntil");
		 diskrecyclerenabled=formData.getBoolean("diskrecyclerenabled");
		
		save();
		super.configure(req, formData);
	    
	}
	
	

	public void setDiskrecyclerenabled(boolean diskrecyclerenabled) {
	this.diskrecyclerenabled = diskrecyclerenabled;
}
	
	public int getSpacecheck()
	{
		return spacecheck;
	}
	
	public void setSpacecheck(int spaceCheck) 
	{
	this.spacecheck = spaceCheck;
}
        public int getClearuntil()
	{
		return clearuntil;
	}
	
	public void setClearuntil(int clearUntil) 
	{
	this.clearuntil = clearUntil;
}

	public boolean isDiskrecyclerenabled() {
		return diskrecyclerenabled;
	}
}