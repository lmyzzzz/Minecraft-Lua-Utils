package com.luautils.lua.command;

import com.luautils.lua.LuaUtils;
import com.luautils.lua.lua.LuaManager;
import com.luautils.lua.util.ChatUtil;
import net.minecraft.command.CommandBase;
import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.util.BlockPos;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.EnumChatFormatting;

import java.io.File;
import java.util.Arrays;
import java.util.List;

public class CommandLUA extends CommandBase {
    
    @Override
    public String getCommandName() {
        return "lua";
    }
    
    @Override
    public String getCommandUsage(ICommandSender sender) {
        return "/lua execute <script_name> | /lua stop | /lua list | /lua reload";
    }
    
    @Override
    public List<String> getCommandAliases() {
        return Arrays.asList("luautils");
    }
    
    @Override
    public void processCommand(ICommandSender sender, String[] args) throws CommandException {
        if (args.length < 1) {
            ChatUtil.sendMessage(EnumChatFormatting.RED + "Invalid command usage. Try: " + getCommandUsage(sender));
            return;
        }
        
        String subCommand = args[0].toLowerCase();
        
        switch (subCommand) {
            case "execute":
                executeScript(sender, args);
                break;
            case "stop":
                stopScript(sender);
                break;
            case "list":
                listScripts(sender);
                break;
            case "reload":
                reloadScripts(sender);
                break;
            default:
                ChatUtil.sendMessage(EnumChatFormatting.RED + "Unknown subcommand: " + subCommand);
                break;
        }
    }
    
    private void executeScript(ICommandSender sender, String[] args) {
        if (args.length < 2) {
            ChatUtil.sendMessage(EnumChatFormatting.RED + "Usage: /lua execute <script_name>");
            return;
        }
        
        String scriptName = args[1];
        if (!scriptName.endsWith(".lua")) {
            scriptName += ".lua";
        }
        
        File scriptFile = new File(LuaUtils.getInstance().getScriptDir(), scriptName);
        
        if (!scriptFile.exists()) {
            ChatUtil.sendMessage(EnumChatFormatting.RED + "Script file not found: " + scriptName);
            return;
        }
        
        try {
            LuaManager luaManager = LuaUtils.getInstance().getLuaManager();
            luaManager.executeScript(scriptFile);
            ChatUtil.sendMessage(EnumChatFormatting.GREEN + "Executing script: " + scriptName);
        } catch (Exception e) {
            ChatUtil.sendMessage(EnumChatFormatting.RED + "Error executing script: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    private void stopScript(ICommandSender sender) {
        LuaManager luaManager = LuaUtils.getInstance().getLuaManager();
        luaManager.stopCurrentScript();
        ChatUtil.sendMessage(EnumChatFormatting.YELLOW + "Script execution stopped.");
    }
    
    private void listScripts(ICommandSender sender) {
        File scriptDir = LuaUtils.getInstance().getScriptDir();
        File[] scripts = scriptDir.listFiles((dir, name) -> name.endsWith(".lua"));
        
        if (scripts == null || scripts.length == 0) {
            ChatUtil.sendMessage(EnumChatFormatting.YELLOW + "No scripts found in " + scriptDir.getAbsolutePath());
            return;
        }
        
        ChatUtil.sendMessage(EnumChatFormatting.GREEN + "Available scripts:");
        for (File script : scripts) {
            ChatUtil.sendMessage("- " + script.getName());
        }
    }
    
    private void reloadScripts(ICommandSender sender) {
        LuaManager luaManager = LuaUtils.getInstance().getLuaManager();
        luaManager.initializeLuaEnvironment();
        ChatUtil.sendMessage(EnumChatFormatting.GREEN + "Lua environment reloaded. All script caches cleared.");
    }
    
    @Override
    public boolean canCommandSenderUseCommand(ICommandSender sender) {
        return true;
    }
    
    @Override
    public List<String> addTabCompletionOptions(ICommandSender sender, String[] args, BlockPos pos) {
        if (args.length == 1) {
            return getListOfStringsMatchingLastWord(args, "execute", "stop", "list", "reload");
        } else if (args.length == 2 && args[0].equalsIgnoreCase("execute")) {
            File scriptDir = LuaUtils.getInstance().getScriptDir();
            File[] scripts = scriptDir.listFiles((dir, name) -> name.endsWith(".lua"));
            
            if (scripts != null) {
                String[] scriptNames = new String[scripts.length];
                for (int i = 0; i < scripts.length; i++) {
                    scriptNames[i] = scripts[i].getName().replace(".lua", "");
                }
                
                return getListOfStringsMatchingLastWord(args, scriptNames);
            }
        }
        
        return null;
    }
} 