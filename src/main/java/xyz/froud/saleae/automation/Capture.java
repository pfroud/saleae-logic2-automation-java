package xyz.froud.saleae.automation;

import saleae.AddAnalyzerReply;
import saleae.AddAnalyzerRequest;
import saleae.AddHighLevelAnalyzerReply;
import saleae.AddHighLevelAnalyzerRequest;
import saleae.AnalyzerSettingValue;
import saleae.CaptureInfo;
import saleae.CloseCaptureRequest;
import saleae.DataTableAnalyzerConfiguration;
import saleae.DataTableFilter;
import saleae.ExportDataTableCsvRequest;
import saleae.ExportRawDataBinaryRequest;
import saleae.ExportRawDataCsvRequest;
import saleae.HighLevelAnalyzerSettingValue;
import saleae.LegacyExportAnalyzerRequest;
import saleae.LogicChannels;
import saleae.RadixType;
import saleae.RemoveAnalyzerRequest;
import saleae.RemoveHighLevelAnalyzerRequest;
import saleae.SaveCaptureRequest;
import saleae.StopCaptureRequest;
import saleae.WaitCaptureRequest;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Represents
 * <p>
 * Java port of <a
 * href="https://github.com/saleae/logic2-automation/blob/develop/python/saleae/automation/capture.py">capture.py</a>.
 */
public class Capture implements AutoCloseable {

    private final Manager MANAGER;
    private final long CAPTURE_ID;

    Capture(Manager manager, CaptureInfo captureInfo) {
        MANAGER = manager;
        CAPTURE_ID = captureInfo.getCaptureId();
    }

    public static class AnalyzerHandle {
        private final long ANALYZER_ID;

        public AnalyzerHandle(long analyzerID) {
            ANALYZER_ID = analyzerID;
        }
    }

    /**
     * Helper for {@link #addAnalyzer}
     */
    public static class AnalyzerSettings {

        private final Map<String, AnalyzerSettingValue> MAP = new HashMap<>();

        public AnalyzerSettings put(String key, String value) {
            MAP.put(key, AnalyzerSettingValue.newBuilder().setStringValue(value).build());
            return this;
        }

        public AnalyzerSettings put(String key, long value) {
            MAP.put(key, AnalyzerSettingValue.newBuilder().setInt64Value(value).build());
            return this;
        }

        public AnalyzerSettings put(String key, boolean value) {
            MAP.put(key, AnalyzerSettingValue.newBuilder().setBoolValue(value).build());
            return this;
        }

        public AnalyzerSettings put(String key, double value) {
            MAP.put(key, AnalyzerSettingValue.newBuilder().setDoubleValue(value).build());
            return this;
        }

        public Map<String, AnalyzerSettingValue> toGRPC() {
            return MAP;
        }

    }

    /**
     * Helper for {@link #addHighLevelAnalyzer}
     */
    public static class HighLevelAnalyzerSettings {

        private final Map<String, HighLevelAnalyzerSettingValue> MAP = new HashMap<>();

        public HighLevelAnalyzerSettings getHighLevelAnalyzerSettingValue(String key, String value) {
            MAP.put(key, HighLevelAnalyzerSettingValue.newBuilder().setStringValue(value).build());
            return this;
        }

        public HighLevelAnalyzerSettings getHighLevelAnalyzerSettingValue(String key, double value) {
            MAP.put(key, HighLevelAnalyzerSettingValue.newBuilder().setNumberValue(value).build());
            return this;
        }

        public Map<String, HighLevelAnalyzerSettingValue> toGRPC() {
            return MAP;
        }

    }

    /**
     * @param name The name of the Analyzer, as shown in the Logic 2 application add analyzer list. This must match
     * exactly.
     * @param label The user-editable display string for the analyzer. This will be shown in the analyzer data table
     * export.
     * @param settings All settings for the analyzer. The keys and values here must exactly match the Analyzer settings
     * as shown in the UI.
     */
    public AnalyzerHandle addAnalyzer(
            String name,
            String label,
            AnalyzerSettings settings
    ) {
        final AddAnalyzerRequest request = AddAnalyzerRequest.newBuilder()
                .setCaptureId(CAPTURE_ID)
                .setAnalyzerName(name)
                .setAnalyzerLabel(label)
                .putAllSettings(settings.toGRPC())
                .build();
        final AddAnalyzerReply reply = MANAGER.STUB.addAnalyzer(request);
        return new AnalyzerHandle(reply.getAnalyzerId());
    }

