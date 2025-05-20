package com.luautils.lua.lua.functions.player;

import com.luautils.lua.LuaUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import org.luaj.vm2.LuaValue;
import org.luaj.vm2.Varargs;
import org.luaj.vm2.lib.VarArgFunction;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

public class SimpleRotateFunction extends VarArgFunction {
    
    private float targetPitch;
    private float targetYaw;
    private float pitchSpeed;    // degrees per tick
    private float yawSpeed;      // degrees per tick
    private float pitchAccel;    // degrees per tick²
    private float yawAccel;      // degrees per tick²
    private boolean isRotating = false;
    private CountDownLatch rotationLatch;
    private RotationHandler rotationHandler;
    
    // Constants
    private static final float PITCH_LIMIT = 90.0f;
    private static final float DISTANCE_THRESHOLD = 0.05f;
    private static final int MAX_ROTATION_TICKS = 200;  // Safety limit to prevent infinite rotation
    private static final boolean DEBUG_MODE = true;     // Set to false to reduce log output
    
    // Smoothing constants
    private static final float MIN_INITIAL_SPEED_FACTOR = 0.15f;  // Initial speed reduction to prevent jerky start
    private static final int EASING_IN_TICKS = 3;  // Number of ticks for initial smooth transition
    private static final float COMPLETION_THRESHOLD = 0.3f;  // Threshold for considering rotation complete (degrees)
    private static final float JITTER_THRESHOLD = 0.1f;  // Threshold for detecting jitter (degrees)
    private static final float APPROACH_SLOWDOWN = 0.7f; // Slow down factor when approaching target
    
    @Override
    public Varargs invoke(Varargs args) {
        if (args.narg() < 6) {
            return LuaValue.valueOf("Error: SimpleRotate requires 6 arguments");
        }
        
        try {
            targetPitch = (float) args.checkdouble(1);
            targetYaw = (float) args.checkdouble(2);
            pitchSpeed = Math.abs((float) args.checkdouble(3));     // Always positive
            yawSpeed = Math.abs((float) args.checkdouble(4));       // Always positive
            pitchAccel = Math.abs((float) args.checkdouble(5));     // Always positive
            yawAccel = Math.abs((float) args.checkdouble(6));       // Always positive
            
            // Ensure minimum values to prevent division by zero later
            if (pitchSpeed < 0.1f) pitchSpeed = 0.1f;
            if (yawSpeed < 0.1f) yawSpeed = 0.1f;
            if (pitchAccel < 0.05f) pitchAccel = 0.05f;
            if (yawAccel < 0.05f) yawAccel = 0.05f;
            
            // Limit target pitch
            if (targetPitch > PITCH_LIMIT) targetPitch = PITCH_LIMIT;
            if (targetPitch < -PITCH_LIMIT) targetPitch = -PITCH_LIMIT;
            
            // Normalize target yaw
            while (targetYaw > 180.0F) targetYaw -= 360.0F;
            while (targetYaw <= -180.0F) targetYaw += 360.0F;
            
            // Setup rotation handler
            if (rotationHandler == null) {
                rotationHandler = new RotationHandler();
                LuaUtils.getInstance().getLuaManager().runOnMainThread(() -> {
                    net.minecraftforge.common.MinecraftForge.EVENT_BUS.register(rotationHandler);
                    log("Rotation handler registered");
                });
            }
            
            rotationLatch = new CountDownLatch(1);
            isRotating = true;
            
            log("Starting rotation to: pitch=" + targetPitch + ", yaw=" + targetYaw);
            log("Speed parameters: pitch=" + pitchSpeed + "°/tick, yaw=" + yawSpeed + "°/tick");
            
            // Wait for rotation to complete (with timeout)
            boolean completed = rotationLatch.await(15, TimeUnit.SECONDS);
            
            if (!completed) {
                log("Rotation timed out");
            }
            
            return LuaValue.TRUE;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return LuaValue.valueOf("Rotation interrupted");
        } catch (Exception e) {
            e.printStackTrace();
            return LuaValue.valueOf("Error: " + e.getMessage());
        } finally {
            isRotating = false;
        }
    }
    
    /**
     * Log a message with proper prefix
     */
    private void log(String message) {
        System.out.println("[LuaUtils] " + message);
    }
    
