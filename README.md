# AEM Page Packager

This is a project to simplify pages package creation process.

## Modules

The main parts of the template are:

* core: Java bundle containing all core functionality like OSGi services
* ui.apps: contains the /apps (and /etc) parts of the project, ie JS clientlib, runmode specific configs

## How to build

To build all the modules run in the project root directory the following command with Maven 3:

    mvn clean install

If you have a running AEM instance you can build and package the whole project and deploy into AEM with  

    mvn clean install -PautoInstallPackage
    

Or to deploy only the bundle to the author, run

    mvn clean install -PautoInstallBundle

## Samples

There are 2 samples in the `samples` folder:

__IMPORTANT__ (It is required for the tool to work properly!)
* Overlay example to extend page menu
* Required user mapping sample configuration


## Logging

Project has dedicated logger file `project-page-packager.log` (By default configured with the `INFO` level).