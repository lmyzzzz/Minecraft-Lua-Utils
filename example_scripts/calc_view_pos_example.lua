-- CalcViewPos Example
-- This script demonstrates how to calculate the view angles
-- needed to look at a specific coordinate in the world

-- Main script execution
function main()
    -- Get player position
    local playerPos = GetPlayerPos()
    
    if not playerPos then
        SendLog("Error: Could not get player position")
        return
    end
    
    SendLog("Player position: X=" .. playerPos.x .. ", Y=" .. playerPos.y .. ", Z=" .. playerPos.z)
    SendLog("Current view angles: Pitch=" .. playerPos.pitch .. ", Yaw=" .. playerPos.yaw)
    
    -- Define a target position 10 blocks ahead and 5 blocks above
    local targetX = playerPos.x + 10
    local targetY = playerPos.y + 5
    local targetZ = playerPos.z + 10
    
    -- Highlight the target position
    local highlight = HighlightBlock(
        math.floor(targetX), 
        math.floor(targetY), 
        math.floor(targetZ), 
        "#FF0000", 0.8, "FILL"
    )
    
    -- Calculate the view angles to look at this position
    local viewAngles = CalcViewPos(targetX, targetY, targetZ)
    
    if not viewAngles or type(viewAngles) == "string" then
        SendLog("Error calculating view angles: " .. tostring(viewAngles))
        highlight.remove()
        return
    end
    
    -- Report the calculated angles
    SendLog("\n=== Target Information ===")
    SendLog("Target position: X=" .. targetX .. ", Y=" .. targetY .. ", Z=" .. targetZ)
    SendLog("Required view angles to look at target:")
    SendLog("  Pitch: " .. viewAngles.pitch .. " degrees")
    SendLog("  Yaw: " .. viewAngles.yaw .. " degrees")
    
    -- Draw a line from player to target
    local eyeHeight = 1.62 -- Typical eye height in Minecraft
    local line = RenderLine(
        playerPos.x, playerPos.y + eyeHeight, playerPos.z,
        targetX, targetY, targetZ,
        "#FFFF00", 2.0
    )
    
    -- Display a message on how to use these angles
    SendLog("\nTo look at this position, you would need to rotate your camera to:")
    SendLog("  Pitch (up/down): " .. string.format("%.2f", viewAngles.pitch))
    SendLog("  Yaw (left/right): " .. string.format("%.2f", viewAngles.yaw))
    
    -- Calculate the angle difference from current view
    local pitchDiff = viewAngles.pitch - playerPos.pitch
    local yawDiff = viewAngles.yaw - playerPos.yaw
    
    -- Normalize yaw difference to be between -180 and 180
    while yawDiff > 180 do yawDiff = yawDiff - 360 end
    while yawDiff < -180 do yawDiff = yawDiff + 360 end
    
    SendLog("\nAngle changes needed:")
    SendLog("  Pitch change: " .. string.format("%.2f", pitchDiff) .. " degrees")
    SendLog("  Yaw change: " .. string.format("%.2f", yawDiff) .. " degrees")
    
    -- Clean up after 15 seconds
    SendLog("\nRender will be cleared in 15 seconds...")
    Sleep(15000)
    highlight.remove()
    line.remove()
    SendLog("Render cleared")
end

-- Execute the main function
main() 