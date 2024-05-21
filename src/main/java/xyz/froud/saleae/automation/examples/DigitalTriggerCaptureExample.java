package xyz.froud.saleae.automation.examples;

import saleae.DigitalTriggerType;
import xyz.froud.saleae.automation.Capture;
import xyz.froud.saleae.automation.Capture.AnalyzerHandle;
import xyz.froud.saleae.automation.Capture.AnalyzerSettings;
import xyz.froud.saleae.automation.Manager;
import xyz.froud.saleae.automation.Manager.CaptureConfigDigitalTrigger;
import xyz.froud.saleae.automation.Manager.CaptureConfigTimed;
import xyz.froud.saleae.automation.Manager.DeviceConfig;

import java.util.List;

public class DigitalTriggerCaptureExample {

    private final static String ID_OF_LOGIC_PRO_16_DEMO_DEVICE = "F4241";

    public static void main(String[] args) {
        try (Manager manager = new Manager()) {

            final DeviceConfig deviceConfig = new DeviceConfig();
            deviceConfig.digitalChannels = List.of(0);
            deviceConfig.digitalSampleRate = 10_000_000;

            final CaptureConfigDigitalTrigger captureConfig = new CaptureConfigDigitalTrigger();
            captureConfig.digitalTriggerType = DigitalTriggerType.DIGITAL_TRIGGER_TYPE_RISING;
            captureConfig.triggerChannelIndex = 0;

            /*
            The simulated data goes up and down a lot so the trigger will happen
            almost immediately.
             */
            final Capture capture = manager.startCapture(ID_OF_LOGIC_PRO_16_DEMO_DEVICE, deviceConfig, captureConfig);

            // you may now do stuff with the Capture object

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