    private class RotationHandler {
        // Animation data
        private float startPitch;
        private float startYaw;
        private float normalizedTargetYaw;
        
        // Coordinated movement profile
        private RotationProfile rotationProfile;
        
        // State tracking
        private boolean isInitialized = false;
        private int tickCounter = 0;
        private boolean isComplete = false;
        private boolean approachingTarget = false;
        private int stableTickCounter = 0;
        
        // Last position (for smoothing)
        private float lastPitch;
        private float lastYaw;
        private float prevFramePitch;
        private float prevFrameYaw;
        
        // Jitter detection
        private boolean jitterDetected = false;
        private int jitterCount = 0;
        
        @SubscribeEvent
        public void onClientTick(TickEvent.ClientTickEvent event) {
            if (event.phase != TickEvent.Phase.START || !isRotating) return;
            
            Minecraft mc = Minecraft.getMinecraft();
            if (mc == null || mc.thePlayer == null) return;
            
            EntityPlayer player = mc.thePlayer;
            
            // Initialize on first tick
            if (!isInitialized) {
                // Get start positions
                startPitch = player.rotationPitch;
                startYaw = player.rotationYaw;
                
                // Save as last position too
                lastPitch = startPitch;
                lastYaw = startYaw;
                prevFramePitch = startPitch;
                prevFrameYaw = startYaw;
                
                // Normalize yaw values
                float normalizedStartYaw = startYaw;
                while (normalizedStartYaw < -180) normalizedStartYaw += 360;
                while (normalizedStartYaw >= 180) normalizedStartYaw -= 360;
                
                normalizedTargetYaw = targetYaw;
                while (normalizedTargetYaw < -180) normalizedTargetYaw += 360;
                while (normalizedTargetYaw >= 180) normalizedTargetYaw -= 360;
                
                // Calculate shortest yaw path
                float yawDiff = normalizedTargetYaw - normalizedStartYaw;
                while (yawDiff > 180) yawDiff -= 360;
                while (yawDiff < -180) yawDiff += 360;
                
                // Calculate pitch difference
                float pitchDiff = targetPitch - startPitch;
                
                // Create rotation profile
                rotationProfile = new RotationProfile(
                    startPitch, targetPitch, 
                    normalizedStartYaw, normalizedTargetYaw,
                    pitchSpeed, yawSpeed,
                    pitchAccel, yawAccel
                );
                
                log("Rotation initialized from pitch=" + startPitch + "°, yaw=" + normalizedStartYaw + "°");
                log("To pitch=" + targetPitch + "°, yaw=" + normalizedTargetYaw + "°");
                log("Differences: pitch=" + pitchDiff + "°, yaw=" + yawDiff + "°");
                log("Estimated duration: " + rotationProfile.getDuration() + " ticks");
                
                isInitialized = true;
                tickCounter = 0;
                return;
            }
            
            // Safety check
            tickCounter++;
            if (tickCounter > MAX_ROTATION_TICKS) {
                log("Emergency stop: exceeded maximum rotation time (" + MAX_ROTATION_TICKS + " ticks)");
                completeRotation();
                return;
            }
            
            // Get target rotation for this tick
            float[] rotation = rotationProfile.getRotationAtTime(tickCounter);
            float nextPitch = rotation[0];
            float nextYaw = rotation[1];
            
            // Check if we're very close to target and should lock onto it
            float pitchDiff = Math.abs(nextPitch - targetPitch);
            float yawDiff = Math.abs(normalizeAngle(nextYaw - normalizedTargetYaw));
            
            // Detect jitter by checking if we're switching directions rapidly
            boolean currentFrameJitter = false;
            if (tickCounter > 1) {
                float pitchChange = nextPitch - lastPitch;
                float yawChange = normalizeAngle(nextYaw - lastYaw);
                float prevPitchChange = lastPitch - prevFramePitch;
                float prevYawChange = normalizeAngle(lastYaw - prevFrameYaw);
                
                // Check if direction changed with significant movement
                boolean pitchJitter = Math.abs(pitchChange) > JITTER_THRESHOLD && 
                                      Math.abs(prevPitchChange) > JITTER_THRESHOLD && 
                                      Math.signum(pitchChange) != Math.signum(prevPitchChange);
                                      
                boolean yawJitter = Math.abs(yawChange) > JITTER_THRESHOLD && 
                                    Math.abs(prevYawChange) > JITTER_THRESHOLD && 
                                    Math.signum(yawChange) != Math.signum(prevYawChange);
                
                currentFrameJitter = pitchJitter || yawJitter;
                
                if (currentFrameJitter) {
                    jitterCount++;
                    if (jitterCount >= 2 && !jitterDetected) {
                        jitterDetected = true;
                        log("Jitter detected, applying extra smoothing");
                    }
                }
            }
            
            // Start approaching mode when getting close to target
            if (!approachingTarget && (pitchDiff < 5.0f && yawDiff < 5.0f)) {
                approachingTarget = true;
                log("Approaching target, enabling fine control");
            }
            
            // Use our enhanced smoothing when approaching or when jitter is detected
            boolean useEnhancedSmoothing = approachingTarget || jitterDetected || tickCounter <= EASING_IN_TICKS;
            
            // Store previous frame for jitter detection
            prevFramePitch = lastPitch;
            prevFrameYaw = lastYaw;
            
            // Apply smooth rotation logic
            if (useEnhancedSmoothing) {
                // Calculate blend factor based on situation
                float blendFactor;
                
                if (tickCounter <= EASING_IN_TICKS) {
                    // Initial easing during start of rotation
                    float easeFactor = (float)Math.pow(tickCounter / (float)EASING_IN_TICKS, 2);
                    blendFactor = MIN_INITIAL_SPEED_FACTOR + (1.0f - MIN_INITIAL_SPEED_FACTOR) * easeFactor;
                } else if (approachingTarget) {
                    // Approaching target, use slow approach to prevent overshooting
                    float distanceFactor = Math.min(1.0f, Math.max(pitchDiff, yawDiff) / 5.0f);
                    blendFactor = APPROACH_SLOWDOWN + (1.0f - APPROACH_SLOWDOWN) * distanceFactor;
                } else if (jitterDetected) {
                    // Apply heavy smoothing when jitter is detected
                    blendFactor = 0.4f;
                } else {
                    // Default case should never happen, but just in case
                    blendFactor = 0.7f;
                }
                
                // Apply smoothed position for pitch
                float smoothPitch = lastPitch + (nextPitch - lastPitch) * blendFactor;
                
                // Apply smoothed position for yaw with special wrapping handling
                float yawDelta = normalizeAngle(nextYaw - lastYaw);
                float smoothYaw = lastYaw + yawDelta * blendFactor;
                while (smoothYaw < -180) smoothYaw += 360;
                while (smoothYaw >= 180) smoothYaw -= 360;
                
                // Update player rotation
                player.rotationPitch = smoothPitch;
                player.rotationYaw = smoothYaw;
                
                // Store for next frame
                lastPitch = smoothPitch;
                lastYaw = smoothYaw;
            } else {
                // Standard rotation when no special handling is needed
                player.rotationPitch = nextPitch;
                player.rotationYaw = nextYaw;
                
                // Store for next frame
                lastPitch = nextPitch;
                lastYaw = nextYaw;
            }
            
            // Check if we're extremely close to target (completion check)
            if (pitchDiff <= COMPLETION_THRESHOLD && yawDiff <= COMPLETION_THRESHOLD) {
                stableTickCounter++;
                
                // Require multiple stable ticks to confirm completion
                if (stableTickCounter >= 3) {
                    // Snap to exact target
                    player.rotationPitch = targetPitch;
                    player.rotationYaw = normalizedTargetYaw;
                    log("Rotation completed in " + tickCounter + " ticks");
                    completeRotation();
                }
            } else {
                stableTickCounter = 0;
            }
            
            // Log progress
            if (DEBUG_MODE && (tickCounter % 20 == 0 || tickCounter < 5)) {
                log("Tick " + tickCounter + 
                    ": Pitch=" + String.format("%.2f", player.rotationPitch) + 
                    "°, Yaw=" + String.format("%.2f", player.rotationYaw) + "°");
            }
        }
        
