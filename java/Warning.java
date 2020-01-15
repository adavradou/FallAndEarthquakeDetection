package com.example.unipismartalert;

public class Warning {

    String id;
    String user;
    String emergency;
    Double latitude;
    Double longitude;
    String timestamp;

    public Warning(){

    }

    public Warning(String id, String user,String emergency,Double latitude,Double longitude, String timestamp){
        this.id = id;
        this.user = user;
        this.emergency = emergency;
        this.latitude = latitude;
        this.longitude = longitude;
        this.timestamp = timestamp;

    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getUser() {
        return user;
    }

    public void setUser(String user) {
        this.user = user;
    }

    public String getEmergency() {
        return emergency;
    }

    public void setEmergency(String emergency) {
        this.emergency = emergency;
    }

    public Double getLatitude() {
        return latitude;
    }

    public void setLatitude(Double latitude) {
        this.latitude = latitude;
    }

    public Double getLongitude() {
        return longitude;
    }

    public void setLongitude(Double longitude) {
        this.longitude = longitude;
    }

    public String getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(String timestamp) {
        this.timestamp = timestamp;
    }

}
