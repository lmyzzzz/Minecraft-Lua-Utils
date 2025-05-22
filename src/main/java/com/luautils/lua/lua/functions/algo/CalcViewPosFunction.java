package com.luautils.lua.lua.functions.algo;

import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.EntityPlayer;
import org.luaj.vm2.LuaTable;
import org.luaj.vm2.LuaValue;
import org.luaj.vm2.Varargs;
import org.luaj.vm2.lib.VarArgFunction;

public class CalcViewPosFunction extends VarArgFunction {
    
    @Override
    public Varargs invoke(Varargs args) {
        try {
            // Check for correct argument count
            if (args.narg() < 3) {
                return LuaValue.valueOf("Error: CalcViewPos requires 3 arguments (x, y, z)");
            }
            
            // Parse target position as doubles
            final double targetX = args.checkdouble(1);
            final double targetY = args.checkdouble(2);
            final double targetZ = args.checkdouble(3);
            
            // Get player's current position
            Minecraft mc = Minecraft.getMinecraft();
            EntityPlayer player = mc.thePlayer;
            
            if (player == null) {
                return LuaValue.valueOf("Error: Player not available");
            }
            
            // Calculate vector from player to target
            double deltaX = targetX - player.posX;
            double deltaY = targetY - player.posY - player.getEyeHeight(); // Account for eye height
            double deltaZ = targetZ - player.posZ;
            
            // Calculate horizontal distance
            double horizontalDistance = Math.sqrt(deltaX * deltaX + deltaZ * deltaZ);
            
            // Calculate pitch (vertical angle)
            // Convert from radians to degrees and adjust for Minecraft's coordinate system
            double pitch = -Math.toDegrees(Math.atan2(deltaY, horizontalDistance));
            
            // Calculate yaw (horizontal angle)
            // Convert from radians to degrees and adjust for Minecraft's coordinate system
            double yaw = Math.toDegrees(Math.atan2(deltaZ, deltaX)) - 90.0;
            
            // Normalize yaw to be between -180 and 180
            while (yaw > 180.0) {
                yaw -= 360.0;
            }
            while (yaw < -180.0) {
                yaw += 360.0;
            }
            
            // Create result table
            LuaTable result = new LuaTable();
            result.set("pitch", LuaValue.valueOf(pitch));
            result.set("yaw", LuaValue.valueOf(yaw));
            
            return result;
        } catch (Exception e) {
            e.printStackTrace();
            return LuaValue.valueOf("Error: " + e.getMessage());
        }
    }
} 