# Java Multi-thread Downloader


A lightweight command-line download manager written in Java. Supports multi-threaded downloading for higher speeds and includes a robust resume capability.

## Build

You can build this project in two ways: as a standard JAR or as a standalone native executable (GraalVM).

### 1. Standard JAR

Build the project using Gradle:

```bash
./gradlew clean build
```

The executable JAR will be located in `build/libs/`.

### 2. Native Executable (GraalVM)

Build a standalone binary that runs without a separate Java installation (Linux only). Prerequisite: GraalVM JDK 21+ must be installed and configured.

```bash
./gradlew nativeCompile
```

The standalone binary will be located in `build/native/nativeCompile/downloader`.

---

## Usage

Basic syntax:

Using the JAR:

```bash
java -jar build/libs/MultithreadDownloader-1.0-SNAPSHOT.jar <URL> [OPTIONS]
```

Using the native binary:

```bash
./build/native/nativeCompile/downloader <URL> [OPTIONS]
```

### Options

| Flag          | Description                                     | Default           |
|---------------|-------------------------------------------------|-------------------|
| `<URL>`       | Direct link to the file to download.            | **(Required)** |
| `-n <number>` | Number of threads (connections) to use.         | `4`               |
| `-o <path>`   | Specify custom output file name or path.        | Filename from URL |
| `-h`          | Show help message.                          | —                 |
---

## Examples

1) Download with 8 threads:

```bash
java -jar build/libs/MultithreadDownloader-1.0-SNAPSHOT.jar https://releases.ubuntu.com/24.04/ubuntu-24.04.3-desktop-amd64.iso -n 8
```

2) Using the native binary:

```bash
./build/native/nativeCompile/downloader https://releases.ubuntu.com/24.04/ubuntu-24.04.3-desktop-amd64.iso -n 16
```

---

## Resume capability & state management

This tool automatically handles interruptions and manual stops:

- To pause/stop: press `Ctrl+C`. The program attempts to stop workers cleanly, save progress to a `.meta` file, and exit.
- To resume: run the same command again. The tool:
  - Detects an existing `.meta` file for the same destination URL.
  - Verifies file size and server support for range requests.
  - Resumes downloading from the saved progress.
- If a download completes successfully, the `.meta` file is removed.

Notes:

- Use the same destination and URL to ensure correct resume behavior.
---

## Docker
You can build and run the downloader using Docker.

Build the Docker image:

```bash
docker build -t multithread-downloader .
```

Run the downloader in a container:

```bash
docker run --it --rm --init -v $(pwd):/downloads multithread-downloader <URL> [OPTIONS]
```