package xyz.froud.saleae.automation.examples;

import saleae.Device;
import xyz.froud.saleae.automation.Manager;

import java.util.List;

public class ListDevicesExample {

    public static void main(String[] args) {
        try (Manager manager = new Manager()) {

            final boolean includeSimulationDevices = true;
            final List<Device> devices = manager.getDevices(includeSimulationDevices);

            if (devices.isEmpty()) {
                System.out.println("No devices found!");

            } else {
                System.out.printf("Found %d device(s):\n", devices.size());
                for (int i = 0; i < devices.size(); i++) {
                    final Device device = devices.get(i);
                    System.out.printf("Device %d / %d: isSimulation %b; type %s; ID \"%s\"\n",
                            i + 1, devices.size(),
                            device.getIsSimulation(),
                            device.getDeviceType().name(),
                            device.getDeviceId()
                    );
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
