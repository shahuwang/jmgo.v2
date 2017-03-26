package com.shahuwang.jmgo;

/**
 * Created by rickey on 2017/3/27.
 */
public class Credential {
    private String username;
    private String password;
    private String source; // 认证时所使用的数据库，默认是admin
    private String service; // 使用GSSAPI mechanism的名称，默认是mongodb
    private String serviceHost; // 使用GSSAPI mechanism的地址,默认和mongod一样
    private String mechanism;  // https://docs.mongodb.com/manual/core/authentication-mechanisms/

    public String getUsername() {
        return username;
    }

    public Credential setUsername(String username) {
        this.username = username;
        return this;
    }

    public String getPassword() {
        return password;
    }

    public Credential setPassword(String password) {
        this.password = password;
        return this;
    }

    public String getSource() {
        return source;
    }

    public Credential setSource(String source) {
        this.source = source;
        return this;
    }

    public String getService() {
        String service = this.service;
        if(this.service == "" || this.service == null){
            service = "mongodb";
        }
        return service;
    }

    public Credential setService(String service) {
        this.service = service;
        return this;
    }

    public String getServiceHost() {
        return serviceHost;
    }

    public Credential setServiceHost(String serviceHost) {
        this.serviceHost = serviceHost;
        return this;
    }

    public String getMechanism() {
        return mechanism;
    }

    public Credential setMechanism(String mechanism) {
        this.mechanism = mechanism;
        return this;
    }
}
