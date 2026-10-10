package com.turkerozturk.multipledatabases;

/** Explicit form fields prevent arbitrary property binding. Password is never populated on edit. */
public class TenantWizardForm {
    public String tenant = "", revision = "", name = "", type = "sqlite", path = "", url = "", username = "", password = "";
    public String role = "USER", newNodeName = "", newNodeTags = "", maxEmbeddedFileSizeMB = "9";
    public boolean writable, allowLegacySchemaUpgrade, clearPassword;
    public String getTenant(){return tenant;} public void setTenant(String v){tenant=v;}
    public String getRevision(){return revision;} public void setRevision(String v){revision=v;}
    public String getName(){return name;} public void setName(String v){name=v;}
    public String getType(){return type;} public void setType(String v){type=v;}
    public String getPath(){return path;} public void setPath(String v){path=v;}
    public String getUrl(){return url;} public void setUrl(String v){url=v;}
    public String getUsername(){return username;} public void setUsername(String v){username=v;}
    public String getPassword(){return password;} public void setPassword(String v){password=v;}
    public String getRole(){return role;} public void setRole(String v){role=v;}
    public String getNewNodeName(){return newNodeName;} public void setNewNodeName(String v){newNodeName=v;}
    public String getNewNodeTags(){return newNodeTags;} public void setNewNodeTags(String v){newNodeTags=v;}
    public String getMaxEmbeddedFileSizeMB(){return maxEmbeddedFileSizeMB;} public void setMaxEmbeddedFileSizeMB(String v){maxEmbeddedFileSizeMB=v;}
    public boolean isWritable(){return writable;} public void setWritable(boolean v){writable=v;}
    public boolean isAllowLegacySchemaUpgrade(){return allowLegacySchemaUpgrade;} public void setAllowLegacySchemaUpgrade(boolean v){allowLegacySchemaUpgrade=v;}
    public boolean isClearPassword(){return clearPassword;} public void setClearPassword(boolean v){clearPassword=v;}
}
