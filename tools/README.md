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

Alternatively it's possible to load lua script by giving it in command line:
```
 wireshark -X lua_script:sox.lua
```

Docs Build Environment
=================================
For building docs locally, please update the local python environment, and install the required plugins Following are instructions to update a unix (docker) environment:

```bash
# Update python tools
$ yum upgrade python-setuptools

# get pip installer and install it
$ curl "https://bootstrap.pypa.io/get-pip.py" -o "get-pip.py"
$ chmod +x get-pip.py
$ ./get-pip.py

# install mkdocs, mkdocs material and other plugins
$ pip install mkdocs
$ pip install mkdocs-material
$ pip install mkdocs-git-revision-date-localized-plugin
```

Once the packages are installed, navigate to the root folder and type the following:
```bash
# build the site. This should create a new /site folder
$ mkdocs build
# OR build the site, and turn-on watch for live editing
$ mkdocs serve
```

The new docs should be available on `<localhost>:8000`.

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
