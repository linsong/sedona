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
