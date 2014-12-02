package com.cloudera.recycler;

import hudson.Plugin;
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
		diskrecyclerenabled=formData.getBoolean("diskrecyclerenabled");
		spacecheck=formData.getInt("spacecheck");
		save();
		super.configure(req, formData);
	}
	
//    @SuppressWarnings("rawtypes")
//	public static boolean shouldScan(AbstractBuild build) {
//        return shouldScan(build.getProject());
//    }
//
//    @SuppressWarnings({ "rawtypes", "unchecked" })
//	public static boolean shouldScan(AbstractProject project) {
//        if (getInstance().isdiskrecyclerenabled()) {
//      //      ScannerJobProperty property = (ScannerJobProperty)project.getProperty(ScannerJobProperty.class);
//        //    if (property != null) {
//        //        return !property.isDoNotScan();
//        //    } else {
//        //        return true;
//      //      }
//        } else {
//            return false;
//        }
//    }
	

	public void setDiskrecyclerenabled(boolean diskrecyclerenabled) {
	this.diskrecyclerenabled = diskrecyclerenabled;
}
	
	public Integer spacecheck()
	{
		return spacecheck;
	}

	public boolean isDiskrecyclerenabled() {
		return diskrecyclerenabled;
	}
}