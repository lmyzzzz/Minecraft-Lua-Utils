-- tb Function Example
-- This script demonstrates how to use the tb function
-- to convert coordinates to block center coordinates

-- Main script execution
function main()
    -- Get player position
    local playerPos = GetPlayerPos()
    
    if not playerPos then
        SendLog("Error: Could not get player position")
        return
    end
    
    -- Log player position
    SendLog("Player position:")
    SendLog("  X: " .. playerPos.x .. " (raw)")
    SendLog("  Y: " .. playerPos.y .. " (raw)")
    SendLog("  Z: " .. playerPos.z .. " (raw)")
    
    -- Get the block center coordinates for all axes
    local centerX = tb(playerPos.x)
    local centerY = tb(playerPos.y)  -- Now also calculating Y center
    local centerZ = tb(playerPos.z)
    
    -- Calculate which block the player is in
    local blockX = math.floor(playerPos.x)
    local blockY = math.floor(playerPos.y)
    local blockZ = math.floor(playerPos.z)
    
    -- Log the converted coordinates
    SendLog("\nBlock position:")
    SendLog("  Block X: " .. blockX)
    SendLog("  Block Y: " .. blockY)
    SendLog("  Block Z: " .. blockZ)
    
    SendLog("\nBlock center coordinates:")
    SendLog("  Center X: " .. centerX)
    SendLog("  Center Y: " .. centerY)  -- Now showing Y center
    SendLog("  Center Z: " .. centerZ)
    
    -- Calculate distance from player to block center
    local distanceX = math.abs(playerPos.x - centerX)
    local distanceY = math.abs(playerPos.y - centerY)  -- Now calculating Y distance
    local distanceZ = math.abs(playerPos.z - centerZ)
    
    SendLog("\nDistance to block center:")
    SendLog("  X distance: " .. string.format("%.2f", distanceX))
    SendLog("  Y distance: " .. string.format("%.2f", distanceY))  -- Now showing Y distance
    SendLog("  Z distance: " .. string.format("%.2f", distanceZ))
    
    -- Highlight the block the player is in
    local highlight = HighlightBlock(blockX, blockY, blockZ, "#00FF00", 0.3, "LINE")
    
    -- Examples with various coordinates
    SendLog("\nExample conversions:")
    local examples = {
        {value = 5.7, label = "X: 5.7"},
        {value = -3.2, label = "X: -3.2"},
        {value = 0.0, label = "X/Y/Z: 0.0"},
        {value = 0.9, label = "X: 0.9"},
        {value = 67.9, label = "Y: 67.9"},
        {value = 256.3, label = "Y: 256.3"},
        {value = -0.1, label = "Y: -0.1"},
        {value = -1.0, label = "Z: -1.0"}
    }
    
    for _, example in ipairs(examples) do
        local center = tb(example.value)
        SendLog("  " .. example.label .. " → " .. center .. " (block: " .. math.floor(example.value) .. ")")
    end
    
    -- Draw a line from player position to block center for visualization
    local line = RenderLine(
        playerPos.x, playerPos.y, playerPos.z,
        centerX, centerY, centerZ,  -- Now using Y center too
        "#FF00FF", 2.0
    )
    
    -- Also show a line to X-Z center (keeping Y the same)
    local lineXZ = RenderLine(
        playerPos.x, playerPos.y, playerPos.z,
        centerX, playerPos.y, centerZ,
        "#00FFFF", 1.0
    )
    
    -- Clean up after 15 seconds
    SendLog("\nRender will be cleared in 15 seconds...")
    Sleep(15000)
    highlight.remove()
    line.remove()
    lineXZ.remove()
    SendLog("Render cleared")
end

-- Execute the main function
main() 