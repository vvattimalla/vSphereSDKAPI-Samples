
/**
 * Utility program to find all the vmx files in a datastore and register as VMs.
 * 
 *
 * Copyright (c) 2016
 *
 * Permission is hereby granted, free of charge, to any person
 * obtaining a copy of this software and associated documentation files
 * (the "Software"), to deal in the Software without restriction,
 * including without limitation the rights to use, copy, modify, merge,
 * publish, distribute, sublicense, and/or sell copies of the Software,
 * and to permit persons to whom the Software is furnished to do so,
 * subject to the following conditions:
 *
 * @author Vattimalla Venkatesh (vvattimalla@vmware.com)
 * @version 1.0
 *
 * The above copyright notice and this permission notice shall be
 * included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES
 * OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT
 * HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
 * FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR
 * OTHER DEALINGS IN THE SOFTWARE.
 */




package com.vmware.serchVMXFiles;

import java.net.MalformedURLException;
import java.net.URL;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.List;

import com.vmware.vim25.ArrayOfHostDatastoreBrowserSearchResults;
import com.vmware.vim25.FileInfo;
import com.vmware.vim25.FileQuery;
import com.vmware.vim25.FileQueryFlags;
import com.vmware.vim25.HostDatastoreBrowserSearchResults;
import com.vmware.vim25.HostDatastoreBrowserSearchSpec;
import com.vmware.vim25.TaskInfoState;
import com.vmware.vim25.mo.Datacenter;
import com.vmware.vim25.mo.Datastore;
import com.vmware.vim25.mo.Folder;
import com.vmware.vim25.mo.HostSystem;
import com.vmware.vim25.mo.InventoryNavigator;
import com.vmware.vim25.mo.ManagedEntity;
import com.vmware.vim25.mo.ResourcePool;
import com.vmware.vim25.mo.ServiceInstance;
import com.vmware.vim25.mo.Task;

public class searchVMXFiles {

	public ServiceInstance Initialisation(String url, String username,
			String password) throws RemoteException, MalformedURLException {
		ServiceInstance si = new ServiceInstance(new URL(url), username,
				password, true);
		return si;
	}

	public static void main(String[] args) throws RemoteException,
			MalformedURLException {

		searchVMXFiles search = new searchVMXFiles();
		//Modify the VCIP and credentials
		ServiceInstance vcObject = search.Initialisation(
				"https://10.161.237.222/sdk", "Administrator@vsphere.local",
				"Admin!23");
		System.out.println("VCObject" + vcObject);
		Folder rootFolder = vcObject.getRootFolder();
		// Replace the Datacenter Name
		Datacenter dc = (Datacenter) new InventoryNavigator(rootFolder)
				.searchManagedEntity("Datacenter", "MYDC");
		Folder vmFolder = (Folder) new InventoryNavigator(rootFolder).searchManagedEntity("Folder","vm");
		// Replace the Resourcepool Name
		ResourcePool resourcepool = (ResourcePool) new InventoryNavigator(rootFolder)
		.searchManagedEntity("ResourcePool", "RP");
		ManagedEntity[] hostmanagedEntities = new InventoryNavigator(rootFolder)
				.searchManagedEntities("HostSystem");

		for (ManagedEntity hostmanagedEntity : hostmanagedEntities) {
			HostSystem hostsys = (HostSystem) hostmanagedEntity;
			String ESXhostname = hostsys.getName();
			System.out.println("ESXhostname" + ESXhostname);
			Datastore[] HDS = hostsys.getDatastores();

			List<String> filePaths = new ArrayList<String>();

			for (Datastore ds : HDS) {
				System.out.println(ds.getName());
				HostDatastoreBrowserSearchSpec hostDatastoreBrowserSearchSpec = new HostDatastoreBrowserSearchSpec();

				//below code find the vmx files in datastore and return in the form of ArrayOfHostDatastoreBrowserSearchResults
				hostDatastoreBrowserSearchSpec
						.setQuery(new FileQuery[] { new FileQuery() });
				FileQueryFlags fqf = new FileQueryFlags();
				fqf.setFileOwner(false);
				hostDatastoreBrowserSearchSpec.setDetails(fqf);
				hostDatastoreBrowserSearchSpec.setSearchCaseInsensitive(false);
				hostDatastoreBrowserSearchSpec
						.setMatchPattern(new String[] { "*" });

				Task task = ds.getBrowser().searchDatastoreSubFolders_Task(
						"[" + ds.getName() + "]",
						hostDatastoreBrowserSearchSpec);
				System.out.println(task.getTaskInfo().getResult());
				try {
		            task.waitForTask();
		            if(task.getTaskInfo().getState().equals(task.getTaskInfo().getState().success)){
		            ArrayOfHostDatastoreBrowserSearchResults searchResults =
		                    (ArrayOfHostDatastoreBrowserSearchResults) task.getTaskInfo().getResult();
		            HostDatastoreBrowserSearchResults[] results = searchResults.getHostDatastoreBrowserSearchResults();
		            System.out.println("lenght"+results.length);
		            if (results != null && results.length > 0) {
		            	for(int i=0;i <= results.length;i++){
		                FileInfo[] fileInfo = results[i].getFile();
		                if (fileInfo != null) {
		                	//Below functionality register the vmx file one by one.
		                    for (FileInfo fi : fileInfo) {
		                    	if(fi.path.contains("vmx")){
		                    		System.out.println("[" + ds.getName() + "] "+fi.path.substring(0, fi.path.indexOf("."))+"/"+fi.path);
		                    		String dspath="[" + ds.getName() + "] "+fi.path.substring(0, fi.path.indexOf(".vmx"))+"/"+fi.path;
		                    		Task registerVMTask = vmFolder.registerVM_Task(dspath, fi.path.substring(0, fi.path.indexOf(".")), false, resourcepool, hostsys);
		                    		
		                    		TaskInfoState taskinfostate = registerVMTask.getTaskInfo().getState();
		                    		
		                    		while(!registerVMTask.getTaskInfo().getState().equals(taskinfostate.success)){
		                    			if(registerVMTask.getTaskInfo().getState().equals(taskinfostate.error)){
		                    				System.out.println("Register"+fi.path.substring(0, fi.path.indexOf(".vmx"))+"VM task failed");
		                    				break;
		                    			}else{
		                    				System.out.println("Register"+fi.path.substring(0, fi.path.indexOf(".vmx"))+" task is still running");
		                    			}
		                    			taskinfostate = registerVMTask.getTaskInfo().getState();
		                    		}
		                    		if(registerVMTask.getTaskInfo().getState().equals(taskinfostate.success)){
		                    			System.out.println("Register"+fi.path.substring(0, fi.path.indexOf(".vmx"))+" task is success");
		                    		}
		                    	}
		                        filePaths.add(fi.path);
		                        
		                    }
		                }
		            }
		            }
		        } else{
		        	System.out.println("Got error in seaching vmx files");
		        }
			}
				catch (Exception e) {
		            
		        }

			}

		}

	}

}
