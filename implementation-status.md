# Implementation status

| GRPC or protobuf name     | Status                                |
|---------------------------|---------------------------------------|
| `GetAppInfo`              | ✅ `Manager.getAppInfo()`              |
| `GetDevices`              | ✅ `Manager.getDevices()`              |
| `StartCapture`            | ✅ `Manager.startCapture`              |
| `StopCapture`             | ✅ `Capture.stop()`                    |
| `WaitCapture`             | ✅ `Capture.waitForCaptureToEnd()`     |
| `LoadCapture`             | ✅ `Manager.loadCapture`               |
| `SaveCapture`             | ✅ `Capture.save()`                    |
| `CloseCapture`            | ✅ `Capture.close()`                   |
| `AddAnalyzer`             | ✅ `Capture.addAnalyzer()`             |
| `RemoveAnalyzer`          | ✅ `Capture.addAnalyzer()`             |
| `AddHighLevelAnalyzer`    | ✅ `Capture.addHighLevelAnalyzer()`    |
| `RemoveHighLevelAnalyzer` | ✅ `Capture.removeHighLevelAnalyzer()` |
| `ExportRawDataCsv`        | ✅ `Capture.exportRawDataCsv()`        |
| `ExportRawDataBinary`     | ✅ `Capture.exportRawDataBinary(`      |
| `ExportDataTableCsv`      | ✅ `Capture.exportDataTableCsv(`       |
| `LegacyExportAnalyzer`    | ✅ `Capture.legacyExportAnalyzer`      | 