        /**
         * Complete the rotation and clean up
         */
        private void completeRotation() {
            if (!isComplete) {
                isComplete = true;
                isRotating = false;
                isInitialized = false;
                
                // Signal completion
                if (rotationLatch != null) {
                    rotationLatch.countDown();
                }
            }
        }
        
        /**
         * Smooth out rotation rendering between ticks
         */
        @SubscribeEvent
        public void onRenderTick(TickEvent.RenderTickEvent event) {
            if (event.phase != TickEvent.Phase.START || !isRotating || !isInitialized || isComplete) return;
            
            Minecraft mc = Minecraft.getMinecraft();
            if (mc == null || mc.thePlayer == null) return;
            
            // Only perform interpolation if we have valid profile
            if (rotationProfile != null && event.renderTickTime > 0) {
                float partialTick = event.renderTickTime;
                float currentTick = tickCounter + partialTick;
                
                // Get interpolated rotation
                float[] rotation = rotationProfile.getRotationAtTime(currentTick);
                float interpPitch = rotation[0];
                float interpYaw = rotation[1];
                
                // Start approaching mode when getting close to target
                float pitchDiff = Math.abs(interpPitch - targetPitch);
                float yawDiff = Math.abs(normalizeAngle(interpYaw - normalizedTargetYaw));
                boolean isCloseToTarget = (pitchDiff < 5.0f && yawDiff < 5.0f);
                
                // Determine if we should apply enhanced smoothing
                boolean useEnhancedSmoothing = approachingTarget || jitterDetected || tickCounter <= EASING_IN_TICKS;
                
                if (useEnhancedSmoothing) {
                    // When close to target or in initial phase, use more smoothing
                    float blendFactor;
                    
                    if (tickCounter < EASING_IN_TICKS) {
                        // Initial easing
                        float easeAmount = (float)Math.pow(currentTick / (float)EASING_IN_TICKS, 2);
                        blendFactor = MIN_INITIAL_SPEED_FACTOR + (1.0f - MIN_INITIAL_SPEED_FACTOR) * easeAmount;
                        blendFactor *= partialTick; // Scale by partial tick for smooth interpolation
                    } else if (isCloseToTarget) {
                        // Approaching target
                        float distanceFactor = Math.min(1.0f, Math.max(pitchDiff, yawDiff) / 5.0f);
                        blendFactor = (APPROACH_SLOWDOWN + (1.0f - APPROACH_SLOWDOWN) * distanceFactor) * partialTick;
                    } else if (jitterDetected) {
                        // Jitter smoothing
                        blendFactor = 0.4f * partialTick;
                    } else {
                        // Default interpolation
                        blendFactor = 0.7f * partialTick;
                    }
                    
                    // Apply smoothed interpolation for pitch
                    interpPitch = lastPitch + (interpPitch - lastPitch) * blendFactor;
                    
                    // Apply smoothed interpolation for yaw with special wrapping handling
                    float yawDelta = normalizeAngle(interpYaw - lastYaw);
                    interpYaw = lastYaw + yawDelta * blendFactor;
                    while (interpYaw < -180) interpYaw += 360;
                    while (interpYaw >= 180) interpYaw -= 360;
                }
                
                // Apply interpolated position
                mc.thePlayer.rotationPitch = interpPitch;
                mc.thePlayer.rotationYaw = interpYaw;
            }
        }
        
