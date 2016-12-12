/**
 * Utility program to Search VMS in the Inventory based on VM properties like Number of CPU and Guest OS Name.
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

import com.vmware.vim25.mo.Datacenter;
import com.vmware.vim25.mo.Folder;
import com.vmware.vim25.mo.InventoryNavigator;
import com.vmware.vim25.mo.ManagedEntity;
import com.vmware.vim25.mo.ServiceInstance;
import com.vmware.vim25.mo.VirtualMachine;

public class VMSearchFilter {

	static String vcURL = "https://10.199.100.100/sdk";
	static String vcUsername = "VCUsername";
	static String vcPassword = "VCPassword";
	static String dataCenter = "vcqaDC";

	public ServiceInstance Initialisation(String url, String username,
			String password) throws RemoteException, MalformedURLException {
		ServiceInstance si = new ServiceInstance(new URL(url), username,
				password, true);
		return si;
	}
	//This method search Virtual machines by number of CPU and guest OS provide by User
    //Note: Please provide proper guest OS name in the Property Value
	public static List<VirtualMachine> searchVMSByProperty(ManagedEntity[] vms,String vmPropertyType,String vmPropertyValue){
		
		List<VirtualMachine> searchResults = new ArrayList<VirtualMachine>();
		
		for (ManagedEntity vm : vms) {
			VirtualMachine virtualMachine = (VirtualMachine) vm;
			if(vmPropertyType.contains("CPU")){
				if ((virtualMachine.getConfig().hardware.numCPU != 0)&& (virtualMachine.getConfig().hardware.numCPU == Integer.parseInt(vmPropertyValue))) {
				    searchResults.add(virtualMachine);
			   }
			}else if(vmPropertyType.contains("Guest OS")){
				
				if ((virtualMachine.getConfig().guestFullName != null)&& (virtualMachine.getConfig().guestFullName.contains(vmPropertyValue))){
				    searchResults.add(virtualMachine);
			   }
			}
		}
		
		return searchResults;
		
	}

	public static void main(String[] args) throws RemoteException,
			MalformedURLException {

		VMSearchFilter vmFilter = new VMSearchFilter();
		// It gives the VC Connection Object.Replace the VCIP,Username and
		// Password.
		ServiceInstance vcObject = vmFilter.Initialisation(vcURL, vcUsername,
				vcPassword);
		System.out.println("VCObject" + vcObject);
		// traverse to Root Folder
		Folder rootFolder = vcObject.getRootFolder();
		// Move to the data center by replacing the MYDC
		Datacenter dc = (Datacenter) new InventoryNavigator(rootFolder)
				.searchManagedEntity("Datacenter", dataCenter);
		ManagedEntity[] vms = new InventoryNavigator(rootFolder).searchManagedEntities("VirtualMachine");
		List<VirtualMachine> vmsByNumberofCPU =  new ArrayList<VirtualMachine>();
		List<VirtualMachine> vmsByGuestOS =  new ArrayList<VirtualMachine>();
		if (vms != null) {
			//Seaching Vms by Number of CPU
			vmsByNumberofCPU= searchVMSByProperty(vms, "CPU", "2");
			//Searching Vms by Guest OS  Windows  or CentOS 4/5
			vmsByGuestOS= searchVMSByProperty(vms, "Guest OS", "Windows");
			//Iterating through VMs List
			if(vmsByNumberofCPU!=null){
				for(VirtualMachine vm: vmsByNumberofCPU){
					System.out.println(vm.getConfig().getName());
				}
			}else{
				System.out.println("No Virtual Machines found for the given search criteria");
				vcObject.getServerConnection().logout();
			}
			//Iterating through VMs List 
			if(vmsByGuestOS!=null){
				for(VirtualMachine vm: vmsByGuestOS){
					System.out.println(vm.getConfig().getName());
				}
			}else{
				System.out.println("No Virtual Machines found for the given search criteria");
				vcObject.getServerConnection().logout();
			}
			
		} else {
			System.out.println("No Virtual Machines found in the Inventory");
			vcObject.getServerConnection().logout();
		}

	}

}
