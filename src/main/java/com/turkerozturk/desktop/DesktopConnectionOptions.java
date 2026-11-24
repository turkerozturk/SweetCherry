package com.turkerozturk.desktop;
import java.nio.file.*;
import java.util.*;
import javax.swing.*;
import org.yaml.snakeyaml.*;
import org.yaml.snakeyaml.constructor.SafeConstructor;
/** Only interactive launches without explicit external connection settings ask for a port. */
public final class DesktopConnectionOptions {
 private DesktopConnectionOptions(){}
 static boolean needsPrompt(Path directory,String[] args){
  for(String arg:args)if(arg.startsWith("--server.port=")||arg.startsWith("--server.http.port=")||arg.startsWith("--spring.config.")||arg.startsWith("--spring.profiles.active="))return false;
  for(String key:List.of("server.port","server.http.port"))if(System.getProperty(key)!=null||System.getenv(key.toUpperCase(Locale.ROOT).replace('.','_'))!=null)return false;
  for(String key:List.of("SPRING_APPLICATION_JSON","SPRING_CONFIG_LOCATION","SPRING_CONFIG_ADDITIONAL_LOCATION","SPRING_CONFIG_IMPORT","SPRING_PROFILES_ACTIVE"))if(System.getenv(key)!=null)return false;
  for(String key:List.of("spring.config.location","spring.config.additional-location","spring.config.import","spring.profiles.active"))if(System.getProperty(key)!=null)return false;
  try{
   var options=new LoaderOptions();options.setCodePointLimit(1_048_576);options.setMaxAliasesForCollections(20);
   var yaml=new Yaml(new SafeConstructor(options));
   for(Path parent:List.of(directory,directory.resolve("config")))for(String ext:List.of(".yml",".yaml")){
    Path file=parent.resolve("application"+ext);if(!Files.exists(file))continue;
    if(Files.size(file)>1_048_576)return false;
    try(var reader=Files.newBufferedReader(file)){for(Object document:yaml.loadAll(reader))if(document instanceof Map<?,?> map){
     if(map.get("server.port")!=null||map.get("server.http.port")!=null)return false;
     if(map.get("server") instanceof Map<?,?> server&&(server.get("port")!=null||server.get("http") instanceof Map<?,?> http&&http.get("port")!=null))return false;
    }}
   }
  }catch(Exception invalid){return false;} // Let Spring report configuration errors; never hide them with overrides.
  return true;
 }
 static String[] arguments(String[] original,String protocol,int port){
  if(!Set.of("http","https").contains(protocol)||port<1||port>65535)throw new IllegalArgumentException("Invalid connection settings");
  var args=new ArrayList<>(List.of(original));args.add("--server.port="+port);
  args.add("--server.ssl.enabled="+protocol.equals("https"));args.add("--server.http.enabled=false");return args.toArray(String[]::new);
 }
 public static String[] choose(Path directory,String[] args)throws Exception{
  if(!needsPrompt(directory,args))return args;
  final String[][] result={null};SwingUtilities.invokeAndWait(()->{
   boolean tr=Locale.getDefault().getLanguage().equals("tr");
   var protocol=new JComboBox<>(new String[]{"http","https"});var port=new JSpinner(new SpinnerNumberModel(8080,1,65535,1));
   var panel=new JPanel(new java.awt.GridLayout(0,2,8,8));
   panel.add(new JLabel(tr?"Protokol":"Protocol"));panel.add(protocol);panel.add(new JLabel("Port"));panel.add(port);
   panel.add(new JLabel(tr?"HTTPS için YAML SSL ayarları gerekir.":"HTTPS requires SSL settings in YAML."));panel.add(new JLabel(""));
   if(JOptionPane.showConfirmDialog(null,panel,"SweetCherry",JOptionPane.OK_CANCEL_OPTION,JOptionPane.PLAIN_MESSAGE)==JOptionPane.OK_OPTION)
    result[0]=arguments(args,(String)protocol.getSelectedItem(),((Number)port.getValue()).intValue());
  });return result[0];
 }
}
