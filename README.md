# SpringBoot 3 with Java 21
Build basic configuration of Spring Boot 3 using Java 21

### Installation [Ubuntu]
```bash
sudo apt update
sudo apt install openjdk-21-jdk
```

### Encounter version issue
```bash
java.lang.UnsupportedClassVersionError: com/nazran/bigfile/BigfileApplication has been compiled by a more recent version of the Java Runtime (class file version 65.0), this version of the Java Runtime only recognizes class file versions up to 61.0
```
* Explanation:
  The error `java.lang.UnsupportedClassVersionError` indicates that our application (BigfileApplication) has been compiled using a newer version of Java (in this case, Java 21 with class file version 65.0), but we are attempting to run it on an older Java Runtime Environment (JRE) that supports only up to class file version 61.0, which corresponds to Java 17.


* Solution: If we are running the application from IntelliJ IDEA, make sure to
  - Set Project SDK to JDK 21 in `File` > `Project Structure` > **Project Settings**.
  - Set the JDK version in the Run/Debug Configurations to JDK 21.
