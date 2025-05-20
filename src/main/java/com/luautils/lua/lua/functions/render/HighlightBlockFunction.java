package com.luautils.lua.lua.functions.render;

import com.luautils.lua.LuaUtils;
import com.luautils.lua.util.RenderUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.util.BlockPos;
import net.minecraftforge.client.event.RenderWorldLastEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import org.luaj.vm2.LuaTable;
import org.luaj.vm2.LuaValue;
import org.luaj.vm2.Varargs;
import org.luaj.vm2.lib.VarArgFunction;

import java.awt.Color;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class HighlightBlockFunction extends VarArgFunction {
    
    private static final Map<String, HighlightData> HIGHLIGHTS = new ConcurrentHashMap<>();
    private static boolean registered = false;
    
    public HighlightBlockFunction() {
        // Register event handler if not already done
        if (!registered) {
            MinecraftForge.EVENT_BUS.register(new RenderHandler());
            registered = true;
        }
    }
    
    /**
     * Clears all active block highlights
     * @return Number of highlights cleared
     */
    public static int clearAllHighlights() {
        int count = HIGHLIGHTS.size();
        HIGHLIGHTS.clear();
        System.out.println("[LuaUtils] Cleared " + count + " block highlights");
        return count;
    }
    
    @Override
    public Varargs invoke(Varargs args) {
        try {
            // Check for correct argument count
            if (args.narg() < 6) {
                return LuaValue.valueOf("Error: HighlightBlock requires 6 arguments (x, y, z, color, opacity, type)");
            }
            
            // Parse position
            final int x = args.checkint(1);
            final int y = args.checkint(2);
            final int z = args.checkint(3);
            
            // Parse color
            final String colorStr = args.checkjstring(4);
            
            // Parse opacity
            final float opacity = (float) args.checkdouble(5);
            if (opacity < 0f || opacity > 1f) {
                return LuaValue.valueOf("Error: Opacity must be between 0.0 and 1.0");
            }
            
            // Parse render type
            final String typeStr = args.checkjstring(6).toUpperCase();
            final RenderUtil.RenderType renderType;
            
            if (typeStr.equals("LINE")) {
                renderType = RenderUtil.RenderType.LINE;
            } else if (typeStr.equals("FILL")) {
                renderType = RenderUtil.RenderType.FILL;
            } else {
                return LuaValue.valueOf("Error: Type must be either LINE or FILL");
            }
            
            // Create block position
            final BlockPos pos = new BlockPos(x, y, z);
            
            // Parse color
            Color color = RenderUtil.parseColor(colorStr, opacity);
            
            // Generate a unique ID for this highlight
            final String highlightId = UUID.randomUUID().toString();
            
            // Create highlight data with active flag
            HighlightData data = new HighlightData(pos, color, renderType, true);
            
            // Run on main thread to safely add the highlight
            LuaUtils.getInstance().getLuaManager().runOnMainThread(() -> {
                HIGHLIGHTS.put(highlightId, data);
            });
            
            // Return a table with methods to control the highlight
            LuaTable highlightRef = new LuaTable();
            
            // Add ID to the table
            highlightRef.set("id", LuaValue.valueOf(highlightId));
            
            // Add remove method to disable the highlight
            highlightRef.set("remove", new VarArgFunction() {
                @Override
                public Varargs invoke(Varargs args) {
                    LuaUtils.getInstance().getLuaManager().runOnMainThread(() -> {
                        HIGHLIGHTS.remove(highlightId);
                    });
                    return LuaValue.TRUE;
                }
            });
            
            // Add setColor method to change the color
            highlightRef.set("setColor", new VarArgFunction() {
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
                            HighlightData existingData = HIGHLIGHTS.get(highlightId);
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
            
            // Add setType method to change the render type
            highlightRef.set("setType", new VarArgFunction() {
                @Override
                public Varargs invoke(Varargs args) {
                    if (args.narg() < 1) {
                        return LuaValue.valueOf("Error: setType requires type argument (LINE or FILL)");
                    }
                    
                    try {
                        final String newTypeStr = args.checkjstring(1).toUpperCase();
                        final RenderUtil.RenderType newRenderType;
                        
                        if (newTypeStr.equals("LINE")) {
                            newRenderType = RenderUtil.RenderType.LINE;
                        } else if (newTypeStr.equals("FILL")) {
                            newRenderType = RenderUtil.RenderType.FILL;
                        } else {
                            return LuaValue.valueOf("Error: Type must be either LINE or FILL");
                        }
                        
                        LuaUtils.getInstance().getLuaManager().runOnMainThread(() -> {
                            HighlightData existingData = HIGHLIGHTS.get(highlightId);
                            if (existingData != null) {
                                existingData.renderType = newRenderType;
                            }
                        });
                        
                        return LuaValue.TRUE;
                    } catch (Exception e) {
                        e.printStackTrace();
                        return LuaValue.valueOf("Error: " + e.getMessage());
                    }
                }
            });
            
            // Add isActive method to check if the highlight is still active
            highlightRef.set("isActive", new VarArgFunction() {
                @Override
                public Varargs invoke(Varargs args) {
                    return LuaValue.valueOf(HIGHLIGHTS.containsKey(highlightId));
                }
            });
            
            return highlightRef;
        } catch (Exception e) {
            e.printStackTrace();
            return LuaValue.valueOf("Error: " + e.getMessage());
        }
    }
    
    private static class HighlightData {
        final BlockPos pos;
        Color color;
        RenderUtil.RenderType renderType;
        boolean active;
        
        HighlightData(BlockPos pos, Color color, RenderUtil.RenderType renderType, boolean active) {
            this.pos = pos;
            this.color = color;
            this.renderType = renderType;
            this.active = active;
        }
    }
    
    public static class RenderHandler {
        
        @SubscribeEvent
        public void onRenderWorldLast(RenderWorldLastEvent event) {
            if (HIGHLIGHTS.isEmpty() || Minecraft.getMinecraft().thePlayer == null) {
                return;
            }
            
            try {
                // Clean up expired highlights
                Iterator<Map.Entry<String, HighlightData>> it = HIGHLIGHTS.entrySet().iterator();
                while (it.hasNext()) {
                    Map.Entry<String, HighlightData> entry = it.next();
                    HighlightData data = entry.getValue();
                    
                    // If block is too far away, remove it from highlights
                    if (Minecraft.getMinecraft().thePlayer.getDistanceSq(data.pos) > 4096) { // 64 blocks squared
                        it.remove();
                    }
                }
                
                // Render all highlights
                for (Map.Entry<String, HighlightData> entry : HIGHLIGHTS.entrySet()) {
                    HighlightData data = entry.getValue();
                    if (data.active) {
                        RenderUtil.highlightBlock(data.pos, data.color, data.renderType);
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
} 