    /**
     * @param extensionDirectory The directory of the extension that the HLA is in.
     * @param name The name of the HLA, as specified in the extension.json of the extension.
     * @param label The user-editable display string for the high level analyzer. This will be shown in the analyzer
     * data table export.
     * @param inputAnalyzer Handle to analyzer (added via {@link #addAnalyzer) to use as input to this HLA.
     * @param settings All settings for the analyzer. The keys and values here must match the HLA settings as shown in
     * the HLA class.
     */
    public AnalyzerHandle addHighLevelAnalyzer(
            String extensionDirectory,
            String name,
            String label,
            AnalyzerHandle inputAnalyzer,
            HighLevelAnalyzerSettings settings
    ) {
        final AddHighLevelAnalyzerRequest request = AddHighLevelAnalyzerRequest.newBuilder()
                .setCaptureId(CAPTURE_ID)
                .setExtensionDirectory(extensionDirectory)
                .setHlaName(name)
                .setHlaLabel(label)
                .setInputAnalyzerId(inputAnalyzer.ANALYZER_ID)
                .putAllSettings(settings.toGRPC())
                .build();
        final AddHighLevelAnalyzerReply reply = MANAGER.STUB.addHighLevelAnalyzer(request);
        return new AnalyzerHandle(reply.getAnalyzerId());
    }

    public void removeHighLevelAnalyzer(
            AnalyzerHandle analyzerHandle
    ) {
        final RemoveHighLevelAnalyzerRequest request = RemoveHighLevelAnalyzerRequest.newBuilder()
                .setCaptureId(CAPTURE_ID)
                .setAnalyzerId(analyzerHandle.ANALYZER_ID)
                .build();

        //noinspection ResultOfMethodCallIgnored
        MANAGER.STUB.removeHighLevelAnalyzer(request);
    }

    public void removeAnalyzer(AnalyzerHandle handle) {
        final RemoveAnalyzerRequest request = RemoveAnalyzerRequest.newBuilder()
                .setAnalyzerId(handle.ANALYZER_ID)
                .build();

        //noinspection ResultOfMethodCallIgnored
        MANAGER.STUB.removeAnalyzer(request);
    }

    /**
     * Saves the capture to a .sal file, which can be loaded later either through the UI or with the load_capture()
     * function.
     */
    public void save(String filePath) {
        final SaveCaptureRequest request = SaveCaptureRequest.newBuilder()
                .setCaptureId(CAPTURE_ID)
                .setFilepath(filePath)
                .build();

        //noinspection ResultOfMethodCallIgnored
        MANAGER.STUB.saveCapture(request);
    }

    /**
     * Exports the specified analyzer using the analyzer plugin export format, and not the data table format.
     */
    public void legacyExportAnalyzer(
            String filePath,
            AnalyzerHandle analyzerHandle,
            RadixType radix
    ) {

        final LegacyExportAnalyzerRequest request = LegacyExportAnalyzerRequest.newBuilder()
                .setFilepath(filePath)
                .setAnalyzerId(analyzerHandle.ANALYZER_ID)
                .setRadixType(radix)
                .build();

        //noinspection ResultOfMethodCallIgnored
        MANAGER.STUB.legacyExportAnalyzer(request);
    }

    @Override
    public void close() {
        final CloseCaptureRequest request = CloseCaptureRequest.newBuilder()
                .setCaptureId(CAPTURE_ID)
                .build();

        //noinspection ResultOfMethodCallIgnored
        MANAGER.STUB.closeCapture(request);
    }

    /**
     * Stops the capture. Can be used with any capture mode, but this is recommended for use with ManualCaptureMode.
     * <p>
     * stop() and wait() should never both be used for a single capture.
     * <p>
     * Do not call stop() more than once.
     * <p>
     * stop() should never be called for loaded captures.
     * <p>
     * If an error occurred during the capture (for example, a USB read timeout, or an out of memory error) that error
     * will be raised by this function.
     */
    public void stop() {
        final StopCaptureRequest request = StopCaptureRequest.newBuilder()
                .setCaptureId(CAPTURE_ID)
                .build();

        //noinspection ResultOfMethodCallIgnored
        MANAGER.STUB.stopCapture(request);
    }

    /**
     * Waits for the capture to complete. This should only be used with TimedCaptureMode or DigitalTriggerCaptureMode.
     * <p>
     * For TimedCaptureMode, this will wait for the capture duration to complete.
     * <p>
     * For DigitalTriggerCaptureMode, this will wait for the digital trigger to be found and the capture to complete.
     * <p>
     * stop() and wait() should never both be used for a single capture.
     * <p>
     * Do not call wait() more than once.
     * <p>
     * wait() should never be called for loaded captures.
     * Can't use the name {@code wait()} because it is already {@link java.lang.Object#wait}
     */
    public void waitForCaptureToEnd() {
        final WaitCaptureRequest request = WaitCaptureRequest.newBuilder()
                .setCaptureId(CAPTURE_ID)
                .build();

        //noinspection ResultOfMethodCallIgnored
        MANAGER.STUB.waitCapture(request);
    }

