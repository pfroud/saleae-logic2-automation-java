package xyz.froud.saleae.automation;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import saleae.AppInfo;
import saleae.CaptureConfiguration;
import saleae.Device;
import saleae.DigitalTriggerCaptureMode;
import saleae.DigitalTriggerLinkedChannel;
import saleae.DigitalTriggerLinkedChannelState;
import saleae.DigitalTriggerType;
import saleae.GetAppInfoReply;
import saleae.GetAppInfoRequest;
import saleae.GetDevicesReply;
import saleae.GetDevicesRequest;
import saleae.GlitchFilterEntry;
import saleae.LoadCaptureReply;
import saleae.LoadCaptureRequest;
import saleae.LogicChannels;
import saleae.LogicDeviceConfiguration;
import saleae.ManagerGrpc;
import saleae.ManagerGrpc.ManagerBlockingStub;
import saleae.ManualCaptureMode;
import saleae.StartCaptureReply;
import saleae.StartCaptureRequest;
import saleae.ThisApiVersion;
import saleae.TimedCaptureMode;

import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Java port of <a
 * href="https://github.com/saleae/logic2-automation/blob/develop/python/saleae/automation/manager.py">manager.py</a>.
 */
public class Manager implements AutoCloseable {

    private static final String DEFAULT_GRPC_ADDRESS = "127.0.0.1";
    private static final int DEFAULT_GRPC_PORT = 10430;

    private final ManagedChannel CHANNEL;

    final ManagerBlockingStub STUB;

