# Tables

This project is __*actively maintained*__

It is part of the ODK-X Android tools suite.

ODK-X Tables is a program that allows you to visualize and edit data, revisiting existing data and syncing it with an ODK-X Aggregate instance in the cloud.

Instructions on how to use Tables can be found [here](https://docs.odk-x.org/tables-using/).

The developer [wiki](https://github.com/odk-x/tool-suite-X/wiki) (including release notes) and [issues tracker](https://github.com/odk-x/tool-suite-X/issues) are located under the [**ODK-X Tool Suite**](https://github.com/odk-x) project.

Engage with the community and get technical support on [the ODK-X forum](https://forum.odk-x.org)

## Setting up your environment

General instructions for setting up an ODK-X environment can be found at our [Developer Environment Setup wiki page](https://github.com/odk-x/tool-suite-X/wiki/Developer-Environment-Setup).

Install [Android Studio](http://developer.android.com/tools/studio/index.html) and the [SDK](http://developer.android.com/sdk/index.html#Other).

This project depends on ODK's [androidlibrary](https://github.com/opendatakit/androidlibrary) and [androidcommon](https://github.com/opendatakit/androidcommon) projects; their binaries will be downloaded automatically fom our maven repository during the build phase. If you wish to modify them yourself, you must clone them into the same parent directory as tables. You directory stucture should resemble the following:

        |-- odk-x

            |-- androidcommon

            |-- androidlibrary

            |-- tables


  * Note that this only applies if you are modifying the library projects. If you use the maven dependencies (the default option), the projects will not show up in your directory. 
    
ODK-X [Service](https://github.com/odk-x/services) __MUST__ be installed on your device, whether by installing the APK or by cloning the project and deploying it. ODK-X [Survey](https://github.com/odk-x/survey) also integrates well with ODK-X Tables, but is not required.

Now you should be ready to build.

## Building the project

Open the Tables project in Android Studio. Select `Build->Make Project` to build the app.

## Running

Be sure to install ODK-X Services onto your device before attempting to run Tables.

## Running tests

When running tests from Android Studio, execute `adb shell pm grant org.opendatakit.tables android.permission.SET_ANIMATION_SCALE` in the terminal first. 

## Source tree information
Quick description of the content in the root folder:

    |-- tables_app     -- Source tree for Java components

        |-- src

            |-- main

                |-- res     -- Source tree for Android resources

                |-- java

                    |-- org

                        |-- opendatakit

                            |-- tables     -- The most relevant Java code lives here

            |-- androidTest    -- Source tree for Android implementation tests




## How to contribute
If you’re new to ODK-X you can check out the documentation:
- [https://docs.odk-x.org](https://docs.odk-x.org)

Once you’re up and running, you can choose an issue to start working on from here: 
- [https://github.com/odk-x/tool-suite-X/issues](https://github.com/odk-x/tool-suite-X/issues)

Issues tagged as [good first issue](https://github.com/odk-x/tool-suite-X/issues?q=is%3Aissue+is%3Aopen+label%3A%22good+first+issue%22) should be a good place to start.

Pull requests are welcome, though please submit them against the development branch. We prefer verbose descriptions of the change you are submitting. If you are fixing a bug please provide steps to reproduce it or a link to a an issue that provides that information. If you are submitting a new feature please provide a description of the need or a link to a forum discussion about it. 

## Links for users
This document is aimed at helping developers and technical contributors. For information on how to get started as a user of ODK-X, see our [online documentation](https://docs.odk-x.org), or to learn more about the Open Data Kit project, visit [https://odk-x.org](https://odk-x.org).
