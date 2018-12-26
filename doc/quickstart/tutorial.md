<!--
[//]: # (Copyright &#169; 2008 Tridium, Inc
  Licensed under the Academic Free License version 3.0

  History:
    21 Aug 08  Brian Frank  Creation
) -->
[![Sedona](../logo.png)](/)
# Tutorial

## Overview
This tutorial takes you through the various development steps
involved in building Sedona Framework applications:

1. Build a new kit and component
2. Build a new scode image
3. Build an application
4. Run the application

It is recommended that you read the [Architecture](/quickstart/architecture)
chapter before exploring this tutorial.  Refer to the [diagram](/quickstart/architecture#workflow), which illustrates the
work flow this tutorial will take.  Also make sure you have your
Sedona Framework environment [setup](/quickstart/setup) correctly before.

## Build New Kit

Kits are modules used to organize Sedona code.  For this
example we will create a new kit called <code>tutorial</code>.
In our kit we will create a component called <code>Add</code>,
which adds two inputs together.

First let's create a directory under <code>src</code> called "tutorial", with two text files:
```
src/
  tutorial/
    +- kit.xml
    +- Add.sedona
```

For more information see [Structure](/language/lang#structure)
to see how kits are organized.

The `kit.xml` file is used to define the kit's meta-data for the compiler:
```xml
<sedonaKit name="tutorial" vendor="Tridium" description="blah">
  <depend on="sys 1.2+" />
  <source dir="." />
</sedonaKit>
```

The `kit.xml` file specifies the name of our kit, a short description, dependencies, and the directories of source code. (The vendor attribute specifies the company that developed the kit - for the purposes of the tutorial leave this as "Tridium" because otherwise the kit name must be prefixed with the vendor name.)  For more information see [Kits](/deployment/kits) and [Compile Kit](/development/sedonac#compile-kit).

The "Add.sedona" file defines the source code for our new component:

```java
public class Add extends Component
{
  property float out
  property float in1
  property float in2

  override void execute() { out := in1 + in2 }
}
```

The Add component is pretty simple - it declares two inputs and an output.
When the component is "executed" we add the two inputs and update the
output.  For more information see [Components](/language/components)

To compile our new kit we just need to run sedonac against our directory
or the `kit.xml` file:

```shell
C:\Sedona\Sedona-1.2.27\src\tutorial>dir
 Volume in drive C has no label.
 Volume Serial Number is 50BC-4286

 Directory of C:\Sedona\Sedona-1.2.27\src\tutorial

02/25/2013  09:57 AM    <DIR>          .
02/25/2013  09:57 AM    <DIR>          ..
02/25/2013  09:52 AM               160 Add.sedona
02/25/2013  09:55 AM               131 kit.xml
               2 File(s)            291 bytes
               2 Dir(s)  170,500,431,872 bytes free

C:\Sedona\Sedona-1.2.27\src\tutorial>sedonac kit.xml
  Parse [1 files]
  WriteKit [C:\Sedona\Sedona-1.2.27\kits\tutorial\tutorial-89858e3e-1.2.27.kit]
  WriteManifest [C:\Sedona\Sedona-1.2.27\manifests\tutorial\tutorial-89858e3e.xml]
*** Success! ***
```

If successful, you should have a new kit file in your
`kits/tutorial` directory.  You can open this file in a tool like
WinZip to explore the compiler's output.  For more information
see [Sedonac](/development/sedonac#compile-kit).  If you have
trouble running sedonac, then see [Setup](/quickstart/setup).

## Build New SCode Image

Kits are units of deployment, but are not run directly by the
SVM.  First we have to compile a set of kits into an scode
image that can be run directly by the SVM.  Create a new
directory called `tutorialApp` with one file:

```
tutorialApp/
  +- kits.xml
```

The "kits.xml" file specifies the kits we wish to compile into
an image:

```xml
<sedonaCode
  endian="little" blockSize="4"
  refSize="4" main="sys::Sys.main" debug="true" test="true"
>
  <depend on="sys 1.2+" />
  <depend on="sox 1.2+" />
  <depend on="inet 1.2+" />
  <depend on="web 1.2+" />
  <depend on="func 1.2+" />
  <depend on="platWin32 1.2+" />
  <depend on="tutorial 1.2+" />
</sedonaCode>
```

!!! Info
    See [Sedonac](/development/sedonac#compile-code) for more information on these settings.

Run sedonac on this file to produce an scode image:

```shell
C:\Sedona\Sedona-1.2.27\tutorialApp>sedonac kits.xml
  ReadKits [7 kits]
  WriteImage [C:\Sedona\Sedona-1.2.27\tutorialApp\kits.scode] (89088 bytes)
  +----------------------------------
  |  Data:      7.8kb (8020 bytes)
  |  Code:       87kb (89088 bytes)
  |  Total:    94.8kb (97108 bytes)
  +----------------------------------
*** Success! ***

C:\Sedona\Sedona-1.2.27\tutorialApp>dir
 Volume in drive C has no label.
 Volume Serial Number is 50BC-4286

 Directory of C:\Sedona\Sedona-1.2.27\tutorialApp

02/25/2013  10:30 AM    <DIR>          .
02/25/2013  10:30 AM    <DIR>          ..
02/25/2013  10:30 AM            89,088 kits.scode
02/25/2013  09:57 AM               336 kits.xml
               2 File(s)         89,424 bytes
               2 Dir(s)  170,506,817,536 bytes free
```
If successful, then you should have now have a `kits.scode` file.

## Build New App

Sedona is a component oriented language that enables you to
build new applications by assembling components.  The application
file stores a tree of components, their configuration properties,
and how they are linked together.  Typically applications are built
with graphical tools.  For this tutorial we will hand code an
application file using XML.  In the `tutorialApp` directory let's
create a new `app.sax` file:

```XML
<sedonaApp>
<schema>
  <kit name='sys'/>
  <kit name='sox'/>
  <kit name='inet'/>
  <kit name='web'/>
  <kit name='func'/>
  <kit name='platWin32'/>
  <kit name='tutorial'/>
</schema>
<app>
  <comp name="plat" type="platWin32::Win32PlatformService"/>
  <comp name="users" type="sys::UserService">
    <comp name="admin" type="sys::User">
      <prop name="cred" val="hE49ksThgAeLkWB3NUU1NWeDO54="/>
      <prop name="perm" val="2147483647"/>
      <prop name="prov" val="255"/>
    </comp>
  </comp>
  <comp name="sox" type="sox::SoxService"/>
  <comp name="web" type="web::WebService">
    <prop name="port" val="8080"/>
  </comp>
  <comp name="rampA" type="func::Ramp"/>
  <comp name="rampB" type="func::Ramp"/>
  <comp name="add"   type="tutorial::Add" id="12"/>
</app>
<links>
  <link from="/rampA.out" to="/add.in1"/>
  <link from="/rampB.out" to="/add.in2"/>
</links>
</sedonaApp>
```

The file above declares four Service components:

-  The <code>Win32PlatformService</code> component provides access to platform properties  and other platform-specific features.  It is a subclass of <code>sys::PlatformService</code> class which provides generic platform functionality.  Sedona running on another platform would need a different PlatformService subclass designed for that platform.
-  The <code>UserService</code> component contains an entry for each authorized user, specifying the user's credentials and privileges.  The properties here set up a user named "admin" with a blank password.  (Using a blank password for 'admin' should NOT be done on real Sedona devices!  We only do it here to simplify the tutorial.)
- The <code>SoxService</code> component is required in order to create and use a Sox connection to or from the device
- The <code>WebService</code> component will run an HTTP server so we can use a browser for debugging. The web server's port is set to 8080

!!! Note
    Platforms with limited resources may not be able to run a web server.

We then declare two <code>func::Ramp</code> components, which are used to generate dummy data.
Finally we declare a <code>tutorial::Add</code> component, which is the component we built ourselves in the step above.

In the links section we create links between the Ramp component outputs and
the inputs to our <code>Add</code> block to create control flow.

Sedona Framework-enabled devices don't run the XML file directly, so now we need to compile the XML into a binary format that the SVM can use directly.  Do this by running `sedonac` on `app.sax`:

```shell
C:\Sedona\Sedona-1.2.27\tutorialApp>sedonac app.sax
  ConvertAppFile [C:\Sedona\Sedona-1.2.27\tutorialApp\app.sax -> C:\Sedona\Sedona-1.2.27\tutorialApp
\app.sab]
  +----------------------------------
  |  RAM:     14.5kb (14848 bytes)
  |  FLASH:    0.3kb (356 bytes)
  +----------------------------------
*** Success! ***
```
Now we should have a file called `app.sab`.  See [Apps](/apps/apps) for more information.

## Run App

If you have followed the steps above, we have:

- Built a new kit called `tutorial` with a component called `Add`
- Compiled our new kit into an scode image called `kits.scode`
- Defined and compiled an app into a file called `app.sab`

At this point we can run our application using the SVM.  Assuming
we are running on Windows we can run our application as follows:

```shell
C:\Sedona\Sedona-1.2.27\tutorialApp>svm kits.scode app.sab

Sedona VM 1.2.27
buildDate: Oct 17 2012 08:40:10
endian:    little
blockSize: 4
refSize:   4

-- MESSAGE [sys::App] starting
-- MESSAGE [sox::SoxService] started port=1876
-- MESSAGE [sox::SoxService] DASP Discovery enabled
-- MESSAGE [web::WebService] started port=8080
-- MESSAGE [sys::App] running
```

Here we just pass our code and application filenames to the prebuilt Win32 "svm.exe" executable included with the Sedona open source.
If you are running on a different platform you will need an SVM executable designed for that platform - see [Porting](/development/porting).

Note that if you plan to restart or reboot the Sedona VM remotely, you should start
the Sedona VM with the <code>--plat</code> option to run it in "platform mode".
(See [Common Commands](/quickstart/setup#common-commands) for more details.)

Now that the SVM is running the application, you should be able to access it with your browser at [http://localhost:8080/](http://localhost:8080/). You can use the `spy` URL [http://localhost:8080/spy/app/12](http://localhost:8080/spy/app/12) to view the current values of your <code>tutorial::Add</code> component. Hit refresh a couple times to see how the inputs and output change in real-time.

If you have a remote administration tool such as the Sedona Framework Workbench, you should now be able to connect to Sox port 1876 with the username "admin" and empty password.

Refer to your Sedona Framework tool documentation for more information.

## Success!

Congratulations, you've just built and deployed your first Sedona Framework
application!  This tutorial illustrates just the very basics of Sedona,
using only the command line tools.  Continue to explore
[sedonadev.org](http://www.sedonadev.org/) to learn more about
Sedona and the tools available for managing Sedona devices.

!!! Info
    As of summer of 2018, SedonaDev website was taken down by Tridium.