        /**
         * Normalize angle to -180..180 range
         */
        private float normalizeAngle(float angle) {
            while (angle > 180) angle -= 360;
            while (angle < -180) angle += 360;
            return angle;
        }
    }
    
    /**
     * Physics-based rotation profile that handles coordinated pitch and yaw rotation
     */
    private class RotationProfile {
        // Initial and target positions
        private final float startPitch;
        private final float endPitch;
        private final float startYaw;
        private final float endYaw;
        
        // Distance to travel
        private final float pitchDistance;
        private final float yawDistance;
        
        // Max speeds and accelerations
        private final float maxPitchSpeed;
        private final float maxYawSpeed;
        private final float pitchAcceleration;
        private final float yawAcceleration;
        
        // Duration of rotation
        private final int duration;
        
        // Rotation phases (for each axis)
        private final AxisProfile pitchProfile;
        private final AxisProfile yawProfile;
        
        /**
         * Create a physics-based rotation profile
         */
        public RotationProfile(
                float startPitch, float endPitch, 
                float startYaw, float endYaw,
                float pitchSpeed, float yawSpeed, 
                float pitchAccel, float yawAccel) {
                
            this.startPitch = startPitch;
            this.endPitch = endPitch;
            this.startYaw = startYaw;
            this.endYaw = endYaw;
            
            // Calculate pitch distance
            this.pitchDistance = endPitch - startPitch;
            
            // Calculate yaw distance with shortest path
            float rawYawDist = endYaw - startYaw;
            float yawDist = rawYawDist;
            while (yawDist > 180) yawDist -= 360;
            while (yawDist < -180) yawDist += 360;
            this.yawDistance = yawDist;
            
            // Set speed and acceleration with direction
            this.maxPitchSpeed = pitchSpeed * Math.signum(pitchDistance);
            this.maxYawSpeed = yawSpeed * Math.signum(yawDistance);
            this.pitchAcceleration = pitchAccel * Math.signum(pitchDistance);
            this.yawAcceleration = yawAccel * Math.signum(yawDistance);
            
            // Create axis-specific profiles
            this.pitchProfile = new AxisProfile(
                Math.abs(pitchDistance), 
                Math.abs(maxPitchSpeed), 
                Math.abs(pitchAcceleration)
            );
            
            this.yawProfile = new AxisProfile(
                Math.abs(yawDistance), 
                Math.abs(maxYawSpeed), 
                Math.abs(yawAcceleration)
            );
            
            // Synchronize duration - use the longer time
            int pitchDuration = pitchProfile.getTotalDuration();
            int yawDuration = yawProfile.getTotalDuration();
            this.duration = Math.max(pitchDuration, yawDuration);
            
            // If durations differ, adjust the shorter one to match
            if (pitchDuration < duration && Math.abs(pitchDistance) > 0.1f) {
                pitchProfile.adjustDuration(duration);
            } else if (yawDuration < duration && Math.abs(yawDistance) > 0.1f) {
                yawProfile.adjustDuration(duration);
            }
            
            log("Created rotation profile: duration=" + duration + " ticks");
            log("Pitch: distance=" + String.format("%.2f", pitchDistance) + "°, " + 
                "max_speed=" + String.format("%.2f", maxPitchSpeed) + "°/tick");
            log("Yaw: distance=" + String.format("%.2f", yawDistance) + "°, " + 
                "max_speed=" + String.format("%.2f", maxYawSpeed) + "°/tick");
        }
        
