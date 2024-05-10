package xyz.froud.saleae.automation.examples;

import saleae.Device;
import xyz.froud.saleae.automation.Manager;

import java.util.List;

public class ListDevicesExample {

    public static void main(String[] args) {
        try (Manager manager = new Manager()) {
            final List<Device> devices = manager.getDevices(true);
            if (devices.isEmpty()) {
                System.out.println("No devices found!");
                return;
            }
            System.out.printf("Found %d device(s):\n", devices.size());
            for (int i = 0; i < devices.size(); i++) {
                final Device device = devices.get(i);
                System.out.printf("Device %d / %d: isSimulated %b; type %s; ID \"%s\"\n",
                        i + 1, devices.size(),
                        device.getIsSimulation(),
                        device.getDeviceType().name(),
                        device.getDeviceId()
                );
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
