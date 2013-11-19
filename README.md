pi-calculator
=============

Simple load-balancing project used for the calculation of PI constant.


This is a simple project based on the library Eneter for working with load balancers.

The project's code is similar to the implementation found here :
http://www.codeproject.com/Articles/318290/How-to-Implement-Load-Balancing-to-Distribute-Work

In addition to the code provided by Eneter, which provides a Roundrobin even task distribution scheme,
this project implements a weighted task distribution load balancing scheme (see package com.loadbalancing.loadbalancer)


In order to run the demo, do the following:
1. start the weight load balancer (com.loadbalancing.loadbalancer.WeightedLoadBalancer)
    - it currently statically uses the ports :
       - 8060 for client connections
       - 8061 for service registrations
2. start 3 instances of com.loadbalancing.server.PiCalculator by supplying port and weight for the services:
 - 8061 5
 - 8062 2
 - 8063 1 
 as program arguments. They will register themselves to the load balancer

3. start the client com.loadbalancing.client.Client

Eneter Messaging Framework : http://www.eneter.net/

Possible improvements :
- the load balancer address could be provided as argument to the client and services so that it doesn't get hardcoded
anymore in their code.