        /**
         * Get the duration of this rotation in ticks
         */
        public int getDuration() {
            return duration;
        }
        
        /**
         * Get rotation at a specific time
         */
        public float[] getRotationAtTime(float time) {
            if (time <= 0) return new float[] { startPitch, startYaw };
            if (time >= duration) return new float[] { endPitch, endYaw };
            
            // Calculate progress for the current time
            float pitchProgress = pitchProfile.getProgressAtTime(time, duration);
            float yawProgress = yawProfile.getProgressAtTime(time, duration);
            
            // Calculate actual positions
            float pitch = startPitch + pitchDistance * pitchProgress;
            float yaw = startYaw + yawDistance * yawProgress;
            
            // Normalize yaw
            while (yaw < -180) yaw += 360;
            while (yaw >= 180) yaw -= 360;
            
            return new float[] { pitch, yaw };
        }
        
        /**
         * Represents movement profile for a single axis of rotation
         */
        private class AxisProfile {
            private final float distance;
            private float maxSpeed;
            private float acceleration;
            
            // Phase durations
            private int accelDuration;
            private int cruiseDuration;
            private int decelDuration;
            private int totalDuration;
            
            // Phase distances
            private float accelDistance;
            private float cruiseDistance;
            private float decelDistance;
            
            /**
             * Create a motion profile for a single axis
             */
            public AxisProfile(float distance, float maxSpeed, float acceleration) {
                this.distance = distance;
                this.maxSpeed = maxSpeed;
                this.acceleration = acceleration;
                
                // Calculate all phases
                calculatePhases();
            }
            
