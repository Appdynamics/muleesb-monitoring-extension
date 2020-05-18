package com.appdynamics.extensions.muleesb.config;


import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement(name = "stats")
@XmlAccessorType(XmlAccessType.FIELD)
public class Stats {
    @XmlElement(name = "stat")
    private Stat[] stat;

    public Stat[] getStat() {
        return stat;
    }

    public void setStat(Stat[] stat) {
        this.stat = stat;
    }

}
