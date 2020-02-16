# HGVaadinPlugin - Hybrid Gradle Vaadin Plugin

This plugin is to use with Vaadin Flow 14+ and use maven to compile the front-end.
Since the oficial Gradle plugin requiere a licence, I create this as a workaround because I realy do not like maven's POMs, so I wrote this to do not fight with it.

To use it, just call:

gradle vaadinBuild

the plugin will remove the "dependencies" node in the POM and recreate it with the dependencies thats found in the build.gradle
and update de entry <vaadin.version> in the properties section.