            /**
             * Calculate the three phases of the motion profile
             */
            private void calculatePhases() {
                // Time to reach max speed
                float timeToMaxSpeed = maxSpeed / acceleration;
                
                // Distance covered during acceleration/deceleration
                float accelDist = 0.5f * acceleration * timeToMaxSpeed * timeToMaxSpeed;
                
                // Check if we can reach max speed
                if (accelDist * 2 > distance) {
                    // Triangle profile - we never reach max speed
                    float timeToMiddle = (float) Math.sqrt(distance / acceleration);
                    accelDuration = Math.round(timeToMiddle);
                    cruiseDuration = 0;
                    decelDuration = accelDuration;
                    
                    // Calculate actual distances
                    accelDistance = 0.5f * acceleration * accelDuration * accelDuration;
                    cruiseDistance = 0;
                    decelDistance = distance - accelDistance;
                } else {
                    // Trapezoid profile - we reach max speed
                    accelDuration = Math.round(timeToMaxSpeed);
                    decelDuration = accelDuration;
                    
                    // Calculate distances
                    accelDistance = 0.5f * acceleration * accelDuration * accelDuration;
                    decelDistance = accelDistance;
                    cruiseDistance = distance - accelDistance - decelDistance;
                    
                    // Calculate cruise duration
                    cruiseDuration = Math.round(cruiseDistance / maxSpeed);
                }
                
                // Set total duration
                totalDuration = accelDuration + cruiseDuration + decelDuration;
            }
            
            /**
             * Get the total duration of this profile
             */
            public int getTotalDuration() {
                return totalDuration;
            }
            
            /**
             * Adjust the profile to match a new duration
             */
            public void adjustDuration(int newDuration) {
                if (newDuration <= totalDuration) return;
                
                // Scale down speeds and accelerations to stretch the profile
                float scaleFactor = (float)totalDuration / newDuration;
                float adjustedMaxSpeed = maxSpeed * scaleFactor;
                float adjustedAcceleration = acceleration * scaleFactor * scaleFactor;
                
                // Store the current values
                float oldMaxSpeed = maxSpeed;
                float oldAcceleration = acceleration;
                
                // Update fields (can't be final for this adjustment)
                this.maxSpeed = adjustedMaxSpeed;
                this.acceleration = adjustedAcceleration;
                
                // Recalculate phases with new parameters
                calculatePhases();
                
                // Ensure we match the target duration exactly
                if (totalDuration < newDuration) {
                    // Fine-tune by adding to cruise phase
                    cruiseDuration += (newDuration - totalDuration);
                    totalDuration = newDuration;
                }
                
                log("Adjusted profile: speed " + String.format("%.2f", oldMaxSpeed) + 
                    " → " + String.format("%.2f", adjustedMaxSpeed) + 
                    ", accel " + String.format("%.2f", oldAcceleration) + 
                    " → " + String.format("%.2f", adjustedAcceleration));
            }
            
            /**
             * Get movement progress at a specific time
             */
            public float getProgressAtTime(float time, int totalTime) {
                // Avoid division by zero and handle edge cases
                if (distance < 0.01f) return 1.0f;
                if (time <= 0) return 0.0f;
                if (time >= totalTime) return 1.0f;
                
                float position;
                
                // Handle triangular profile case (no cruise phase)
                if (cruiseDuration == 0) {
                    if (time <= accelDuration) {
                        // Acceleration phase
                        position = 0.5f * acceleration * time * time;
                    } else {
                        // Deceleration phase
                        float timeFromEnd = totalTime - time;
                        position = distance - 0.5f * acceleration * timeFromEnd * timeFromEnd;
                    }
                } else {
                    // Handle trapezoidal profile (with cruise phase)
                    if (time <= accelDuration) {
                        // Acceleration phase
                        position = 0.5f * acceleration * time * time;
                    } else if (time <= accelDuration + cruiseDuration) {
                        // Cruise phase
                        float cruiseTime = time - accelDuration;
                        position = accelDistance + (maxSpeed * cruiseTime);
                    } else {
                        // Deceleration phase
                        float timeFromEnd = totalTime - time;
                        position = distance - 0.5f * acceleration * timeFromEnd * timeFromEnd;
                    }
                }
                
                // Convert to 0-1 progress
                return position / distance;
            }
        }
    }
} 