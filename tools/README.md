This folder include several handy tools to make sedona hacking easier.

Wireshare Sox/Dasp Dissector 
============================
Please follow these steps to set it up: 
 1. copy wireshark/sox.lua file into Wireshark's installation folder(for example: c:/Program Files/Wireshark)
 2. edit init.lua file under Wireshark's installation folder, append following
line:
```
  dofile(DATA_DIR.."sox.lua")
```
 3. restart Wireshark 

Then you can use filter like this: 
```
  (sox.cmd == "c" || sox.cmd == "C") && sox.compId == 1
```

Docker Build Environment
=================================
1. Make sure that docker is installed and started. On OSX, you can install Docker Toolbox and run Docker Quickstart Terminal

2. In terminal window, go to sedona's folder /tools/docker and initialize the build environment:
```
 $ cd /path/to/sedona/tools/docker 
 $ ./makeDev-docker.sh
```
 This pulls a 32-bit (CentOS) linux image and installs JDK, git and few other libraries. If you want a ubuntu linux image, you can just rename Dockerfile.ubuntu to Dockerfile before running the above command.

3. In terminal window, start the sedona build environment:
```
 $ ./startDev-docker.sh
```
 This creates a user-specific docker image, starts the container with sedona's folder mapped to it. It also initializes the sedona dev environment through a .bashrc, calling /adm/unix/init.sh
 
4. Once the container is up and running, you can just run the following in the command window:
```
 $ makedev
```
