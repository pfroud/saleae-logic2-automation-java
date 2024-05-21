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

import java.util.Collections;
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
        //System.out.printf("Connected to Logic version %s with PID %d.\n", appInfo.getApplicationVersion(), appInfo.getLaunchPid());

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


    /**
     * Wrapper for {@link saleae.LogicDeviceConfiguration}
     */
    public static class DeviceConfig {
        /**
         * Indexes of digital channels to record.
         */
        public List<Integer> digitalChannels = Collections.emptyList();

        /**
         * Indexes of analog channels to record.
         */
        public List<Integer> analogChannels = Collections.emptyList();

        /**
         * In samples per second
         */
        public int digitalSampleRate;

        /**
         * In samples per second
         */
        public int analogSampleRate;

        /**
         * For Pro 8 and Pro 16, this can be one of: 1.2, 1.8, or 3.3. For other devices this is ignored.
         */
        public double digitalThresholdVolts;

        /**
         * Glitch filters can suppress short digital pulses to help remove noise picked up in a digital recording.
         * <p>
         * The glitch filter is purely a software filter on top of the recorded data. Using the glitch filter does not
         * actually change the data that is recorded. Instead, it sits between the recorded data set and all software
         * components that access it.
         *
         * @see <a
         * href="https://support.saleae.com/user-guide/using-logic/software-glitch-filter">https://support.saleae.com/user-guide/using-logic/software-glitch-filter</a>
         */
        public List<GlitchFilter> glitchFilters = Collections.emptyList();

        private LogicDeviceConfiguration toGRPC() {
            return LogicDeviceConfiguration.newBuilder()
                    .setLogicChannels(
                            LogicChannels.newBuilder()
                                    .addAllAnalogChannels(analogChannels)
                                    .addAllDigitalChannels(digitalChannels)
                    )
                    .setDigitalSampleRate(digitalSampleRate)
                    .setAnalogSampleRate(analogSampleRate)
                    .setDigitalThresholdVolts(digitalThresholdVolts)
                    .addAllGlitchFilters(glitchFilters.stream().map(GlitchFilter::toGRPC).toList())
                    .build();
        }

    }

    /**
     * Wrapper for {@link saleae.GlitchFilterEntry}.
     * <p>
     * Glitch filters can suppress short digital pulses to help remove noise picked up in a digital recording.
     * <p>
     * The glitch filter is purely a software filter on top of the recorded data. Using the glitch filter does not
     * actually change the data that is recorded. Instead, it sits between the recorded data set and all software
     * components that access it.
     *
     * @see <a
     * href="https://support.saleae.com/user-guide/using-logic/software-glitch-filter">https://support.saleae.com/user-guide/using-logic/software-glitch-filter</a>
     */
    public static class GlitchFilter {

        public int channelIndex;
        public double pulseWidthSeconds;

        private GlitchFilterEntry toGRPC() {
            return GlitchFilterEntry.newBuilder()
                    .setChannelIndex(channelIndex)
                    .setPulseWidthSeconds(pulseWidthSeconds)
                    .build();
        }

    }

    /**
     * Wrapper for {@link saleae.CaptureConfiguration}
     */
    public static abstract class CaptureConfig {
        /**
         * The maximum number of megabytes allowed for storing data during a capture.
         * <p>
         * When this limit is reached, what happens depends on the capture mode:
         * <ul>
         * <li>In manual capture mode, the oldest data will be deleted until the total usage is under the limit.</li>
         * <li>In timer and digitalTrigger capture modes, the capture will be terminated.</li>
         * <ul>
         */
        public int bufferSizeMegabytes;

        /**
         * Number of seconds to keep after the capture ends.
         * <ul>
         *     <li>If set to zero or a negative number, the data will not be trimmed.</li>
         *     <li>If set to a positive number, only the latest this many seconds of the capture will be kept.</li>
         * </ul>
         */
        double trimDataSeconds;

        abstract CaptureConfiguration toGRPC();
    }

    /**
     * Wrapper for {@link saleae.CaptureConfiguration} with {@link saleae.ManualCaptureMode}.
     * <p>
     * When in manual capture mode, the capture must be manually stopped using the StopCapture request.
     */
    public static class CaptureConfigManual extends CaptureConfig {

        @Override
        CaptureConfiguration toGRPC() {
            return CaptureConfiguration.newBuilder()
                    .setBufferSizeMegabytes(bufferSizeMegabytes)
                    .setManualCaptureMode(
                            ManualCaptureMode.newBuilder()
                                    .setTrimDataSeconds(trimDataSeconds)
                    )

                    .build();
        }
    }

    /**
     * Wrapper for {@link saleae.CaptureConfiguration} with {@link saleae.TimedCaptureMode}.
     * <p>
     * When in timed capture mode, the capture will automatically stop after {@code durationSeconds}.
     */
    public static class CaptureConfigTimed extends CaptureConfig {
        /**
         * Seconds of data to capture.
         */
        public double durationSeconds;

        @Override
        CaptureConfiguration toGRPC() {
            return CaptureConfiguration.newBuilder()
                    .setBufferSizeMegabytes(bufferSizeMegabytes)
                    .setTimedCaptureMode(
                            TimedCaptureMode.newBuilder()
                                    .setDurationSeconds(durationSeconds)
                                    .setTrimDataSeconds(trimDataSeconds)
                    )

                    .build();
        }
    }

    /**
     * Wrapper for {@link saleae.CaptureConfiguration} with {@link saleae.DigitalTriggerCaptureMode}
     * <p>
     * When in digital trigger capture mode, the capture will automatically stop when the specified digital condition
     * has been met.
     */
    public static class CaptureConfigDigitalTrigger extends CaptureConfig {

        public DigitalTriggerType digitalTriggerType;
        /**
         * Number of seconds to continue capturing after trigger.
         */
        public double afterTriggerSeconds;
        /**
         * Index of channel in which to search for the trigger.
         */
        public int triggerChannelIndex;
        /**
         * Minimum pulse width to trigger on. Only applies when digitalTriggerType is a pulse trigger type.
         */
        public double minPulseWidthSeconds;
        /**
         * Maximum pulse width to trigger on. Only applies when digitalTriggerType is a pulse trigger type.
         */
        public double maxPulseWidthSeconds;

        /**
         * Conditions on other digital channels that must be met in order to meet the trigger condition.
         * <ul>
         *     <li>For an edge trigger, the linked channel must be in the specified state at when the trigger edge occurs.</li>
         *     <li>For a pulse trigger, the linked channel must be in the specified state for the duration of the pulse.</li>
         * </ul>
         */
        public List<LinkedChannel> linkedChannels = Collections.emptyList();

        /**
         * Wrapper for {@link saleae.DigitalTriggerLinkedChannel}
         */
        public static class LinkedChannel {
            /**
             * Channel to link to.
             */
            int channelIndex;

            /**
             * Expected state of the linked channel at trigger.
             */
            DigitalTriggerLinkedChannelState state;

            DigitalTriggerLinkedChannel toGRPC() {
                return DigitalTriggerLinkedChannel.newBuilder()
                        .setChannelIndex(channelIndex)
                        .setState(state)
                        .build();
            }

        }

        @Override
        CaptureConfiguration toGRPC() {
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
                                    .addAllLinkedChannels(
                                            linkedChannels.stream().map(LinkedChannel::toGRPC).toList()
                                    )
                    )
                    .build();
        }

    }


    /**
     * The existing software settings, like selected device or added analyzers, are ignored.
     */
    public Capture startCapture(String deviceID, Manager.DeviceConfig deviceConfig, CaptureConfig captureConfig) {

        final StartCaptureRequest request = StartCaptureRequest.newBuilder()
                .setDeviceId(deviceID)
                .setLogicDeviceConfiguration(deviceConfig.toGRPC())
                .setCaptureConfiguration(captureConfig.toGRPC())
                .build();

        final StartCaptureReply reply = STUB.startCapture(request);

        return new Capture(this, reply.getCaptureInfo());

    }

    /**
     * Loads a .sal file. The returned Capture object will be fully loaded, you do not need to call wait_until_done.
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
