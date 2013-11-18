/*
 * Copyright 2011 NTS New Technology Systems GmbH. All Rights reserved. NTS PROPRIETARY/CONFIDENTIAL. Use is
 * subject to NTS License Agreement. Address: Doernbacher Strasse 126, A-4073 Wilhering, Austria Homepage:
 * www.ntswincash.com
 */

package com.loadbalancing.model;

import java.io.Serializable;

public class Range implements Serializable {
    // apparently the library doesn't propperly do the serialization
    // unless the fields are public
    public double from;
    public double to;

    public Range() {
    }

    public Range(double from, double to) {
        this.from = from;
        this.to = to;
    }

    /**
     * @return the from
     */
    public double getFrom() {
        return from;
    }

    /**
     * @param from the from to set
     */
    public void setFrom(double from) {
        this.from = from;
    }

    /**
     * @return the to
     */
    public double getTo() {
        return to;
    }

    /**
     * @param to the to to set
     */
    public void setTo(double to) {
        this.to = to;
    }

    @Override
    public String toString() {
        return "Range [from=" + from + ", to=" + to + "]";
    }

}
