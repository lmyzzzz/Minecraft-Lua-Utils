package com.luautils.lua.lua.functions.algo;

import org.luaj.vm2.LuaValue;
import org.luaj.vm2.Varargs;
import org.luaj.vm2.lib.OneArgFunction;

/**
 * Converts a floating point coordinate to the center coordinate of the block it's in.
 * Works for any coordinate (x, y, or z).
 * 
 * Examples:
 * - 5.7 -> 5.5
 * - -3.2 -> -3.5
 * - For y coordinate: 67.9 -> 67.5
 */
public class TbFunction extends OneArgFunction {
    
    @Override
    public LuaValue call(LuaValue arg) {
        try {
            // Check argument is a number
            if (!arg.isnumber()) {
                return LuaValue.valueOf("Error: tb requires a number argument");
            }
            
            // Get the coordinate as a double
            double coord = arg.todouble();
            
            // Determine which block this coordinate is in (floor)
            int blockPos = (int) Math.floor(coord);
            
            // Add 0.5 to get the center of the block
            double centerCoord = blockPos + 0.5;
            
            // Return the center coordinate
            return LuaValue.valueOf(centerCoord);
        } catch (Exception e) {
            e.printStackTrace();
            return LuaValue.valueOf("Error: " + e.getMessage());
        }
    }
} 