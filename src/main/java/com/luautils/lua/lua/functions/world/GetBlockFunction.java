package com.luautils.lua.lua.functions.world;

import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.util.BlockPos;
import org.luaj.vm2.LuaTable;
import org.luaj.vm2.LuaValue;
import org.luaj.vm2.Varargs;
import org.luaj.vm2.lib.VarArgFunction;

public class GetBlockFunction extends VarArgFunction {
    
    @Override
    public Varargs invoke(Varargs args) {
        try {
            // Check for correct argument count
            if (args.narg() < 3) {
                return LuaValue.valueOf("Error: GetBlock requires 3 arguments (x, y, z)");
            }
            
            // Parse position as doubles (floating point coordinates)
            final double xDouble = args.checkdouble(1);
            final double yDouble = args.checkdouble(2);
            final double zDouble = args.checkdouble(3);
            
            // Convert to integer coordinates to get the block position
            final int x = (int) Math.floor(xDouble);
            final int y = (int) Math.floor(yDouble);
            final int z = (int) Math.floor(zDouble);
            
            // Create block position
            final BlockPos pos = new BlockPos(x, y, z);
            
            // Get Minecraft instance
            Minecraft mc = Minecraft.getMinecraft();
            
            // Check if world is available
            if (mc.theWorld == null) {
                return LuaValue.valueOf("Error: World not available");
            }
            
            // Get block state at this position
            IBlockState blockState = mc.theWorld.getBlockState(pos);
            if (blockState == null) {
                return LuaValue.valueOf("Error: Unable to get block state at position");
            }
            
            // Get the block from block state
            Block block = blockState.getBlock();
            if (block == null) {
                return LuaValue.valueOf("Error: No block at this position");
            }
            
            // Create result table
            LuaTable blockInfo = new LuaTable();
            
            // Add block coordinates
            blockInfo.set("x", LuaValue.valueOf(x));
            blockInfo.set("y", LuaValue.valueOf(y));
            blockInfo.set("z", LuaValue.valueOf(z));
            
            // Add exact coordinates that were queried
            blockInfo.set("exactX", LuaValue.valueOf(xDouble));
            blockInfo.set("exactY", LuaValue.valueOf(yDouble));
            blockInfo.set("exactZ", LuaValue.valueOf(zDouble));
            
            // Add block type
            blockInfo.set("blockType", LuaValue.valueOf(Block.blockRegistry.getNameForObject(block).toString()));
            
            // Add block ID
            blockInfo.set("blockId", LuaValue.valueOf(Block.getIdFromBlock(block)));
            
            // Add block state metadata
            blockInfo.set("blockState", LuaValue.valueOf(block.getMetaFromState(blockState)));
            
            // Add block properties
            LuaTable properties = new LuaTable();
            blockState.getProperties().forEach((key, value) -> {
                properties.set(key.getName(), LuaValue.valueOf(value.toString()));
            });
            blockInfo.set("properties", properties);
            
            // Add light level
            blockInfo.set("lightLevel", LuaValue.valueOf(mc.theWorld.getLight(pos)));
            
            // Add block hardness
            blockInfo.set("hardness", LuaValue.valueOf(block.getBlockHardness(mc.theWorld, pos)));
            
            // Add block resistance
            blockInfo.set("resistance", LuaValue.valueOf(block.getExplosionResistance(null)));
            
            // Add if the block is solid
            blockInfo.set("isSolid", LuaValue.valueOf(block.isBlockSolid(mc.theWorld, pos, null)));
            
            // Add block's local bounds
            blockInfo.set("minX", LuaValue.valueOf(block.getBlockBoundsMinX()));
            blockInfo.set("minY", LuaValue.valueOf(block.getBlockBoundsMinY()));
            blockInfo.set("minZ", LuaValue.valueOf(block.getBlockBoundsMinZ()));
            blockInfo.set("maxX", LuaValue.valueOf(block.getBlockBoundsMaxX()));
            blockInfo.set("maxY", LuaValue.valueOf(block.getBlockBoundsMaxY()));
            blockInfo.set("maxZ", LuaValue.valueOf(block.getBlockBoundsMaxZ()));
            
            // Add if the block is opaque
            blockInfo.set("isOpaque", LuaValue.valueOf(block.isOpaqueCube()));
            
            // Add if the block is replaceable
            blockInfo.set("isReplaceable", LuaValue.valueOf(block.isReplaceable(mc.theWorld, pos)));
            
            // Add block's material properties
            LuaTable material = new LuaTable();
            material.set("isLiquid", LuaValue.valueOf(block.getMaterial().isLiquid()));
            material.set("isSolid", LuaValue.valueOf(block.getMaterial().isSolid()));
            material.set("isReplaceable", LuaValue.valueOf(block.getMaterial().isReplaceable()));
            material.set("isToolNotRequired", LuaValue.valueOf(block.getMaterial().isToolNotRequired()));
            material.set("isFlammable", LuaValue.valueOf(block.getMaterial().getCanBurn()));
            blockInfo.set("material", material);
            
            // Return the block info table
            return blockInfo;
        } catch (Exception e) {
            e.printStackTrace();
            return LuaValue.valueOf("Error: " + e.getMessage());
        }
    }
} 