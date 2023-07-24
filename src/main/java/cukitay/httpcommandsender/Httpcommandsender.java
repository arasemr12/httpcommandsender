package cukitay.httpcommandsender;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;

public final class Httpcommandsender extends JavaPlugin implements CommandExecutor {
    FileConfiguration config = getConfig();
    HttpServer server = null;

    public void startServer(){
        this.reloadConfig();
        this.config = getConfig();

        try{
            String ip = this.config.getString("ip");
            String password = this.config.getString("password");
            int port = this.config.getInt("port");

            if(ip == null || ip.isEmpty()){
                ip = "localhost";
            }

            if(password == null || password.isEmpty()){
                password = "123";
            }

            if(Integer.toString(port).isEmpty()){
                port = 3000;
            }

            this.server = HttpServer.create(new InetSocketAddress(ip, port), 0);

            ThreadPoolExecutor threadPoolExecutor = (ThreadPoolExecutor) Executors.newFixedThreadPool(2);
            this.server.setExecutor(threadPoolExecutor);

            String finalPassword = password;
            this.server.createContext("/", new HttpHandler() {
                public void send(HttpExchange exchange, String body) throws IOException {
                    OutputStream outputStream = exchange.getResponseBody();

                    exchange.sendResponseHeaders(200, body.length());

                    outputStream.write(body.getBytes());
                    outputStream.flush();
                    outputStream.close();
                }

                @Override
                public void handle(HttpExchange exchange) throws IOException {
                    String query = exchange.getRequestURI().getQuery();
                    if(query == null) {
                        send(exchange,"Query err!");
                        return;
                    }

                    String cmd = "";
                    String pass = "";

                    for (String a : query.split("&")){
                        if(a.split("=")[0].equals("cmd")){
                            cmd = a.split("=")[1];
                        }

                        if(a.split("=")[0].equals("pass")){
                            pass = a.split("=")[1];
                        }
                    }

                    if(!pass.equals(finalPassword)){
                        send(exchange,"Wrong pass!");
                        return;
                    }

                    if(cmd.isEmpty()){
                        send(exchange,"Query err!");
                        return;
                    }

                    getLogger().info(cmd);

                    String finalCmd = cmd;

                    Bukkit.getScheduler().scheduleSyncDelayedTask(Httpcommandsender.this,() -> {
                        Bukkit.dispatchCommand(Bukkit.getConsoleSender(), finalCmd);
                    },1L);

                    send(exchange,"OK"+"\n"+cmd);
                }
            });

            this.server.start();
        }catch (Exception e){
            getLogger().info(e.getMessage());
            System.out.println("Err!" + e.getMessage());
        }
    }

    public void stopServer(){
        this.server.stop(0);
    }

    public void restartPlugin(){
        this.reloadConfig();
    }

    @Override
    public void onEnable() {
        this.config.addDefault("ip","localhost");
        this.config.addDefault("port",3000);
        this.config.addDefault("password","123");

        this.config.options().copyDefaults(true);
        saveConfig();

        startServer();
        this.getCommand("httpcommandsender").setExecutor(new CommandExecutor() {
            @Override
            public boolean onCommand(CommandSender commandSender, Command command, String s, String[] strings) {
                if(!commandSender.isOp()){
                    commandSender.sendMessage(ChatColor.RED + "[http] You are not op!");
                    return true;
                }

                if(strings.length < 1){
                    commandSender.sendMessage(ChatColor.DARK_PURPLE + "HELP");
                    commandSender.sendMessage("Example http req: http://"+Httpcommandsender.this.config.getString("ip")+":"+Httpcommandsender.this.config.getInt("port")+"?cmd=help&pass="+Httpcommandsender.this.config.getString("password"));
                    return true;
                };

                stopServer();
                startServer();

                commandSender.sendMessage(ChatColor.GREEN + "[http] Reloaded!");

                return true;
            }
        });
    }

    @Override
    public void onDisable() {
        stopServer();
    }
}
