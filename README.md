DotNetTables
============

DotNetTables is an extention of the NetworkTables interface provided by FRC:
	http://firstforge.wpi.edu/sf/projects/network_tables_2_0

The goal of this project is two-fold:

1. To provide an easy-to-integrate .NET conversion of NetworkTables, for use in non-Java driver station interfaces
2. To provide enforced directionality and other wrapper functions to more strictly manage the use of NetworkTables

While our team uses the DotNetTables wrapper classes, this project is intended to provide complete access to the underlying NetworkTables implementation, if you prefer the interface that provides. Both the Java and .NET projects expose all classes, methods, and data types provided by the NetworkTables API.

The DotNetTables API is documented here:
	http://frc-dotnettables.github.io/Java/

The corresponding .NET project is available here:
	https://github.com/FRC-DotNetTables/.NET

If you have questions feel free to mail:
	zach@kotlarek.com

Java Implementation
-------------------

In the dist(dist/) folder you'll find DotNetTables.jar(dist/DotNetTables.jar) and DotNetTables-combined.jar(dist/DotNettables-combined.jar).

Both archives contain the classes from this project, and the *-combined version also contains the classes from FRC NetworkTables desktop implementation. So if you are already including the FRC-provided networktables-desktop.jar library in your project you should use DotNetTables.jar, otherwise you want the DotNetTables-combined.jar library.
