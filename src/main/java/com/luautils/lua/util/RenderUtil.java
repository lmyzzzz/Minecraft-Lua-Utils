package com.luautils.lua.util;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.RenderGlobal;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.WorldRenderer;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.BlockPos;
import org.lwjgl.opengl.GL11;

import java.awt.Color;

public class RenderUtil {

    /**
     * Renders a block highlight at specified coordinates
     * 
     * @param pos Block position
     * @param color Color in RGBA format
     * @param renderType Type of rendering (LINE or FILL)
     */
    public static void highlightBlock(BlockPos pos, Color color, RenderType renderType) {
        Minecraft mc = Minecraft.getMinecraft();
        
        if (mc.theWorld == null || pos == null) return;
        
        // Calculate render position
        double renderX = pos.getX() - mc.getRenderManager().viewerPosX;
        double renderY = pos.getY() - mc.getRenderManager().viewerPosY;
        double renderZ = pos.getZ() - mc.getRenderManager().viewerPosZ;

        // Create bounding box
        AxisAlignedBB boundingBox = new AxisAlignedBB(
            renderX, renderY, renderZ,
            renderX + 1, renderY + 1, renderZ + 1
        );
        
        // Set up OpenGL
        GlStateManager.pushMatrix();
        GlStateManager.disableTexture2D();
        GlStateManager.enableBlend();
        GlStateManager.disableLighting();
        GlStateManager.disableDepth();
        GlStateManager.depthMask(false);
        GlStateManager.blendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0);
        
        GlStateManager.color(
            color.getRed() / 255f,
            color.getGreen() / 255f,
            color.getBlue() / 255f,
            color.getAlpha() / 255f
        );
        
        // Render based on type
        if (renderType == RenderType.LINE) {
            // Draw outline
            GL11.glLineWidth(2.0F);
            GL11.glPolygonMode(GL11.GL_FRONT_AND_BACK, GL11.GL_LINE);
            drawOutlinedBoundingBox(boundingBox);
        } else {
            // Draw filled
            GL11.glPolygonMode(GL11.GL_FRONT_AND_BACK, GL11.GL_FILL);
            drawFilledBoundingBox(boundingBox);
        }
        
