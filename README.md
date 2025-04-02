# Saleae Logic2 Automation API for Java

Lets you control Saleae logic analyzers via the Saleae Logic 2 software from a Java program.

Saleae provides a [gRPC API and Python library](https://github.com/saleae/logic2-automation), this project is a Java library for that API.

The gRPC API can only control an already-running instance of the Logic 2 software. It cannot communicate directly with the logic analyzer hardware.

## How to get the Git submodule

The gRPC API is specified using a Protocol Buffer langauge file ([saleae.proto](https://github.com/saleae/logic2-automation/blob/develop/proto/saleae/grpc/saleae.proto)) in the Python library. We need that file to generate Java code, so the entire Python library repository is included as a [Git submodule](https://git-scm.com/book/en/v2/Git-Tools-Submodules).

If you don't do anything special when cloning the repository then there will be an empty subdirectory called "logic2-automation". There are two ways to  get the submodule:

1. When you clone the repository, add the [`--recurse-submodules`](https://git-scm.com/docs/git-clone#Documentation/git-clone.txt-code--recurse-submodulescodecodecodeemltpathspecgtem) option:

    ```commandline
    git clone --recurse-submodules https://github.com/pfroud/saleae-logic2-automation-java.git
    ```

2. Or, if you have already cloned the repository, run:

    ```commandline
    git submodule update --init
    ```

    [Link to documentation about the `git submodule update` command](https://git-scm.com/docs/git-submodule#Documentation/git-submodule.txt-update--init--remote-N--no-fetch--no-recommend-shallow-f--force--checkout--rebase--merge--referenceltrepositorygt--depthltdepthgt--recursive--jobsltngt--no-single-branch--filterltfilter-specgt--ltpathgt82308203)

See <https://git-scm.com/book/en/v2/Git-Tools-Submodules#_cloning_submodules>.

After running the Gradle build, a bunch of files will be created:

* File build/generated/source/proto/main/grpc/saleae/ManagerGrpc.java
* A lof of .java files in build/generated/source/proto/main/java/saleae
* A lot of .class files in build/classes/java/main/saleae
* File build/resources/main/saleae.proto

## Example code

The [examples](src/main/java/xyz/froud/saleae/automation/examples) directory has lots of ready-to-run examples.

The Logic 2 software must already be running for the examples to work.

All the examples use a simulated device included in Logic 2, so a logic analyzer does not need to be connected to the computer.

## Java code style

Java does not support keyword arguments aka named arguments, so we cannot exactly replicate [this example](https://saleae.github.io/logic2-automation/getting_started.html#using-the-python-automation-api) from the Python library:

```python
my_device_config = automation.LogicDeviceConfiguration(
    enabled_digital_channels=[0, 1, 2, 3],
    digital_sample_rate=10_000_000,
    digital_threshold_volts=3.3
)
```

Instead I am doing

```java
DeviceConfig myDeviceConfig = new DeviceConfig();
myDeviceConfig.digitalChannels = List.of(0, 1, 2, 3);
myDeviceConfig.digitalSampleRate = 10_000_000;
myDeviceConfig.digitalThresholdVolts = 3.3;
```
