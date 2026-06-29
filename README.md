# numworx-dwo-runner

## Introduction

The main purpose of this project is to facilitate running the Numworx Author application, maintenance and initialize the database. 

## Prerequisites

This project has dependencies from de numworx-dwo-project project. Since TeamDev stopped supporting OSGI headers in their manifest for the jxbrowser, they are rebuild using the bnd tools.

## Contents 

### Folder structure

The important folders:
* DWO_runner, a simple java-8 runner application for the Numworx Author jars at https://app.dwo.nl/dwo/
* initdb, initiaize the database. create all tables for the JPA persistent classes
* maintenance, remove old users and their data from the database
* osgi, part of the OSGi runner application at https://github.com/wimvvelt/numworx-author. All supporting bundles.

### File formats 

Everything is a Java-11 Maven project. Install with `mvn clean install`

## Usage

Build and install this project with maven. The artefacts are dependencies of the numworx-dwo-resources and numworx-dwo-setup projects.
The github repository https://maven.pkg.github.com/UtrechtUniversity/numworx-dwo-runner contains the deployed artifacts.

## License

This work is licensed under the GNU General Public License version 3.0
Copyright 2006 Utrecht University, all rights reserved.

## Contact 

[Wim van Velthoven](mailto:w.p.g.vanvelthoven@uu.nl)
