pi-calculator
=============

Simple load-balancing project used for the calculation of PI constant.


This is a simple project based on the library Eneter for working with load balancers.

The project's code is similar to the implementation found here :
http://www.codeproject.com/Articles/318290/How-to-Implement-Load-Balancing-to-Distribute-Work

In addition to the code provided by Eneter, which provides a Roundrobin even task distribution scheme,
this project implements a weighted task distribution load balancing scheme (see package com.loadbalancing.loadbalancer)


In order to run the demo, do the following:
1. start 3 instances of com.loadbalancing.server.PiCalculator by supplying 8061, respectively 8062, respectively 8062 as program arguments
2. start a load balancer (com.loadbalancing.loadbalancer.EvenLoadBalancer/com.loadbalancing.loadbalancer.WeightedLoadBalancer)
3. start the client com.loadbalancing.client.Client

Eneter Messaging Framework : http://www.eneter.net/