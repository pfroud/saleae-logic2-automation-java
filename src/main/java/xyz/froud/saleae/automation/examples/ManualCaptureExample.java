package xyz.froud.saleae.automation.examples;

import xyz.froud.saleae.automation.Capture;
import xyz.froud.saleae.automation.Capture.AnalyzerHandle;
import xyz.froud.saleae.automation.Capture.AnalyzerSettings;
import xyz.froud.saleae.automation.Manager;
import xyz.froud.saleae.automation.Manager.CaptureConfigManual;
import xyz.froud.saleae.automation.Manager.CaptureConfigTimed;
import xyz.froud.saleae.automation.Manager.DeviceConfig;

import java.util.List;

public class ManualCaptureExample {

    private final static String ID_OF_LOGIC_PRO_16_DEMO_DEVICE = "F4241";

    public static void main(String[] args) {
        try (Manager manager = new Manager()) {

            final DeviceConfig deviceConfig = new DeviceConfig();
            deviceConfig.digitalChannels = List.of(0);
            deviceConfig.digitalSampleRate = 10_000_000;

            final CaptureConfigManual captureConfig = new CaptureConfigManual();

            final Capture capture = manager.startCapture(ID_OF_LOGIC_PRO_16_DEMO_DEVICE, deviceConfig, captureConfig);

            try{
                Thread.sleep(1000);
            }catch (InterruptedException ignore){}

            capture.stop();

            // you may now do stuff with the Capture object


        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
