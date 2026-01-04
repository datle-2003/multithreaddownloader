# Java Multi-thread Downloader

A lightweight, command-line download manager written in Java. It supports multi-threaded downloading for higher speeds and includes a robust resume capability.

## 🛠 Build

Build the project using Gradle:

```bash
./gradlew clean build
```
The executable JAR will be located in build/libs/.

Usage
Basic Syntax
Bash

java -jar build/libs/MultithreadDownloader-1.0-SNAPSHOT.jar <URL> [OPTIONS]

Options
Flag	Description	Default
<URL>	Direct link to the file you want to download.	(Required)
-n <number>	Number of threads (connections) to use.	1
💡 Examples

1. Download a file with default settings (Single thread):
   Bash

java -jar build/libs/MultithreadDownloader-1.0-SNAPSHOT.jar [https://releases.ubuntu.com/24.04/ubuntu-24.04.iso](https://releases.ubuntu.com/24.04/ubuntu-24.04.iso)

2. Download with 16 threads (Recommended for speed):
   Bash

java -jar build/libs/MultithreadDownloader-1.0-SNAPSHOT.jar [https://releases.ubuntu.com/24.04/ubuntu-24.04.iso](https://releases.ubuntu.com/24.04/ubuntu-24.04.iso) -n 16

🔄 Resume Capability & State Management

This tool automatically handles network interruptions and manual stops.

    To Pause/Stop: Press Ctrl + C. The program will gracefully stop workers, save the current progress to a .meta file, and exit.

    To Resume: Simply run the exact same command again.

        The tool detects the existing .meta file.

        It automatically verifies the file size and server support.

        It resumes downloading exactly where it left off.

    Note: If the download completes successfully, the .meta file is automatically deleted.