    public static void main(String[] args) {
        try (Manager manager = new Manager()) {

            System.out.println(manager.getDevices(true));

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Try to connect to a running instance of the Logic 2 software using the default host and port.
     */
    public Manager() throws IncompatibleApiVersionException {
        this(DEFAULT_GRPC_ADDRESS, DEFAULT_GRPC_PORT);
    }

    /**
     * Try to connect to a running instance of the Logic 2 software using the specified host and port.
     */
    public Manager(String host, int port) throws IncompatibleApiVersionException {
        CHANNEL = ManagedChannelBuilder.forAddress(host, port).usePlaintext().build();
        STUB = ManagerGrpc.newBlockingStub(CHANNEL);

        final AppInfo appInfo = getAppInfo();
        final saleae.Version appApiVersion = appInfo.getApiVersion();
        if (ThisApiVersion.THIS_API_VERSION_MAJOR.getNumber() != appApiVersion.getMajor()) {
            try {
                close();
            } catch (InterruptedException ignore) {
            }
            throw new IncompatibleApiVersionException(appApiVersion);
        }
        System.out.printf("Connected to Logic version %s with PID %d.\n", appInfo.getApplicationVersion(), appInfo.getLaunchPid());

    }

    /**
     * Get information about the connected Logic 2 instance.
     */
    public AppInfo getAppInfo() {
        final GetAppInfoRequest request = GetAppInfoRequest.newBuilder().build();
        final GetAppInfoReply reply = STUB.getAppInfo(request);
        return reply.getAppInfo();
    }

    /**
     * Returns a list of devices connected to the Logic 2 instance.
     *
     * @param includeSimulationDevices whether to include devices which are simulated inside Logic2.
     */
    public List<Device> getDevices(boolean includeSimulationDevices) {
        final GetDevicesRequest request = GetDevicesRequest.newBuilder()
                .setIncludeSimulationDevices(includeSimulationDevices)
                .build();
        final GetDevicesReply reply = STUB.getDevices(request);
        return reply.getDevicesList();
    }

    public static LogicChannels getLogicChannels(
            List<Integer> digitalChannels,
            List<Integer> analogChannels
    ) {
        return LogicChannels.newBuilder()
                .addAllDigitalChannels(digitalChannels)
                .addAllAnalogChannels(analogChannels)
                .build();
    }

    /**
     * @param digitalThresholdVolts For Pro 8 and Pro 16, this can be one of: 1.2, 1.8, or 3.3. For other devices this
     * is ignored.
     */
    public static LogicDeviceConfiguration getLogicDeviceConfiguration(
            LogicChannels channels,
            int digitalSampleRate,
            int analogSampleRate,
            double digitalThresholdVolts,
            List<GlitchFilterEntry> glitchFilters
    ) {
        return LogicDeviceConfiguration.newBuilder()
                .setLogicChannels(channels)
                .setDigitalSampleRate(digitalSampleRate)
                .setAnalogSampleRate(analogSampleRate)
                .setDigitalThresholdVolts(digitalThresholdVolts)
                .addAllGlitchFilters(glitchFilters)
                .build();
    }

    /**
     * When in manual capture mode, the capture must be manually stopped by calling {@link Capture#stop()}.
     *
     * @param bufferSizeMegabytes This is the maximum number of megabytes allowed for storing data during a capture. *
     * When this limit is reached, the oldest data will be deleted until the total usage is under bufferSizeMegabytes.
     * @param trimDataSeconds Number of seconds to keep after the capture ends. If greater than 0, only the latest
     * `{@code trimDataSeconds} of the capture will be kept, otherwise the data will not be trimmed.
     */
    public static CaptureConfiguration getCaptureConfigurationManual(
            int bufferSizeMegabytes,
            double trimDataSeconds
    ) {
        return CaptureConfiguration.newBuilder()
                .setBufferSizeMegabytes(bufferSizeMegabytes)
                .setManualCaptureMode(
                        ManualCaptureMode.newBuilder()
                                .setTrimDataSeconds(trimDataSeconds)
                )

                .build();
    }

    /**
     * When in timed capture mode, the capture will automatically stop after {@code durationSeconds}.
     *
     * @param bufferSizeMegabytes This is the maximum number of megabytes allowed for storing data during a capture. *
     * When this limit is reached, the capture will be terminated.
     * @param trimDataSeconds Number of seconds to keep after the capture ends. If greater than 0, only the latest
     * `{@code trimDataSeconds} of the capture will be kept, otherwise the data will not be trimmed.
     */
    public static CaptureConfiguration getCaptureConfigurationTimed(
            int bufferSizeMegabytes,
            double durationSeconds,
            double trimDataSeconds
    ) {
        return CaptureConfiguration.newBuilder()
                .setBufferSizeMegabytes(bufferSizeMegabytes)
                .setTimedCaptureMode(
                        TimedCaptureMode.newBuilder()
                                .setDurationSeconds(durationSeconds)
                                .setTrimDataSeconds(trimDataSeconds)
                )
                .build();
    }

    /**
     * @see #getCaptureConfigurationDigitalTrigger
     */
    public static DigitalTriggerLinkedChannel getDigitalTriggerLinkedChannel(
            int channelIndex,
            DigitalTriggerLinkedChannelState state
    ) {
        return DigitalTriggerLinkedChannel.newBuilder()
                .setChannelIndex(channelIndex)
                .setState(state)
                .build();
    }

    /**
     * When in digital trigger capture mode, the capture will automatically stop when the specified digital condition
     * has been met.
     *
     * @param bufferSizeMegabytes This is the maximum number of megabytes allowed for storing data during a capture.
     * When this limit is reached, the capture will be terminated.
     * @param afterTriggerSeconds Number of seconds to continue capturing after trigger
     * @param trimDataSeconds Number of seconds to keep after the capture ends. If greater than 0, only the latest
     * `{@code trimDataSeconds} of the capture will be kept, otherwise the data will not be trimmed.
     * @param triggerChannelIndex index of channel in which to search for the trigger
     * @param minPulseWidthSeconds Minimum pulse width to trigger on. Only applies when digitalTriggerType is a pulse
     * trigger type.
     * @param maxPulseWidthSeconds Maximum pulse width to trigger on. Only applies when digitalTriggerType is a pulse
     * trigger type.
     * @param linkedChannels Conditions on other digital channels that must be met in order to meet the trigger
     * condition. For an edge trigger, the linked channel must be in the specified state at when the trigger edge
     * occurs. For a pulse trigger, the linked channel must be in the specified state for the duration of the pulse.
     */
    public static CaptureConfiguration getCaptureConfigurationDigitalTrigger(
            int bufferSizeMegabytes,
            DigitalTriggerType digitalTriggerType,
            double afterTriggerSeconds,
            double trimDataSeconds,
            int triggerChannelIndex,
            double minPulseWidthSeconds,
            double maxPulseWidthSeconds,
            List<DigitalTriggerLinkedChannel> linkedChannels
    ) {
        return CaptureConfiguration.newBuilder()
                .setBufferSizeMegabytes(bufferSizeMegabytes)
                .setDigitalCaptureMode(
                        DigitalTriggerCaptureMode.newBuilder()
                                .setTriggerType(digitalTriggerType)
                                .setAfterTriggerSeconds(afterTriggerSeconds)
                                .setTrimDataSeconds(trimDataSeconds)
                                .setTriggerChannelIndex(triggerChannelIndex)
                                .setMinPulseWidthSeconds(minPulseWidthSeconds)
                                .setMaxPulseWidthSeconds(maxPulseWidthSeconds)
                                .addAllLinkedChannels(linkedChannels)
                )
                .build();
    }

    /**
     * The existing software settings, like selected device or added analyzers, are ignored.
     */
    public Capture startCapture(String deviceID, LogicDeviceConfiguration logicDeviceConfiguration, CaptureConfiguration captureConfiguration) {

        final StartCaptureRequest request = StartCaptureRequest.newBuilder()
                .setDeviceId(deviceID)
                .setLogicDeviceConfiguration(logicDeviceConfiguration)
                .setCaptureConfiguration(captureConfiguration)
                .build();

        final StartCaptureReply reply = STUB.startCapture(request);

        return new Capture(this, reply.getCaptureInfo());

    }

    /**
     * Loads a .sal file. The returned xyz.froud.saleae.automation.Capture object will be fully loaded, wait_until_done not required.
     */
    public Capture loadCapture(
            String filePath
    ) {
        final LoadCaptureRequest request = LoadCaptureRequest.newBuilder()
                .setFilepath(filePath)
                .build();

        final LoadCaptureReply reply = STUB.loadCapture(request);

        return new Capture(this, reply.getCaptureInfo());
    }

    @Override
    public void close() throws InterruptedException {
        // ManagedChannels use resources like threads and TCP connections. To prevent leaking these
        // resources the channel should be shut down when it will no longer be used. If it may be used
        // again leave it running.
        CHANNEL.shutdownNow().awaitTermination(5, TimeUnit.SECONDS);
    }

    public static class IncompatibleApiVersionException extends Exception {

        public final saleae.Version APP_API_VERSION;

        public IncompatibleApiVersionException(saleae.Version appApiVersion) {
            APP_API_VERSION = appApiVersion;
        }

        @Override
        public String toString() {
            return String.format(
                    "The Java code is for API version %d, but the Logic app has API version %d.%d.%d",
                    ThisApiVersion.THIS_API_VERSION_MAJOR.getNumber(),
                    APP_API_VERSION.getMajor(),
                    APP_API_VERSION.getMinor(),
                    APP_API_VERSION.getPatch()
            );
        }


    }

}
