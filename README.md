# FreeFallDetection
Free fall detection app created with Android Studio.



The fall detection was based on available literature: [1], [2] and takes into considation the followings:

1) In the beginning of a a fall, there is always a free fall happening, meaning that the vector sum will be near zero, in our case <0.3g:

![2](https://user-images.githubusercontent.com/30274421/72463155-4698b580-37db-11ea-8e24-c02ec2ff65c1.png)

2) Next, the landing phase is taking place, in which the vector sum is way larger than 1g, in our case 2g:

![2_1](https://user-images.githubusercontent.com/30274421/72463156-4698b580-37db-11ea-87b7-4a31a1818fb4.png)

3) Finally, if the person is injured and cannot get up, then the he/she remains motionless in the ground and therefore the acceleration is approximately 1g. 

![2_2](https://user-images.githubusercontent.com/30274421/72463157-47314c00-37db-11ea-934e-3512cb984367.png)

The 2nd phase should happen in approx. 1s after the free fall (1st phase) is detected, because a typical fall lasts approximaty 1s. In addition, the user should remain on the ground for at least 2seconds in order for this to be considered a fall. If any of this does not happen, a free fall is NOT detected. 

Summing up, the graphical representation of all the above, will look similar to this [1]:

![3](https://user-images.githubusercontent.com/30274421/72462760-814e1e00-37da-11ea-956b-0b5a2def5042.png)



The main screen of the application contains some buttons on the top (statistics, change language, google map, usb connection indication and a SOS button) at the bottom.

![1_mainScreen](https://user-images.githubusercontent.com/30274421/72461473-c58bef00-37d7-11ea-9981-1449a5a56fdc.png)

If the usb is connected, the app monitors for user falls and if it's not (usb letters are red), then it monitors for earthquakes. 



In cases of some emergencies (fall detected, SOS button pressed) an SMS message is sent to selected contacts.

The user is also given the option to see all the points of danger (earthquake, sos button, fall, abort) on google map. 

![4_map](https://user-images.githubusercontent.com/30274421/72461475-c58bef00-37d7-11ea-86d6-a89d9cc806f6.png)



[1] Ge, Yujia, and Bin Xu. "Detecting Falls Using Accelerometers by Adaptive
Thresholds in Mobile Devices." JCP 9.7 (2014): 1553-1559.

[2] Sposaro, Frank, and Gary Tyson. "iFall: an Android application for fall monitoring
and response." 2009 Annual International Conference of the IEEE Engineering in
Medicine and Biology Society. IEEE, 2009.
