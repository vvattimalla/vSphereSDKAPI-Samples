/**
 * Utility program to Add existing disk to Powered on VM from different folder.
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

package com.vmware.samples;

import java.net.MalformedURLException;
import java.net.URL;
import java.rmi.RemoteException;

import com.vmware.vim25.TaskInfoState;
import com.vmware.vim25.VirtualDevice;
import com.vmware.vim25.VirtualDeviceConfigSpec;
import com.vmware.vim25.VirtualDeviceConfigSpecOperation;
import com.vmware.vim25.VirtualDeviceFileBackingInfo;
import com.vmware.vim25.VirtualDisk;
import com.vmware.vim25.VirtualMachineConfigInfo;
import com.vmware.vim25.VirtualMachineConfigSpec;
import com.vmware.vim25.mo.Datacenter;
import com.vmware.vim25.mo.Folder;
import com.vmware.vim25.mo.HostSystem;
import com.vmware.vim25.mo.InventoryNavigator;
import com.vmware.vim25.mo.ManagedEntity;
import com.vmware.vim25.mo.ServiceInstance;
import com.vmware.vim25.mo.Task;
import com.vmware.vim25.mo.VirtualMachine;

public class HotAddExistingDisk {

	static String IPAddress = "https://10.10.10.1/sdk"; // VC
															// ipaddress/hostname
	static String userName = "Administrator@vsphere.local"; // VC username
	static String passwd = "password"; // VC password
	static String sourceVMName = "SourceVM"; // Provide the sourceVM name its
												// disk is attached to targetVM
	static String targetVMName = "TargetVM"; // VM name to which disk is
												// attached
	static String hostIp = "10.10.10.2";
	//static String diskPath = "[]vmfs/volumes/sharedVmfs-2/SourceVM/SourceVM.vmdk";

	public ServiceInstance Initialisation(String url, String username,
			String password) throws RemoteException, MalformedURLException {
		ServiceInstance si = new ServiceInstance(new URL(url), username,
				password, true);
		return si;
	}

	public static void main(String[] args) throws RemoteException,
			MalformedURLException {

		int ckey = 0;
		int unitNumber = 0;
		int diskCount = 1;

		HotAddExistingDisk hotAddDisk = new HotAddExistingDisk();
		ServiceInstance vcObject = hotAddDisk.Initialisation(IPAddress,
				userName, passwd);
		Folder rootFolder = vcObject.getRootFolder();
		Datacenter dc = (Datacenter) new InventoryNavigator(rootFolder)
				.searchManagedEntity("Datacenter", "MYDC");
		VirtualMachine targetVM = (VirtualMachine) new InventoryNavigator(
				rootFolder).searchManagedEntity("VirtualMachine", targetVMName);
		VirtualMachine sourceVM = (VirtualMachine) new InventoryNavigator(
				rootFolder).searchManagedEntity("VirtualMachine", sourceVMName);
		ManagedEntity hostmanagedEntitity = new InventoryNavigator(rootFolder)
				.searchManagedEntity("HostSystem", hostIp);
		VirtualMachineConfigSpec vmConfigSpec = new VirtualMachineConfigSpec();
		VirtualDeviceConfigSpec diskSpec = new VirtualDeviceConfigSpec();

		if (targetVM != null) {
			if (hostmanagedEntitity != null) {
				Task powerOnTask = targetVM
						.powerOnVM_Task((HostSystem) hostmanagedEntitity);
				try {
					powerOnTask.waitForTask();
					if (powerOnTask.getTaskInfo().getState()
							.equals(powerOnTask.getTaskInfo().state.success)) {
						VirtualMachineConfigInfo vmConfigInfo = targetVM
								.getConfig();
						VirtualDevice[] virtualDevices = vmConfigInfo
								.getHardware().getDevice();
						for (VirtualDevice virutalDevice : virtualDevices) {
							if (virutalDevice.getDeviceInfo().getLabel()
									.equalsIgnoreCase("SCSI Controller 0")) {
								ckey = virutalDevice.getKey();
							} else if ((virutalDevice.getDeviceInfo()
									.getLabel().equalsIgnoreCase("Hard disk 1"))) {
								diskCount++;
							}
						}
						// Get the Source VM Disk
						VirtualMachineConfigInfo vmci = sourceVM.getConfig();
						VirtualDevice[] devices = vmci.getHardware()
								.getDevice();
						VirtualDisk theDisk = null;
						for (int i = 0; devices != null && i < devices.length; i++) {
							if (devices[i].getDeviceInfo().getLabel()
									.equals("Hard disk 1")) {
								theDisk = (VirtualDisk) devices[i];
							}
						}
						if (theDisk == null) {
							System.out.println("Disk not found");
							return;
						} else {
							unitNumber = (diskCount != 7) ? diskCount: diskCount + 1;
							diskSpec.setDevice(theDisk);
							diskSpec.setOperation(VirtualDeviceConfigSpecOperation.add);
							diskSpec.setFileOperation(null);
							if (diskSpec != null) {
								vmConfigSpec.setDeviceChange(new VirtualDeviceConfigSpec[] { diskSpec });
								vmConfigSpec.getDeviceChange()[0].getDevice().setControllerKey(ckey);
								vmConfigSpec.getDeviceChange()[0].getDevice().setUnitNumber(unitNumber);
								Task reConfigTask = targetVM
										.reconfigVM_Task(vmConfigSpec);
								if ((reConfigTask.getTaskInfo().getState())
										.equals(TaskInfoState.success)) {
									System.out
											.println("Successfully reconfigured VM with Exsisitng Disk");
								} else if ((reConfigTask.getTaskInfo()
										.getState())
										.equals(TaskInfoState.error)) {
									System.out
											.println("Hot add operaion Failed");
								}
							}
						}
					}

				} catch (InterruptedException e) {
					e.printStackTrace();
				}

			} else {
				System.out.println("Host not found");
			}
		} else {
			System.out.println("VM with given name not Found");
		}

	}

}
