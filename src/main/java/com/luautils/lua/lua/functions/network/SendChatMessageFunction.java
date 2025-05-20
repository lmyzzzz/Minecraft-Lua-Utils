package com.luautils.lua.lua.functions.network;

import com.luautils.lua.LuaUtils;
import com.luautils.lua.util.ChatUtil;
import org.luaj.vm2.LuaString;
import org.luaj.vm2.LuaValue;
import org.luaj.vm2.Varargs;
import org.luaj.vm2.lib.VarArgFunction;

public class SendChatMessageFunction extends VarArgFunction {
    
    @Override
    public Varargs invoke(Varargs args) {
        try {
            StringBuilder message = new StringBuilder();
            
            // Process all arguments and concatenate them
            for (int i = 1; i <= args.narg(); i++) {
                LuaValue arg = args.arg(i);
                
                message.append(arg.tojstring());
            }
            
            String finalMessage = message.toString();
            
            // Run on main thread to safely interact with Minecraft
            LuaUtils.getInstance().getLuaManager().runOnMainThread(() -> {
                ChatUtil.sendChatMessage(finalMessage);
            });
            
            return LuaValue.TRUE;
        } catch (Exception e) {
            e.printStackTrace();
            return LuaValue.valueOf("Error: " + e.getMessage());
        }
    }
} 