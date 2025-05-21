package com.luautils.lua.lua.functions.render;

import org.luaj.vm2.LuaValue;
import org.luaj.vm2.Varargs;
import org.luaj.vm2.lib.VarArgFunction;

/**
 * Lua function to clear all render elements created by Lua scripts.
 * This has the same effect as running "/lua resetrender" command.
 */
public class ClearRenderFunction extends VarArgFunction {
    
    @Override
    public Varargs invoke(Varargs args) {
        try {
            // Clear all block highlights
            HighlightBlockFunction.clearAllHighlights();
            
            // Clear all lines
            RenderLineFunction.clearAllLines();
            
            // Return nil (no return value)
            return LuaValue.NIL;
        } catch (Exception e) {
            e.printStackTrace();
            return LuaValue.valueOf("Error: " + e.getMessage());
        }
    }
} 