    /**
     * This produces exactly the same format as used in the Logic 2 software when using the "Export Raw Data" dialog
     * with the "binary" option selected. Documentation for the format can be found here: <a
     * href="https://support.saleae.com/faq/technical-faq/binary-export-format-logic-2">https://support.saleae.com/faq/technical-faq/binary-export-format-logic-2</a>
     * <p>
     * Note, the directory parameter is a specific folder that must already exist, and should not include a filename.
     * The export system will produce one .bin file for each channel exported.
     * <p>
     * If no channels are specified, all channels will be exported.
     *
     * @param directory directory path (not including a filename) to where .bin files will be saved
     * @param analogDownsampleRatio optional analog downsample ratio, useful to help reduce export file sizes where
     * extra analog resolution isn't needed
     */
    public void exportRawDataBinary(
            String directory,
            LogicChannels channels,
            long analogDownsampleRatio
    ) {
        final ExportRawDataBinaryRequest request = ExportRawDataBinaryRequest.newBuilder()
                .setCaptureId(CAPTURE_ID)
                .setDirectory(directory)
                .setLogicChannels(channels)
                .setAnalogDownsampleRatio(analogDownsampleRatio)
                .build();

        //noinspection ResultOfMethodCallIgnored
        MANAGER.STUB.exportRawDataBinary(request);
    }

    /**
     * This produces exactly the same format as used in the Logic 2 software when using the "Export Raw Data" dialog
     * with the "CSV" option selected.
     * <p>
     * The directory parameter is a specific folder that must already exist, and should not include a filename. The
     * export system will produce an analog.csv and/or digital.csv file(s) in that directory.
     * <p>
     * All selected analog channels will be combined into the analog.csv file, and likewise for digital channels and
     * digital.csv. If no channels are specified, all channels will be exported.
     *
     * @param directory directory path (not including a filename) to where analog.csv and/or digital.csv will be saved.
     * @param analogDownsampleRatio optional analog downsample ratio, useful to help reduce export file sizes where
     * extra analog resolution isn't needed.
     * @param useISO8601Timestamps Use this to output wall clock timestamps, instead of capture relative timestamps
     */
    public void exportRawDataCsv(
            String directory,
            LogicChannels channels,
            long analogDownsampleRatio,
            boolean useISO8601Timestamps
    ) {
        final ExportRawDataCsvRequest request = ExportRawDataCsvRequest.newBuilder()
                .setCaptureId(CAPTURE_ID)
                .setDirectory(directory)
                .setLogicChannels(channels)
                .setAnalogDownsampleRatio(analogDownsampleRatio)
                .setIso8601Timestamp(useISO8601Timestamps)
                .build();

        //noinspection ResultOfMethodCallIgnored
        MANAGER.STUB.exportRawDataCsv(request);
    }

    /**
     * Wrapper for {@link saleae.DataTableAnalyzerConfiguration}. Used in {@link #exportDataTableCsv}.
     */
    public static class DataTableAnalyzerConfig {
        AnalyzerHandle analyzerHandle;
        RadixType radix;

        DataTableAnalyzerConfiguration toGRPC() {
            return DataTableAnalyzerConfiguration.newBuilder()
                    .setAnalyzerId(analyzerHandle.ANALYZER_ID)
                    .setRadixType(radix)
                    .build();
        }
    }

    /**
     * Wrapper for {@link saleae.DataTableFilter}. Used in {@link #exportDataTableCsv}.
     */
    public static class DataTableFilterWrapper {
        String query;
        List<String> columns;

        public DataTableFilter toGRPC() {
            return DataTableFilter.newBuilder()
                    .setQuery(query)
                    .addAllColumns(columns)
                    .build();
        }
    }


    /**
     * @param filePath The specified output file, including extension, .csv.
     * @param timestampInISO8601Format Use this to output wall clock timestamps, instead of capture relative timestamps.
     * Defaults to False.
     */
    public void exportDataTableCsv(
            String filePath,
            List<DataTableAnalyzerConfig> analyzers,
            boolean timestampInISO8601Format,
            List<String> columns,
            DataTableFilterWrapper filter
    ) {

        final ExportDataTableCsvRequest request = ExportDataTableCsvRequest.newBuilder()
                .setCaptureId(CAPTURE_ID)
                .setFilepath(filePath)
                .addAllAnalyzers(analyzers.stream().map(DataTableAnalyzerConfig::toGRPC).toList())
                .setIso8601Timestamp(timestampInISO8601Format)
                .addAllExportColumns(columns)
                .setFilter(filter.toGRPC())
                .build();

        //noinspection ResultOfMethodCallIgnored
        MANAGER.STUB.exportDataTableCsv(request);

    }

}
