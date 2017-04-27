# J2ObjC Gradle Plugin

The __J2ObjC Gradle plugin__ enables Java source to be part of an iOS application's build
so you can write an app's non-UI code (such as application logic and data models) in Java,
which is then shared by Android apps (natively Java) and iOS apps (using J2ObjC).

This plugin is stripped version of original. Diff:
* Building static libraries is no longer available
* Plugin does not translate and run unit tests anymore
* Plugin now support transitive dependencies. So you don't have to add dependencies manually.
* There is no cocoapods support anymore.

Why do we cut this features? - Plugin became more flexible, understandable and easy to support.
Now you can use any gradle version you want. Issue https://github.com/j2objc-contrib/j2objc-gradle/issues/568 was fixed.

# Original plugin
https://github.com/j2objc-contrib/j2objc-gradle
