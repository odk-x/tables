# tables

This project is __*actively maintained*__

It is part of the ODK 2.0 Android tools suite.

ODK Tables is a program that allows you to visualize and edit data, revisiting existing data and syncing it with an ODK Aggregate instance in the cloud.

Instructions on how to use Tables can be found [here](https://opendatakit.org/use/2_0_tools/odk-tables-2-0-rev126/).

The developer [wiki](https://github.com/opendatakit/opendatakit/wiki) (including release notes) and
[issues tracker](https://github.com/opendatakit/opendatakit/issues) are located under
the [**opendatakit**](https://github.com/opendatakit/opendatakit) project.

The Google group for software engineering questions is: [opendatakit-developers@](https://groups.google.com/forum/#!forum/opendatakit-developers)

## Setting up your environment

General instructions for setting up an ODK 2.0 environment can be found at our [DevEnv Setup wiki page](https://github.com/opendatakit/opendatakit/wiki/DevEnv-Setup)

Install [Android Studio](http://developer.android.com/tools/studio/index.html) and the [SDK](http://developer.android.com/sdk/index.html#Other).

This project depends on ODK's [androidlibrary](https://github.com/opendatakit/androidlibrary) and [androidcommon](https://github.com/opendatakit/androidcommon) projects; their binaries will be downloaded automatically fom our maven repository during the build phase. If you wish to modify them yourself, you must clone them into the same parent directory as tables. You directory stucture should resemble the following:

        |-- odk

            |-- androidcommon

            |-- androidlibrary

            |-- tables


  * Note that this only applies if you are modifying the library projects. If you use the maven dependencies (the default option), the projects will not show up in your directory. 
    
ODK [Services](https://github.com/opendatakit/services) __MUST__ be installed on your device, whether by installing the APK or by cloning the project and deploying it. ODK [Survey](https://github.com/opendatakit/survey) also integrates well with ODK Tables, but is not required.

Now you should be ready to build.

## Building the project

Open the Tables project in Android Studio. Select `Build->Make Project` to build the app.

## Running

Be sure to install ODK Services onto your device before attempting to run Tables.

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
