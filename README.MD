Jenkins DiskCheck Plugin
=============
 
Motivation
---------
 Disk run out of space while running a build , We do use Disk Usage plugin , but it checks for disk usage every hour also mark the slave offline without any notification which is not appropriate

Solution :
--------
 The plugin checks for disk available before starting a job and fail the build is the disk is below the Threshold ( You define the Threshold in a global parameter) , 

-------------
Also there is a Global Configuration which says Disk Recycler if it's set and the disk spaceis low , then it will delete everything in the current job workspace directory,
