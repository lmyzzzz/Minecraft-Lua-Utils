package com.luautils.lua;

import com.luautils.lua.command.CommandLUA;
import com.luautils.lua.lua.LuaManager;
import com.luautils.lua.util.ChatUtil;
import net.minecraftforge.client.ClientCommandHandler;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.Mod.EventHandler;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

@Mod(modid = LuaUtils.MODID, version = LuaUtils.VERSION, clientSideOnly = true)
public class LuaUtils {
    public static final String MODID = "luautils";
    public static final String VERSION = "1.0";
    
    private static LuaUtils instance;
    private LuaManager luaManager;
    private File configDir;
    private File scriptDir;
    
    @EventHandler
    public void preInit(FMLPreInitializationEvent event) {
        instance = this;
        
        try {
            // 初始化配置目录
            configDir = new File(event.getModConfigurationDirectory(), MODID);
            if (!configDir.exists() && !configDir.mkdirs()) {
                System.err.println("[LuaUtils] Failed to create config directory: " + configDir.getAbsolutePath());
            }
            
            // 初始化脚本目录
            scriptDir = new File(configDir, "scripts");
            if (!scriptDir.exists() && !scriptDir.mkdirs()) {
                System.err.println("[LuaUtils] Failed to create scripts directory: " + scriptDir.getAbsolutePath());
            }
            
            // 创建一个示例脚本，如果目录创建成功但还没有脚本
            createExampleScriptIfNeeded();
            
            luaManager = new LuaManager();
        } catch (Exception e) {
            System.err.println("[LuaUtils] Error during initialization:");
            e.printStackTrace();
        }
    }
    
    @EventHandler
    public void init(FMLInitializationEvent event) {
        try {
            ClientCommandHandler.instance.registerCommand(new CommandLUA());
            MinecraftForge.EVENT_BUS.register(luaManager);
            System.out.println("[LuaUtils] Lua Utils initialized successfully.");
            System.out.println("[LuaUtils] Scripts directory: " + scriptDir.getAbsolutePath());
        } catch (Exception e) {
            System.err.println("[LuaUtils] Error during initialization:");
            e.printStackTrace();
        }
    }
    
    private void createExampleScriptIfNeeded() {
        if (scriptDir.exists() && scriptDir.isDirectory()) {
            File[] files = scriptDir.listFiles((dir, name) -> name.endsWith(".lua"));
            if (files == null || files.length == 0) {
                File exampleFile = new File(scriptDir, "example.lua");
                try (FileWriter writer = new FileWriter(exampleFile)) {
                    writer.write("-- Example Lua Utils script\n");
                    writer.write("-- Use this as a template for your own scripts\n\n");
                    writer.write("-- Get and display player position\n");
                    writer.write("local pos = GetPlayerPos()\n");
                    writer.write("SendLog(\"Current position:\", pos.x, pos.y, pos.z)\n");
                    writer.write("SendLog(\"Current rotation:\", pos.pitch, pos.yaw)\n\n");
                    writer.write("-- Wait a moment\n");
                    writer.write("Sleep(1000)\n\n");
                    writer.write("-- Send a message to chat\n");
                    writer.write("SendLog(\"Hello from Lua Utils script!\")\n");
                    System.out.println("[LuaUtils] Created example script at: " + exampleFile.getAbsolutePath());
                } catch (IOException e) {
                    System.err.println("[LuaUtils] Failed to create example script:");
                    e.printStackTrace();
                }
            }
        }
    }
    
    public static LuaUtils getInstance() {
        return instance;
    }
    
    public LuaManager getLuaManager() {
        return luaManager;
    }
    
    public File getConfigDir() {
        return configDir;
    }
    
    public File getScriptDir() {
        return scriptDir;
    }
} 