package com.vmware.samples;

import java.net.MalformedURLException;
import java.net.URL;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.List;

import com.vmware.vim25.HostConnectSpec;
import com.vmware.vim25.TaskInfoState;
import com.vmware.vim25.VirtualMachinePowerState;
import com.vmware.vim25.mo.Folder;
import com.vmware.vim25.mo.HostSystem;
import com.vmware.vim25.mo.InventoryNavigator;
import com.vmware.vim25.mo.ManagedEntity;
import com.vmware.vim25.mo.ServiceInstance;
import com.vmware.vim25.mo.Task;
import com.vmware.vim25.mo.VirtualMachine;

public class HostOps {

	static String vcURL = "https://10.10.10.1/sdk";
	static String vcUsername = "Administrator@vsphere.local";
	static String vcPassword = "password";
	static String dataCenter = "vcqaDC";
	static String hostIp = "10.10.10.2";

	public ServiceInstance Initialisation(String url, String username,
			String password) throws RemoteException, MalformedURLException {
		ServiceInstance si = new ServiceInstance(new URL(url), username,
				password, true);
		return si;
	}

	public static void main(String[] args) throws RemoteException, MalformedURLException, InterruptedException {
		
		HostOps hostOps = new HostOps();
		ServiceInstance vcObject = hostOps.Initialisation(vcURL, vcUsername,vcPassword);
		System.out.println("VCObject" + vcObject);
		Folder rootFolder = vcObject.getRootFolder();
		ManagedEntity hostmanagedEntity = new InventoryNavigator(rootFolder).searchManagedEntity("HostSystem", hostIp);
		HostSystem hostsys = (HostSystem) hostmanagedEntity;
		//get all the vms on host and check all the vms are in poweredoff and suspend state 
		VirtualMachine[] vms  =  hostsys.getVms();
		List<VirtualMachine> suspendVMs = new ArrayList<VirtualMachine>();
		if(vms.length>0){
		for(VirtualMachine vm :  vms){
			if(vm.getRuntime().powerState.equals(VirtualMachinePowerState.poweredOn))
			{
				Task suspendVmTask = vm.suspendVM_Task();
				suspendVmTask.waitForTask();
				if(suspendVmTask.getTaskInfo().getState().equals(TaskInfoState.success)){
					System.out.println("vm suspend successfully");
					suspendVMs.add(vm);
				}else{
					System.out.println("one of the VM failed to enter suspended state");
				  return;
				}		
			}
		}
		}
		//enter and exit maintainance mode
	    Task enterMaintainanceTask = 	hostsys.enterMaintenanceMode(100, false);
	    enterMaintainanceTask.waitForTask();
	    if(enterMaintainanceTask.getTaskInfo().getState().equals(TaskInfoState.success)){
	    	System.out.println("Host is succfully entered in to maintaince mode");
	    	//exit maintaince mode 
	    	Task exitMaintainancetask = hostsys.exitMaintenanceMode(100);
	    	exitMaintainancetask.waitForTask();
	    	   if(exitMaintainancetask.getTaskInfo().getState().equals(TaskInfoState.success)){
	    		   System.out.println("Host is succfully exit in to maintaince mode");
	    		   //Move Suspend VMs back to powered on state
	    		  if(suspendVMs.size()>0){
	    			  for(VirtualMachine vm : suspendVMs){
	    				  if(vm.getRuntime().powerState.equals(VirtualMachinePowerState.suspended)){
	    					  Task poweronVmTask = vm.powerOnVM_Task(hostsys);
	    					   poweronVmTask.waitForTask();
	    						if(poweronVmTask.getTaskInfo().getState().equals(TaskInfoState.success)){
	    							System.out.println("vm powered on successfully");
	    						}else{
	    							System.out.println("one of the VM failed to poweredon");
	    						}		
	    				  }
	    			  }
	    		  }
	    		   
	    	   }else{
	    		   System.out.println("Host is not exited from maintaince mode");
	    	   }
	    }else{
	    	System.out.println("Host is not  entered in to maintaince mode");
	    }
		//disconnect and reconnect host to VC
	    Task diskConnectTask = hostsys.disconnectHost();
	    diskConnectTask.waitForTask();
	    if(diskConnectTask.getTaskInfo().getState().equals(TaskInfoState.success)){
	    	System.out.println("Host disconnected successfully");
	    	HostConnectSpec  hcs = new HostConnectSpec();
	    	 Task reConnectTask = hostsys.reconnectHost_Task(hcs);
	    	 reConnectTask.waitForTask();
	    	if(reConnectTask.getTaskInfo().getState().equals(TaskInfoState.success)){
	    		System.out.println("Host reconnected successfully");
	    	}
	    }
	}
}
