# Flingr
*An easy way to transfer files between PC and mobile phones.*
Authors:   [Adam Cubel](https://gitlab.com/adamcubel) and [Austin Alderton](https://github.com/AustinA)

Flingr allows users to use 6-character activation code to automatically populate 
SSH connection information to send files from an Android device to a PC. This
project was inspired by account setup on Rokus and Smart TVs.

## AWS
The AWS web service uses a REST API with encoded JSON messages that registers
PC connection information to an activation code and stores them in a database.

## PC
The desktop application runs in the background and registers the computer's IP
configuration to the AWS lambda service by a random, ratcheting, 6-character activation
code.  An OpenSSH instance creates and maintains ssh file transfer connections initiated 
by a mobile device.

## Android
The mobile application gathers SSH connection information by querying the user for a matching
activation code, then making an AWS REST call to retrieve them.  Once a user name and password
is provided, files can be sent to the PC running Flingr using the Android system share menu.