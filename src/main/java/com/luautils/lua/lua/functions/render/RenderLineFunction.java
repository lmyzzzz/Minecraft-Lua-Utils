package com.luautils.lua.lua.functions.render;

import com.luautils.lua.LuaUtils;
import com.luautils.lua.util.RenderUtil;
import net.minecraft.client.Minecraft;
import net.minecraftforge.client.event.RenderWorldLastEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import org.luaj.vm2.LuaTable;
import org.luaj.vm2.LuaValue;
import org.luaj.vm2.Varargs;
import org.luaj.vm2.lib.VarArgFunction;

import java.awt.Color;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class RenderLineFunction extends VarArgFunction {
    
    private static final Map<String, LineData> LINES = new ConcurrentHashMap<>();
    private static boolean registered = false;
    
    public RenderLineFunction() {
        // Register event handler if not already done
        if (!registered) {
            MinecraftForge.EVENT_BUS.register(new RenderHandler());
            registered = true;
        }
    }
    
    /**
     * Clears all active line renders
     * @return Number of lines cleared
     */
    public static int clearAllLines() {
        int count = LINES.size();
        LINES.clear();
        System.out.println("[LuaUtils] Cleared " + count + " rendered lines");
        return count;
    }
    
    @Override
    public Varargs invoke(Varargs args) {
        try {
            // Check for correct argument count
            if (args.narg() < 9) {
                return LuaValue.valueOf("Error: RenderLine requires 9 arguments (startx, starty, startz, endx, endy, endz, color, opacity, width)");
            }
            
            // Parse start position
            final double startX = args.checkdouble(1);
            final double startY = args.checkdouble(2);
            final double startZ = args.checkdouble(3);
            
            // Parse end position
            final double endX = args.checkdouble(4);
            final double endY = args.checkdouble(5);
            final double endZ = args.checkdouble(6);
            
            // Parse color
            final String colorStr = args.checkjstring(7);
            
            // Parse opacity
            final float opacity = (float) args.checkdouble(8);
            if (opacity < 0f || opacity > 1f) {
                return LuaValue.valueOf("Error: Opacity must be between 0.0 and 1.0");
            }
            
            // Parse line width
            final float width = (float) args.checkdouble(9);
            if (width <= 0f) {
                return LuaValue.valueOf("Error: Width must be greater than 0");
            }
            
            // Parse color
            Color color = RenderUtil.parseColor(colorStr, opacity);
            
            // Generate a unique ID for this line
            final String lineId = UUID.randomUUID().toString();
            
            // Create line data with active flag
            LineData data = new LineData(startX, startY, startZ, endX, endY, endZ, color, width, true);
            
            // Run on main thread to safely add the line
            LuaUtils.getInstance().getLuaManager().runOnMainThread(() -> {
                LINES.put(lineId, data);
            });
            
            // Return a table with methods to control the line
            LuaTable lineRef = new LuaTable();
            
            // Add ID to the table
            lineRef.set("id", LuaValue.valueOf(lineId));
            
            // Add remove method to disable the line
            lineRef.set("remove", new VarArgFunction() {
                @Override
                public Varargs invoke(Varargs args) {
                    LuaUtils.getInstance().getLuaManager().runOnMainThread(() -> {
                        LINES.remove(lineId);
                    });
                    return LuaValue.TRUE;
                }
            });
            
            // Add setColor method to change the color
            lineRef.set("setColor", new VarArgFunction() {
                @Override
                public Varargs invoke(Varargs args) {
                    if (args.narg() < 2) {
                        return LuaValue.valueOf("Error: setColor requires color and opacity arguments");
                    }
                    
                    try {
                        final String newColorStr = args.checkjstring(1);
                        final float newOpacity = (float) args.checkdouble(2);
                        
                        if (newOpacity < 0f || newOpacity > 1f) {
                            return LuaValue.valueOf("Error: Opacity must be between 0.0 and 1.0");
                        }
                        
                        final Color newColor = RenderUtil.parseColor(newColorStr, newOpacity);
                        
                        LuaUtils.getInstance().getLuaManager().runOnMainThread(() -> {
                            LineData existingData = LINES.get(lineId);
                            if (existingData != null) {
                                existingData.color = newColor;
                            }
                        });
                        
                        return LuaValue.TRUE;
                    } catch (Exception e) {
                        e.printStackTrace();
                        return LuaValue.valueOf("Error: " + e.getMessage());
                    }
                }
            });
            
            // Add setWidth method to change the line width
            lineRef.set("setWidth", new VarArgFunction() {
                @Override
                public Varargs invoke(Varargs args) {
                    if (args.narg() < 1) {
                        return LuaValue.valueOf("Error: setWidth requires width argument");
                    }
                    
                    try {
                        final float newWidth = (float) args.checkdouble(1);
                        
                        if (newWidth <= 0f) {
                            return LuaValue.valueOf("Error: Width must be greater than 0");
                        }
                        
                        LuaUtils.getInstance().getLuaManager().runOnMainThread(() -> {
                            LineData existingData = LINES.get(lineId);
                            if (existingData != null) {
                                existingData.width = newWidth;
                            }
                        });
                        
                        return LuaValue.TRUE;
                    } catch (Exception e) {
                        e.printStackTrace();
                        return LuaValue.valueOf("Error: " + e.getMessage());
                    }
                }
            });
            
            // Add setPosition method to change both start and end positions
            lineRef.set("setPosition", new VarArgFunction() {
                @Override
                public Varargs invoke(Varargs args) {
                    if (args.narg() < 6) {
                        return LuaValue.valueOf("Error: setPosition requires 6 arguments (startx, starty, startz, endx, endy, endz)");
                    }
                    
                    try {
                        final double newStartX = args.checkdouble(1);
                        final double newStartY = args.checkdouble(2);
                        final double newStartZ = args.checkdouble(3);
                        final double newEndX = args.checkdouble(4);
                        final double newEndY = args.checkdouble(5);
                        final double newEndZ = args.checkdouble(6);
                        
                        LuaUtils.getInstance().getLuaManager().runOnMainThread(() -> {
                            LineData existingData = LINES.get(lineId);
                            if (existingData != null) {
                                existingData.startX = newStartX;
                                existingData.startY = newStartY;
                                existingData.startZ = newStartZ;
                                existingData.endX = newEndX;
                                existingData.endY = newEndY;
                                existingData.endZ = newEndZ;
                            }
                        });
                        
                        return LuaValue.TRUE;
                    } catch (Exception e) {
                        e.printStackTrace();
                        return LuaValue.valueOf("Error: " + e.getMessage());
                    }
                }
            });
            
            // Add isActive method to check if the line is still active
            lineRef.set("isActive", new VarArgFunction() {
                @Override
                public Varargs invoke(Varargs args) {
                    return LuaValue.valueOf(LINES.containsKey(lineId));
                }
            });
            
            return lineRef;
        } catch (Exception e) {
            e.printStackTrace();
            return LuaValue.valueOf("Error: " + e.getMessage());
        }
    }
    
    private static class LineData {
        double startX, startY, startZ;
        double endX, endY, endZ;
        Color color;
        float width;
        boolean active;
        
        LineData(double startX, double startY, double startZ, 
                 double endX, double endY, double endZ, 
                 Color color, float width, boolean active) {
            this.startX = startX;
            this.startY = startY;
            this.startZ = startZ;
            this.endX = endX;
            this.endY = endY;
            this.endZ = endZ;
            this.color = color;
            this.width = width;
            this.active = active;
        }
    }
    
    public static class RenderHandler {
        
        @SubscribeEvent
        public void onRenderWorldLast(RenderWorldLastEvent event) {
            if (LINES.isEmpty() || Minecraft.getMinecraft().thePlayer == null) {
                return;
            }
            
            try {
                // Clean up expired lines
                Iterator<Map.Entry<String, LineData>> it = LINES.entrySet().iterator();
                while (it.hasNext()) {
                    Map.Entry<String, LineData> entry = it.next();
                    LineData data = entry.getValue();
                    
                    // Calculate the average position of the line
                    double avgX = (data.startX + data.endX) / 2.0;
                    double avgY = (data.startY + data.endY) / 2.0;
                    double avgZ = (data.startZ + data.endZ) / 2.0;
                    
                    // If line is too far away, remove it
                    if (Minecraft.getMinecraft().thePlayer.getDistanceSq(avgX, avgY, avgZ) > 262144) { // 512 blocks squared
                        it.remove();
                    }
                }
                
                // Render all active lines
                for (Map.Entry<String, LineData> entry : LINES.entrySet()) {
                    LineData data = entry.getValue();
                    if (data.active) {
                        RenderUtil.drawLine(
                            data.startX, data.startY, data.startZ,
                            data.endX, data.endY, data.endZ,
                            data.color, data.width
                        );
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
} 