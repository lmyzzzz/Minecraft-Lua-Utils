package com.luautils.lua.lua.functions.player;

import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.EntityPlayer;
import org.luaj.vm2.LuaTable;
import org.luaj.vm2.LuaValue;
import org.luaj.vm2.Varargs;
import org.luaj.vm2.lib.OneArgFunction;
import org.luaj.vm2.lib.ZeroArgFunction;

public class GetPlayerPosFunction extends ZeroArgFunction {
    
    @Override
    public LuaValue call() {
        try {
            Minecraft mc = Minecraft.getMinecraft();
            EntityPlayer player = mc.thePlayer;
            
            if (player == null) {
                return LuaValue.NIL;
            }
            
            LuaTable position = new LuaTable();
            position.set("x", LuaValue.valueOf((double) player.posX));
            position.set("y", LuaValue.valueOf((double) player.posY));
            position.set("z", LuaValue.valueOf((double) player.posZ));
            position.set("pitch", LuaValue.valueOf((double) player.rotationPitch));
            position.set("yaw", LuaValue.valueOf((double) player.rotationYaw));
            
            return position;
        } catch (Exception e) {
            e.printStackTrace();
            return LuaValue.NIL;
        }
    }
} 