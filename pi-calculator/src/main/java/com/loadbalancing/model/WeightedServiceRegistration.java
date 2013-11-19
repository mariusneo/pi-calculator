/*
 * Copyright 2011 NTS New Technology Systems GmbH. All Rights reserved.
 * NTS PROPRIETARY/CONFIDENTIAL. Use is subject to NTS License Agreement.
 * Address: Doernbacher Strasse 126, A-4073 Wilhering, Austria
 * Homepage: www.ntswincash.com
 */

package com.loadbalancing.model;

/**
 * @author mga
 *
 */
public class WeightedServiceRegistration {
    public String channelId;
    public int weight;
    
    public WeightedServiceRegistration(){
    }
    
    public WeightedServiceRegistration(String channelId, int weight){
        this.channelId = channelId;
        this.weight = weight;
    }

    /**
     * @return the channelId
     */
    public String getChannelId() {
        return channelId;
    }

    /**
     * @param channelId the channelId to set
     */
    public void setChannelId(String channelId) {
        this.channelId = channelId;
    }

    /**
     * @return the weight
     */
    public int getWeight() {
        return weight;
    }

    /**
     * @param weight the weight to set
     */
    public void setWeight(int weight) {
        this.weight = weight;
    }
    
}
