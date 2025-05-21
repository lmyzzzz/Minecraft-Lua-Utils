-- Get Block Under Player Example
-- This script shows how to get information about the block
-- under the player's feet and log all available details

-- Function to convert a table to a formatted string for logging
function table_to_string(tbl, indent)
    indent = indent or 0
    local result = ""
    local padding = string.rep("  ", indent)
    
    for k, v in pairs(tbl) do
        if type(v) == "table" then
            result = result .. padding .. k .. ":\n" .. table_to_string(v, indent + 1)
        else
            result = result .. padding .. k .. ": " .. tostring(v) .. "\n"
        end
    end
    
    return result
end

-- Main script execution
function main()
    -- Get player position
    local playerPos = GetPlayerPos()
    
    if not playerPos then
        SendLog("Error: Could not get player position")
        return
    end
    
    SendLog("Player position: X=" .. playerPos.x .. ", Y=" .. playerPos.y .. ", Z=" .. playerPos.z)
    
    -- Get the block under player's feet (Y-1)
    local blockX = playerPos.x
    local blockY = playerPos.y - 1 -- Block under player
    local blockZ = playerPos.z
    
    SendLog("Checking block at X=" .. blockX .. ", Y=" .. blockY .. ", Z=" .. blockZ)
    
    -- Get block information
    local block = GetBlock(blockX, blockY, blockZ)
    
    if not block or type(block) == "string" then
        SendLog("Error getting block: " .. tostring(block))
        return
    end
    
    -- Log block information
    SendLog("=== Block Information ===")
    SendLog("Block coordinates: X=" .. block.x .. ", Y=" .. block.y .. ", Z=" .. block.z)
    SendLog("Block type: " .. block.blockType)
    SendLog("Block state: " .. block.blockState)
    
    -- Log all block properties
    SendLog("\nDetailed block information:")
    SendLog(table_to_string(block))
    
    -- Highlight the block for visual reference
    local highlight = HighlightBlock(block.x, block.y, block.z, "#0000BB", 0.8, "LINE")
    
    -- Remove highlight after 10 seconds
    Sleep(10000)
    highlight.remove()
end

-- Execute the main function
main() 