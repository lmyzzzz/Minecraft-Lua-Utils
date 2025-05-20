package com.luautils.lua.lua.functions.utils;

import org.luaj.vm2.LuaValue;
import org.luaj.vm2.Varargs;
import org.luaj.vm2.lib.VarArgFunction;

public class SleepFunction extends VarArgFunction {
    
    @Override
    public Varargs invoke(Varargs args) {
        if (args.narg() < 1) {
            return LuaValue.valueOf("Error: Sleep requires 1 argument (milliseconds)");
        }
        
        try {
            long ms = (long) args.checkdouble(1);
            
            if (ms <= 0) {
                return LuaValue.valueOf("Error: Sleep time must be positive");
            }
            
            Thread.sleep(ms);
            return LuaValue.TRUE;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return LuaValue.valueOf("Sleep interrupted");
        } catch (Exception e) {
            e.printStackTrace();
            return LuaValue.valueOf("Error: " + e.getMessage());
        }
    }
} 