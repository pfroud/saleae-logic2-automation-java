package xyz.froud.saleae.automation.examples;

import saleae.AppInfo;
import saleae.Device;
import saleae.Version;
import xyz.froud.saleae.automation.Manager;

import java.util.List;

public class GetAppInfoExample {

    public static void main(String[] args) {
        try (Manager manager = new Manager()) {

            final AppInfo appInfo = manager.getAppInfo();

            System.out.printf("Application version: %s\n", appInfo.getApplicationVersion());
            final Version apiVersion = appInfo.getApiVersion();
            System.out.printf("        API version: %d.%d.%d\n", apiVersion.getMajor(), apiVersion.getMinor(), apiVersion.getPatch());
            System.out.printf("         Process ID: %d\n", appInfo.getLaunchPid());

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