        // Restore GL state
        GL11.glPolygonMode(GL11.GL_FRONT_AND_BACK, GL11.GL_FILL);
        GlStateManager.enableDepth();
        GlStateManager.depthMask(true);
        GlStateManager.enableTexture2D();
        GlStateManager.disableBlend();
        GlStateManager.enableLighting();
        GlStateManager.popMatrix();
    }
    
    private static void drawOutlinedBoundingBox(AxisAlignedBB boundingBox) {
        Tessellator tessellator = Tessellator.getInstance();
        WorldRenderer renderer = tessellator.getWorldRenderer();
        
        renderer.begin(GL11.GL_LINES, DefaultVertexFormats.POSITION);
        
        // Bottom face
        renderer.pos(boundingBox.minX, boundingBox.minY, boundingBox.minZ).endVertex();
        renderer.pos(boundingBox.maxX, boundingBox.minY, boundingBox.minZ).endVertex();
        
        renderer.pos(boundingBox.maxX, boundingBox.minY, boundingBox.minZ).endVertex();
        renderer.pos(boundingBox.maxX, boundingBox.minY, boundingBox.maxZ).endVertex();
        
        renderer.pos(boundingBox.maxX, boundingBox.minY, boundingBox.maxZ).endVertex();
        renderer.pos(boundingBox.minX, boundingBox.minY, boundingBox.maxZ).endVertex();
        
        renderer.pos(boundingBox.minX, boundingBox.minY, boundingBox.maxZ).endVertex();
        renderer.pos(boundingBox.minX, boundingBox.minY, boundingBox.minZ).endVertex();
        
        // Top face
        renderer.pos(boundingBox.minX, boundingBox.maxY, boundingBox.minZ).endVertex();
        renderer.pos(boundingBox.maxX, boundingBox.maxY, boundingBox.minZ).endVertex();
        
        renderer.pos(boundingBox.maxX, boundingBox.maxY, boundingBox.minZ).endVertex();
        renderer.pos(boundingBox.maxX, boundingBox.maxY, boundingBox.maxZ).endVertex();
        
        renderer.pos(boundingBox.maxX, boundingBox.maxY, boundingBox.maxZ).endVertex();
        renderer.pos(boundingBox.minX, boundingBox.maxY, boundingBox.maxZ).endVertex();
        
        renderer.pos(boundingBox.minX, boundingBox.maxY, boundingBox.maxZ).endVertex();
        renderer.pos(boundingBox.minX, boundingBox.maxY, boundingBox.minZ).endVertex();
        
        // Connecting lines
        renderer.pos(boundingBox.minX, boundingBox.minY, boundingBox.minZ).endVertex();
        renderer.pos(boundingBox.minX, boundingBox.maxY, boundingBox.minZ).endVertex();
        
        renderer.pos(boundingBox.maxX, boundingBox.minY, boundingBox.minZ).endVertex();
        renderer.pos(boundingBox.maxX, boundingBox.maxY, boundingBox.minZ).endVertex();
        
        renderer.pos(boundingBox.maxX, boundingBox.minY, boundingBox.maxZ).endVertex();
        renderer.pos(boundingBox.maxX, boundingBox.maxY, boundingBox.maxZ).endVertex();
        
        renderer.pos(boundingBox.minX, boundingBox.minY, boundingBox.maxZ).endVertex();
        renderer.pos(boundingBox.minX, boundingBox.maxY, boundingBox.maxZ).endVertex();
        
        tessellator.draw();
    }
    
    private static void drawFilledBoundingBox(AxisAlignedBB boundingBox) {
        Tessellator tessellator = Tessellator.getInstance();
        WorldRenderer renderer = tessellator.getWorldRenderer();
        
        renderer.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION);
        
        // Bottom face
        renderer.pos(boundingBox.minX, boundingBox.minY, boundingBox.minZ).endVertex();
        renderer.pos(boundingBox.maxX, boundingBox.minY, boundingBox.minZ).endVertex();
        renderer.pos(boundingBox.maxX, boundingBox.minY, boundingBox.maxZ).endVertex();
        renderer.pos(boundingBox.minX, boundingBox.minY, boundingBox.maxZ).endVertex();
        
        // Top face
        renderer.pos(boundingBox.minX, boundingBox.maxY, boundingBox.minZ).endVertex();
        renderer.pos(boundingBox.minX, boundingBox.maxY, boundingBox.maxZ).endVertex();
        renderer.pos(boundingBox.maxX, boundingBox.maxY, boundingBox.maxZ).endVertex();
        renderer.pos(boundingBox.maxX, boundingBox.maxY, boundingBox.minZ).endVertex();
        
        // Front face (Z-)
        renderer.pos(boundingBox.minX, boundingBox.minY, boundingBox.minZ).endVertex();
        renderer.pos(boundingBox.minX, boundingBox.maxY, boundingBox.minZ).endVertex();
        renderer.pos(boundingBox.maxX, boundingBox.maxY, boundingBox.minZ).endVertex();
        renderer.pos(boundingBox.maxX, boundingBox.minY, boundingBox.minZ).endVertex();
        
        // Back face (Z+)
        renderer.pos(boundingBox.minX, boundingBox.minY, boundingBox.maxZ).endVertex();
        renderer.pos(boundingBox.maxX, boundingBox.minY, boundingBox.maxZ).endVertex();
        renderer.pos(boundingBox.maxX, boundingBox.maxY, boundingBox.maxZ).endVertex();
        renderer.pos(boundingBox.minX, boundingBox.maxY, boundingBox.maxZ).endVertex();
        
        // Left face (X-)
        renderer.pos(boundingBox.minX, boundingBox.minY, boundingBox.minZ).endVertex();
        renderer.pos(boundingBox.minX, boundingBox.minY, boundingBox.maxZ).endVertex();
        renderer.pos(boundingBox.minX, boundingBox.maxY, boundingBox.maxZ).endVertex();
        renderer.pos(boundingBox.minX, boundingBox.maxY, boundingBox.minZ).endVertex();
        
        // Right face (X+)
        renderer.pos(boundingBox.maxX, boundingBox.minY, boundingBox.minZ).endVertex();
        renderer.pos(boundingBox.maxX, boundingBox.maxY, boundingBox.minZ).endVertex();
        renderer.pos(boundingBox.maxX, boundingBox.maxY, boundingBox.maxZ).endVertex();
        renderer.pos(boundingBox.maxX, boundingBox.minY, boundingBox.maxZ).endVertex();
        
        tessellator.draw();
    }
    
    /**
     * Convert hex color string to Color object
     */
    public static Color parseColor(String hexColor, float opacity) {
        if (hexColor.startsWith("#")) {
            hexColor = hexColor.substring(1);
        }
        
        try {
            int rgb = Integer.parseInt(hexColor, 16);
            int r = (rgb >> 16) & 0xFF;
            int g = (rgb >> 8) & 0xFF;
            int b = rgb & 0xFF;
            int a = Math.round(opacity * 255);
            
            return new Color(r, g, b, a);
        } catch (NumberFormatException e) {
            // Default to red if parsing fails
            return new Color(255, 0, 0, Math.round(opacity * 255));
        }
    }
    
    /**
     * Draws a 3D line between two points in the world
     * 
     * @param startX X coordinate of the start point
     * @param startY Y coordinate of the start point
     * @param startZ Z coordinate of the start point
     * @param endX X coordinate of the end point
     * @param endY Y coordinate of the end point
     * @param endZ Z coordinate of the end point
     * @param color Color of the line
     * @param width Width of the line in pixels
     */
    public static void drawLine(double startX, double startY, double startZ, 
                                double endX, double endY, double endZ, 
                                Color color, float width) {
        Minecraft mc = Minecraft.getMinecraft();
        
        if (mc.theWorld == null) return;
        
        // Calculate render positions
        double renderStartX = startX - mc.getRenderManager().viewerPosX;
        double renderStartY = startY - mc.getRenderManager().viewerPosY;
        double renderStartZ = startZ - mc.getRenderManager().viewerPosZ;
        
        double renderEndX = endX - mc.getRenderManager().viewerPosX;
        double renderEndY = endY - mc.getRenderManager().viewerPosY;
        double renderEndZ = endZ - mc.getRenderManager().viewerPosZ;
        
        // Set up OpenGL
        GlStateManager.pushMatrix();
        GlStateManager.disableTexture2D();
        GlStateManager.enableBlend();
        GlStateManager.disableLighting();
        GlStateManager.disableDepth();
        GlStateManager.depthMask(false);
        GlStateManager.blendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0);
        
        // Set color
        GlStateManager.color(
            color.getRed() / 255f,
            color.getGreen() / 255f,
            color.getBlue() / 255f,
            color.getAlpha() / 255f
        );
        
        // Set line width
        GL11.glLineWidth(width);
        
        // Draw the line
        Tessellator tessellator = Tessellator.getInstance();
        WorldRenderer renderer = tessellator.getWorldRenderer();
        
        renderer.begin(GL11.GL_LINES, DefaultVertexFormats.POSITION);
        renderer.pos(renderStartX, renderStartY, renderStartZ).endVertex();
        renderer.pos(renderEndX, renderEndY, renderEndZ).endVertex();
        tessellator.draw();
        
        // Restore GL state
        GL11.glLineWidth(1.0F);
        GlStateManager.enableDepth();
        GlStateManager.depthMask(true);
        GlStateManager.enableTexture2D();
        GlStateManager.disableBlend();
        GlStateManager.enableLighting();
        GlStateManager.popMatrix();
    }
    
    public enum RenderType {
        LINE,
        FILL
    }
} 