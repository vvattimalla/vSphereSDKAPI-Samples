
/**
 * Utility program to deatach ISO on Powered on VM.
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

import com.vmware.vim25.Description;
import com.vmware.vim25.TaskInfo;
import com.vmware.vim25.TaskInfoState;
import com.vmware.vim25.VirtualCdrom;
import com.vmware.vim25.VirtualCdromRemotePassthroughBackingInfo;
import com.vmware.vim25.VirtualDevice;
import com.vmware.vim25.VirtualDeviceConfigSpec;
import com.vmware.vim25.VirtualDeviceConfigSpecOperation;
import com.vmware.vim25.VirtualMachineConfigSpec;
import com.vmware.vim25.VirtualMachineMessage;
import com.vmware.vim25.VirtualMachineQuestionInfo;
import com.vmware.vim25.mo.Folder;
import com.vmware.vim25.mo.InventoryNavigator;
import com.vmware.vim25.mo.ServiceInstance;
import com.vmware.vim25.mo.Task;
import com.vmware.vim25.mo.VirtualMachine;

public class detachISO {

	static String vcURL = "https://10.10.10.10/sdk";
	static String vcUsername = "root";
	static String vcPassword = "password";
	static String dataCenter = "vcqaDC";
	static String targetVMName = "thin-ovf";

	public ServiceInstance Initialisation(String url, String username,
			String password) throws RemoteException, MalformedURLException {
		ServiceInstance si = new ServiceInstance(new URL(url), username,
				password, true);
		return si;
	}

	public static void main(String[] args) throws RemoteException,
			MalformedURLException, InterruptedException {

		detachISO detachISO = new detachISO();

		ServiceInstance vcObject = detachISO.Initialisation(vcURL, vcUsername,
				vcPassword);
		System.out.println("VCObject" + vcObject);
		// traverse to Root Folder
		Folder rootFolder = vcObject.getRootFolder();
		//Get the Virtual Machine with Specified name
		VirtualMachine targetVM = (VirtualMachine) new InventoryNavigator(
				rootFolder).searchManagedEntity("VirtualMachine", targetVMName);

		String cdromName = "CD/DVD Drive 1";
		VirtualDevice cdrom = null;
		if (targetVM!= null) {
				VirtualMachine virtualMachine = (VirtualMachine) targetVM;
				VirtualDevice[] virutualDeviceList = virtualMachine.getConfig()
						.getHardware().getDevice();
				for (VirtualDevice virtualDevice : virutualDeviceList) {
					if (virtualDevice instanceof VirtualCdrom) {
						Description info = virtualDevice.getDeviceInfo();
						if (info != null) {
							if (info.getLabel().equalsIgnoreCase(cdromName)) {
								cdrom = virtualDevice;
								break;
							}
						}
					}
				}
				System.out.println(cdrom.getDeviceInfo().label);
				
				VirtualDeviceConfigSpec deviceSpec = new VirtualDeviceConfigSpec();
				deviceSpec.setOperation(VirtualDeviceConfigSpecOperation.edit);
				deviceSpec.setFileOperation(null);
				
				VirtualCdromRemotePassthroughBackingInfo  passthrough = new VirtualCdromRemotePassthroughBackingInfo ();
				passthrough.setDeviceName("");
				
				cdrom.setBacking(passthrough);
				deviceSpec.setDevice(cdrom);
				
				VirtualMachineConfigSpec virtualMachineConfigSpec = new VirtualMachineConfigSpec();
				virtualMachineConfigSpec.setDeviceChange(new VirtualDeviceConfigSpec[]{deviceSpec});
				Task reConfigTask = virtualMachine.reconfigVM_Task(virtualMachineConfigSpec);
				reConfigTask.waitForTask();
				if ((reConfigTask.getTaskInfo().getState())
						.equals(TaskInfoState.success)) {
					System.out.println("Successfully reconfigured VM By Removing Exisisting Iso");
					
				} else if ((reConfigTask.getTaskInfo()
						.getState())
						.equals(TaskInfoState.error)) {
					System.out
							.println("Detach Iso failed");
				}
		} else {
			System.out.println("No Virtual Machines found in the Inventory");
			vcObject.getServerConnection().logout();
		}
	}

}
