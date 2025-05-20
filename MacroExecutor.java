package com.autofish.mod;

import net.minecraft.client.Minecraft;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.util.MathHelper;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.client.ClientCommandHandler;
import net.minecraftforge.fml.client.FMLClientHandler;
import net.minecraft.server.MinecraftServer;
import org.lwjgl.input.Keyboard;

import java.awt.Robot;
import java.awt.event.KeyEvent;
import java.awt.AWTException;
import java.io.*;
import java.nio.ByteBuffer;
import java.util.*;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.awt.Point;
import java.awt.MouseInfo;
import java.awt.event.InputEvent;
import java.lang.management.ManagementFactory;

// Add JNA imports for background key simulation
import com.sun.jna.Native;
import com.sun.jna.platform.win32.WinDef.HWND;
import com.sun.jna.platform.win32.WinDef.WORD;
import com.sun.jna.platform.win32.WinDef.DWORD;
import com.sun.jna.platform.win32.WinDef.WPARAM;
import com.sun.jna.platform.win32.WinDef.LPARAM;
import com.sun.jna.platform.win32.BaseTSD.ULONG_PTR; // Correct import location
import com.sun.jna.platform.win32.WinUser.INPUT;
import com.sun.jna.Pointer;
import com.sun.jna.ptr.IntByReference;
import com.sun.jna.win32.StdCallLibrary;
import com.sun.jna.Callback;
import com.sun.jna.Structure;

/**
 * 宏脚本执行器，用于执行预定义的一系列游戏操作
 */
public class MacroExecutor {
    private static final Minecraft mc = Minecraft.getMinecraft();
    private static final String CONFIG_DIR = "config";
    
    // 存储宏执行过程中的错误信息
    private List<String> errors = new ArrayList<>();
    
    // 当前执行的宏文件名
    private String currentMacroName;
    
    // 宏指令的执行器映射
    private Map<String, MacroAction> actionMap = new HashMap<>();
    
    // 添加方向记录存储
    private Map<String, float[]> savedDirections = new HashMap<>();
    
    // 添加新的按键映射常量
    private static final Map<String, Integer> KEY_MAP = new HashMap<>();
    
    // JNA用户定义常量
    private static final int KEYEVENTF_KEYDOWN = 0x0000;
    private static final int KEYEVENTF_KEYUP = 0x0002;
    private static final int KEYEVENTF_EXTENDEDKEY = 0x0001;
    private static final int INPUT_KEYBOARD = 1;
    
    // Window消息常量
    private static final int WM_KEYDOWN = 0x0100;
    private static final int WM_KEYUP = 0x0101;
    private static final int WM_SYSCOMMAND = 0x0112;
    
    // Virtual Key映射表 - 用于Windows API
    private static final Map<String, Integer> VK_MAP = new HashMap<>();
    
    // 自定义Windows User32接口，确保方法签名正确
    public interface User32Ex extends StdCallLibrary {
        User32Ex INSTANCE = Native.load("user32", User32Ex.class);
        
        HWND FindWindow(String winClass, String title);
        int GetWindowThreadProcessId(HWND hWnd, IntByReference lpdwProcessId);
        HWND GetForegroundWindow();
        boolean SetForegroundWindow(HWND hWnd);
        boolean EnumWindows(WNDENUMPROC lpEnumFunc, Pointer userData);
        int GetWindowTextA(HWND hWnd, byte[] lpString, int nMaxCount);
        int GetWindowTextW(HWND hWnd, char[] lpString, int nMaxCount);
        int SendInput(int nInputs, INPUT[] pInputs, int cbSize);
        
        // PostMessage返回boolean，不是void
        boolean PostMessage(HWND hWnd, int Msg, WPARAM wParam, LPARAM lParam);
    }
    
    public interface WNDENUMPROC extends StdCallLibrary.StdCallCallback {
        boolean callback(HWND hWnd, Pointer data);
    }
    
    /**
     * 初始化按键映射
     */
    static {
        // 字母键
        KEY_MAP.put("A", KeyEvent.VK_A);
        KEY_MAP.put("B", KeyEvent.VK_B);
        KEY_MAP.put("C", KeyEvent.VK_C);
        KEY_MAP.put("D", KeyEvent.VK_D);
        KEY_MAP.put("E", KeyEvent.VK_E);
        KEY_MAP.put("F", KeyEvent.VK_F);
        KEY_MAP.put("G", KeyEvent.VK_G);
        KEY_MAP.put("H", KeyEvent.VK_H);
        KEY_MAP.put("I", KeyEvent.VK_I);
        KEY_MAP.put("J", KeyEvent.VK_J);
        KEY_MAP.put("K", KeyEvent.VK_K);
        KEY_MAP.put("L", KeyEvent.VK_L);
        KEY_MAP.put("M", KeyEvent.VK_M);
        KEY_MAP.put("N", KeyEvent.VK_N);
        KEY_MAP.put("O", KeyEvent.VK_O);
        KEY_MAP.put("P", KeyEvent.VK_P);
        KEY_MAP.put("Q", KeyEvent.VK_Q);
        KEY_MAP.put("R", KeyEvent.VK_R);
        KEY_MAP.put("S", KeyEvent.VK_S);
        KEY_MAP.put("T", KeyEvent.VK_T);
        KEY_MAP.put("U", KeyEvent.VK_U);
        KEY_MAP.put("V", KeyEvent.VK_V);
        KEY_MAP.put("W", KeyEvent.VK_W);
        KEY_MAP.put("X", KeyEvent.VK_X);
        KEY_MAP.put("Y", KeyEvent.VK_Y);
        KEY_MAP.put("Z", KeyEvent.VK_Z);
        
        // 数字键
        KEY_MAP.put("0", KeyEvent.VK_0);
        KEY_MAP.put("1", KeyEvent.VK_1);
        KEY_MAP.put("2", KeyEvent.VK_2);
        KEY_MAP.put("3", KeyEvent.VK_3);
        KEY_MAP.put("4", KeyEvent.VK_4);
        KEY_MAP.put("5", KeyEvent.VK_5);
        KEY_MAP.put("6", KeyEvent.VK_6);
        KEY_MAP.put("7", KeyEvent.VK_7);
        KEY_MAP.put("8", KeyEvent.VK_8);
        KEY_MAP.put("9", KeyEvent.VK_9);
        
        // 功能键
        KEY_MAP.put("F1", KeyEvent.VK_F1);
        KEY_MAP.put("F2", KeyEvent.VK_F2);
        KEY_MAP.put("F3", KeyEvent.VK_F3);
        KEY_MAP.put("F4", KeyEvent.VK_F4);
        KEY_MAP.put("F5", KeyEvent.VK_F5);
        KEY_MAP.put("F6", KeyEvent.VK_F6);
        KEY_MAP.put("F7", KeyEvent.VK_F7);
        KEY_MAP.put("F8", KeyEvent.VK_F8);
        KEY_MAP.put("F9", KeyEvent.VK_F9);
        KEY_MAP.put("F10", KeyEvent.VK_F10);
        KEY_MAP.put("F11", KeyEvent.VK_F11);
        KEY_MAP.put("F12", KeyEvent.VK_F12);
        
        // 特殊键
        KEY_MAP.put("SPACE", KeyEvent.VK_SPACE);
        KEY_MAP.put("ENTER", KeyEvent.VK_ENTER);
        KEY_MAP.put("TAB", KeyEvent.VK_TAB);
        KEY_MAP.put("ESCAPE", KeyEvent.VK_ESCAPE);
        KEY_MAP.put("ESC", KeyEvent.VK_ESCAPE);
        KEY_MAP.put("BACKSPACE", KeyEvent.VK_BACK_SPACE);
        KEY_MAP.put("DELETE", KeyEvent.VK_DELETE);
        KEY_MAP.put("DEL", KeyEvent.VK_DELETE);
        KEY_MAP.put("INSERT", KeyEvent.VK_INSERT);
        KEY_MAP.put("INS", KeyEvent.VK_INSERT);
        KEY_MAP.put("HOME", KeyEvent.VK_HOME);
        KEY_MAP.put("END", KeyEvent.VK_END);
        KEY_MAP.put("PAGEUP", KeyEvent.VK_PAGE_UP);
        KEY_MAP.put("PAGEDOWN", KeyEvent.VK_PAGE_DOWN);
        
        // 控制键
        KEY_MAP.put("CTRL", KeyEvent.VK_CONTROL);
        KEY_MAP.put("CONTROL", KeyEvent.VK_CONTROL);
        KEY_MAP.put("SHIFT", KeyEvent.VK_SHIFT);
        KEY_MAP.put("ALT", KeyEvent.VK_ALT);
        KEY_MAP.put("ALTGR", KeyEvent.VK_ALT_GRAPH);
        KEY_MAP.put("META", KeyEvent.VK_META);
        KEY_MAP.put("WINDOWS", KeyEvent.VK_WINDOWS);
        KEY_MAP.put("WIN", KeyEvent.VK_WINDOWS);
        
        // 方向键
        KEY_MAP.put("UP", KeyEvent.VK_UP);
        KEY_MAP.put("DOWN", KeyEvent.VK_DOWN);
        KEY_MAP.put("LEFT", KeyEvent.VK_LEFT);
        KEY_MAP.put("RIGHT", KeyEvent.VK_RIGHT);
        
        // 初始化Windows Virtual Key映射
        // 字母键
        VK_MAP.put("A", 0x41);
        VK_MAP.put("B", 0x42);
        VK_MAP.put("C", 0x43);
        VK_MAP.put("D", 0x44);
        VK_MAP.put("E", 0x45);
        VK_MAP.put("F", 0x46);
        VK_MAP.put("G", 0x47);
        VK_MAP.put("H", 0x48);
        VK_MAP.put("I", 0x49);
        VK_MAP.put("J", 0x4A);
        VK_MAP.put("K", 0x4B);
        VK_MAP.put("L", 0x4C);
        VK_MAP.put("M", 0x4D);
        VK_MAP.put("N", 0x4E);
        VK_MAP.put("O", 0x4F);
        VK_MAP.put("P", 0x50);
        VK_MAP.put("Q", 0x51);
        VK_MAP.put("R", 0x52);
        VK_MAP.put("S", 0x53);
        VK_MAP.put("T", 0x54);
        VK_MAP.put("U", 0x55);
        VK_MAP.put("V", 0x56);
        VK_MAP.put("W", 0x57);
        VK_MAP.put("X", 0x58);
        VK_MAP.put("Y", 0x59);
        VK_MAP.put("Z", 0x5A);
        
        // 数字键
        VK_MAP.put("0", 0x30);
        VK_MAP.put("1", 0x31);
        VK_MAP.put("2", 0x32);
        VK_MAP.put("3", 0x33);
        VK_MAP.put("4", 0x34);
        VK_MAP.put("5", 0x35);
        VK_MAP.put("6", 0x36);
        VK_MAP.put("7", 0x37);
        VK_MAP.put("8", 0x38);
        VK_MAP.put("9", 0x39);
        
        // 功能键
        VK_MAP.put("F1", 0x70);
        VK_MAP.put("F2", 0x71);
        VK_MAP.put("F3", 0x72);
        VK_MAP.put("F4", 0x73);
        VK_MAP.put("F5", 0x74);
        VK_MAP.put("F6", 0x75);
        VK_MAP.put("F7", 0x76);
        VK_MAP.put("F8", 0x77);
        VK_MAP.put("F9", 0x78);
        VK_MAP.put("F10", 0x79);
        VK_MAP.put("F11", 0x7A);
        VK_MAP.put("F12", 0x7B);
        
        // 特殊键
        VK_MAP.put("SPACE", 0x20);
        VK_MAP.put("ENTER", 0x0D);
        VK_MAP.put("TAB", 0x09);
        VK_MAP.put("ESCAPE", 0x1B);
        VK_MAP.put("ESC", 0x1B);
        VK_MAP.put("BACKSPACE", 0x08);
        VK_MAP.put("DELETE", 0x2E);
        VK_MAP.put("DEL", 0x2E);
        VK_MAP.put("INSERT", 0x2D);
        VK_MAP.put("INS", 0x2D);
        VK_MAP.put("HOME", 0x24);
        VK_MAP.put("END", 0x23);
        VK_MAP.put("PAGEUP", 0x21);
        VK_MAP.put("PAGEDOWN", 0x22);
        
        // 控制键
        VK_MAP.put("CTRL", 0x11);
        VK_MAP.put("CONTROL", 0x11);
        VK_MAP.put("SHIFT", 0x10);
        VK_MAP.put("ALT", 0x12);
        VK_MAP.put("ALTGR", 0x12);
        VK_MAP.put("WINDOWS", 0x5B);
        VK_MAP.put("WIN", 0x5B);
        
        // 方向键
        VK_MAP.put("UP", 0x26);
        VK_MAP.put("DOWN", 0x28);
        VK_MAP.put("LEFT", 0x25);
        VK_MAP.put("RIGHT", 0x27);
    }
    
    /**
     * 初始化宏执行器，注册所有支持的宏指令
     */
    public MacroExecutor() {
        // 注册所有支持的宏指令
        actionMap.put("jump", this::executeJump);
        actionMap.put("rotate", this::executeRotate);
        actionMap.put("relativerotate", this::executeRelativeRotate);
        
        // 确保所有可能的拼写形式都能被识别 
        actionMap.put("rotatetonearesstplayer", this::executeRotateToNearestPlayer);
        actionMap.put("rotatetonearestplayer", this::executeRotateToNearestPlayer); 
        
        actionMap.put("leftclick", this::executeLeftClick);
        actionMap.put("rightclick", this::executeRightClick);
        actionMap.put("pressw", this::executePressW);
        actionMap.put("pressa", this::executePressA);
        actionMap.put("presss", this::executePressS);
        actionMap.put("pressd", this::executePressD);
        actionMap.put("releasew", this::executeReleaseW);
        actionMap.put("releasea", this::executeReleaseA);
        actionMap.put("releases", this::executeReleaseS);
        actionMap.put("released", this::executeReleaseD);
        actionMap.put("sneak", this::executeSneak);
        actionMap.put("unsneak", this::executeUnsneak);
        actionMap.put("pause", this::executePause);
        actionMap.put("disconnect", this::executeDisconnect);
        actionMap.put("chat", this::executeChat);
        actionMap.put("swap", this::executeSwap);
        // 添加新命令
        actionMap.put("recorddir", this::executeRecordDir);
        actionMap.put("rotateto", this::executeRotateTo);
        // 添加等待附近没有玩家的命令
        actionMap.put("waituntilplayerleaves", this::executeWaitUntilPlayerLeaves);
        // 添加ESC命令
        actionMap.put("esc", this::executeEsc);
        // 添加新的rpress命令
        actionMap.put("rpress", this::executeRobotPressBackground);
        // 添加GUI点击命令（支持单一索引和行列格式）
        actionMap.put("clickgui", this::executeClickGui);
    }
    
    /**
     * 执行指定名称的宏文件
     * @param macroName 宏文件名（不含路径和后缀）
     * @return 是否成功执行完成
     */
    public boolean executeMacro(String macroName) {
        this.currentMacroName = macroName;
        errors.clear();
        
        // 构建完整文件路径
        File configDir = new File(CONFIG_DIR);
        if (!configDir.exists()) {
            configDir.mkdirs();
        }
        
        File macroFile = new File(configDir, macroName + ".txt");
        if (!macroFile.exists()) {
            sendError("Macro file not found: " + macroFile.getPath());
            return false;
        }
        
        // 读取并执行宏文件
        try (BufferedReader reader = new BufferedReader(new FileReader(macroFile))) {
            List<String> lines = new ArrayList<>();
            String line;
            while ((line = reader.readLine()) != null) {
                // 忽略空行和注释行
                line = line.trim();
                if (!line.isEmpty() && !line.startsWith("//")) {
                    lines.add(line);
                }
            }
            
            // 检查disconnect是否只出现在文件末尾
            for (int i = 0; i < lines.size() - 1; i++) {
                if (lines.get(i).toLowerCase().startsWith("disconnect")) {
                    sendError("The 'disconnect' command can only be used at the end of the macro file");
                    return false;
                }
            }
            
            // 逐行执行宏指令
            for (int i = 0; i < lines.size(); i++) {
                String command = lines.get(i);
                if (!executeCommand(command, i + 1)) {
                    return false;
                }
            }
            
            sendMessage(EnumChatFormatting.GREEN + "Macro '" + macroName + "' executed successfully");
            return true;
        } catch (IOException e) {
            sendError("Error reading macro file: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * 执行单条宏指令
     * @param command 宏指令
     * @param lineNumber 行号（用于错误报告）
     * @return 执行是否成功
     */
    private boolean executeCommand(String command, int lineNumber) {
        try {
            // 特殊处理chat指令，因为它可能包含空格
            if (command.toLowerCase().startsWith("chat ")) {
                String chatMessage = command.substring(5); // 取出"chat "后面的所有内容作为消息
                return executeChat(new String[]{chatMessage}, lineNumber);
            }
            
            // 解析其他指令名称和参数
            String[] parts = command.split("\\s+");
            String actionName = parts[0].toLowerCase();
            
            // 查找对应的执行器
            MacroAction action = actionMap.get(actionName);
            if (action == null) {
                sendError("Line " + lineNumber + ": Unknown command '" + actionName + "'");
                return false;
            }
            
            // 提取参数并执行
            String[] args = Arrays.copyOfRange(parts, 1, parts.length);
            return action.execute(args, lineNumber);
        } catch (Exception e) {
            sendError("Line " + lineNumber + ": Error executing command '" + command + "': " + e.getMessage());
            return false;
        }
    }
    
    /**
     * 发送错误消息到游戏聊天栏
     * @param message 错误消息
     */
    private void sendError(String message) {
        if (mc.thePlayer != null) {
            mc.thePlayer.addChatMessage(new ChatComponentText(
                EnumChatFormatting.RED + "[Auto Fish Macro] Error: " + EnumChatFormatting.RESET + message));
        }
        errors.add(message);
    }
    
    /**
     * 发送一般消息到游戏聊天栏
     * @param message 消息内容
     */
    private void sendMessage(String message) {
        if (mc.thePlayer != null) {
            mc.thePlayer.addChatMessage(new ChatComponentText(EnumChatFormatting.GOLD + "[Macro] " + 
                                                            EnumChatFormatting.RESET + message));
        }
    }
    
    /**
     * 模拟键盘按键按下
     * @param keyBinding 按键绑定
     */
    private void pressKey(KeyBinding keyBinding) {
        try {
            final CountDownLatch latch = new CountDownLatch(1);
            
            mc.addScheduledTask(() -> {
                try {
                    KeyBinding.setKeyBindState(keyBinding.getKeyCode(), true);
                    KeyBinding.onTick(keyBinding.getKeyCode());
                    sendDebugMessage("Key pressed: " + keyBinding.getKeyDescription());
                } finally {
                    latch.countDown();
                }
            });
            
            latch.await(500, TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            sendError("Error pressing key: " + e.getMessage());
        }
    }
    
    /**
     * 模拟键盘按键释放
     * @param keyBinding 按键绑定
     */
    private void releaseKey(KeyBinding keyBinding) {
        try {
            final CountDownLatch latch = new CountDownLatch(1);
            
            mc.addScheduledTask(() -> {
                try {
                    KeyBinding.setKeyBindState(keyBinding.getKeyCode(), false);
                    sendDebugMessage("Key released: " + keyBinding.getKeyDescription());
                } finally {
                    latch.countDown();
                }
            });
            
            latch.await(500, TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            sendError("Error releasing key: " + e.getMessage());
        }
    }
    
    //===== 各种宏指令的实现 =====
    
    /**
     * 执行跳跃操作
     */
    private boolean executeJump(String[] args, int lineNumber) {
        if (args.length != 0) {
            sendError("Line " + lineNumber + ": 'jump' command takes no arguments");
            return false;
        }
        
        try {
            sendDebugMessage("Executing jump command");
            
            // 使用CountDownLatch确保主线程操作的同步
            final CountDownLatch pressLatch = new CountDownLatch(1);
            final CountDownLatch releaseLatch = new CountDownLatch(1);
            
            // 在主线程中按下跳跃键
            mc.addScheduledTask(() -> {
                try {
                    if (mc.thePlayer != null) {
                        sendDebugMessage("Pressing jump key");
                        KeyBinding.setKeyBindState(mc.gameSettings.keyBindJump.getKeyCode(), true);
                    }
                } finally {
                    pressLatch.countDown();
                }
            });
            
            // 等待按键按下操作完成
            pressLatch.await(500, TimeUnit.MILLISECONDS);
            
            // 保持按键按下状态200ms
            Thread.sleep(200);
            
            // 在主线程中释放跳跃键
            mc.addScheduledTask(() -> {
                try {
                    if (mc.thePlayer != null) {
                        sendDebugMessage("Releasing jump key");
                        KeyBinding.setKeyBindState(mc.gameSettings.keyBindJump.getKeyCode(), false);
                    }
                } finally {
                    releaseLatch.countDown();
                }
            });
            
            // 等待按键释放操作完成
            releaseLatch.await(500, TimeUnit.MILLISECONDS);
            
            sendDebugMessage("Jump command executed successfully");
            return true;
        } catch (InterruptedException e) {
            sendError("Jump operation was interrupted: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * 执行视角旋转操作
     * 参数：[pitch] [yaw] [speed]
     */
    private boolean executeRotate(String[] args, int lineNumber) {
        if (args.length != 3) {
            sendError("Line " + lineNumber + ": 'rotate' command requires exactly 3 arguments: pitch, yaw, speed");
            return false;
        }
        
        try {
            // 解析参数
            float targetPitch = Float.parseFloat(args[0]);
            float targetYaw = Float.parseFloat(args[1]);
            float speed = Float.parseFloat(args[2]);
            
            // 限制角度范围
            targetPitch = MathHelper.clamp_float(targetPitch, -90.0F, 90.0F);
            
            // 规范化偏航角
            while (targetYaw < -180) targetYaw += 360;
            while (targetYaw >= 180) targetYaw -= 360;
            
            // 输出实际执行的命令内容
            sendMessage("Rotating to pitch: " + targetPitch + ", yaw: " + targetYaw + " at speed: " + speed);
            
            // 平滑旋转到目标角度，保留速度参数的作用
            smoothRotateTo(-targetPitch, targetYaw, speed);  // 添加负号修正pitch方向
            return true;
        } catch (NumberFormatException e) {
            sendError("Line " + lineNumber + ": Invalid numbers in 'rotate' command");
            return false;
        } catch (InterruptedException e) {
            sendError("Line " + lineNumber + ": Rotation was interrupted");
            return false;
        }
    }
    
    /**
     * 执行旋转视角到最近的玩家
     * 参数：[speed]
     */
    private boolean executeRotateToNearestPlayer(String[] args, int lineNumber) {
        if (args.length != 1) {
            sendError("Line " + lineNumber + ": 'rotateToNearestPlayer' command requires exactly 1 argument: speed");
            return false;
        }
        
        try {
            float speed = Float.parseFloat(args[0]);
            
            // 寻找最近的玩家
            EntityPlayer nearestPlayer = findNearestPlayer(20.0);
            if (nearestPlayer == null) {
                // 如果没有找到玩家，直接返回成功但不执行旋转
                sendMessage("No players found within 20 blocks");
                return true;
            }
            
            sendMessage("Found nearest player: " + nearestPlayer.getName() + " at distance: " + 
                       Math.sqrt(mc.thePlayer.getDistanceSqToEntity(nearestPlayer)));
            
            // 计算目标角度
            double deltaX = nearestPlayer.posX - mc.thePlayer.posX;
            double deltaY = nearestPlayer.posY + nearestPlayer.getEyeHeight() - 
                           (mc.thePlayer.posY + mc.thePlayer.getEyeHeight());
            double deltaZ = nearestPlayer.posZ - mc.thePlayer.posZ;
            
            double horizontalDistance = Math.sqrt(deltaX * deltaX + deltaZ * deltaZ);
            float targetYaw = (float) Math.toDegrees(Math.atan2(deltaZ, deltaX)) - 90.0F;
            float targetPitch = (float) -Math.toDegrees(Math.atan2(deltaY, horizontalDistance));
            
            // 限制角度范围
            targetPitch = MathHelper.clamp_float(targetPitch, -90.0F, 90.0F);
            
            // 规范化偏航角
            while (targetYaw < -180) targetYaw += 360;
            while (targetYaw >= 180) targetYaw -= 360;
            
            sendMessage("Rotating to look at player, pitch: " + targetPitch + ", yaw: " + targetYaw);
            
            // 使用平滑旋转功能，使用targetPitch而不添加负号
            smoothRotateTo(targetPitch, targetYaw, speed);
            return true;
        } catch (NumberFormatException e) {
            sendError("Line " + lineNumber + ": Invalid number in 'rotateToNearestPlayer' command");
            return false;
        } catch (InterruptedException e) {
            sendError("Line " + lineNumber + ": Rotation was interrupted");
            return false;
        }
    }
    
    /**
     * 执行左键点击操作
     */
    private boolean executeLeftClick(String[] args, int lineNumber) {
        if (args.length != 0) {
            sendError("Line " + lineNumber + ": 'leftclick' command takes no arguments");
            return false;
        }
        
        pressKey(mc.gameSettings.keyBindAttack);
        releaseKey(mc.gameSettings.keyBindAttack);
        return true;
    }
    
    /**
     * 执行右键点击操作
     */
    private boolean executeRightClick(String[] args, int lineNumber) {
        if (args.length != 0) {
            sendError("Line " + lineNumber + ": 'rightclick' command takes no arguments");
            return false;
        }
        
        pressKey(mc.gameSettings.keyBindUseItem);
        releaseKey(mc.gameSettings.keyBindUseItem);
        return true;
    }
    
    /**
     * 执行按下W键操作
     */
    private boolean executePressW(String[] args, int lineNumber) {
        if (args.length != 0) {
            sendError("Line " + lineNumber + ": 'pressW' command takes no arguments");
            return false;
        }
        
        pressKey(mc.gameSettings.keyBindForward);
        return true;
    }
    
    /**
     * 执行按下A键操作
     */
    private boolean executePressA(String[] args, int lineNumber) {
        if (args.length != 0) {
            sendError("Line " + lineNumber + ": 'pressA' command takes no arguments");
            return false;
        }
        
        pressKey(mc.gameSettings.keyBindLeft);
        return true;
    }
    
    /**
     * 执行按下S键操作
     */
    private boolean executePressS(String[] args, int lineNumber) {
        if (args.length != 0) {
            sendError("Line " + lineNumber + ": 'pressS' command takes no arguments");
            return false;
        }
        
        pressKey(mc.gameSettings.keyBindBack);
        return true;
    }
    
    /**
     * 执行按下D键操作
     */
    private boolean executePressD(String[] args, int lineNumber) {
        if (args.length != 0) {
            sendError("Line " + lineNumber + ": 'pressD' command takes no arguments");
            return false;
        }
        
        pressKey(mc.gameSettings.keyBindRight);
        return true;
    }
    
    /**
     * 执行释放W键操作
     */
    private boolean executeReleaseW(String[] args, int lineNumber) {
        if (args.length != 0) {
            sendError("Line " + lineNumber + ": 'releaseW' command takes no arguments");
            return false;
        }
        
        releaseKey(mc.gameSettings.keyBindForward);
        return true;
    }
    
    /**
     * 执行释放A键操作
     */
    private boolean executeReleaseA(String[] args, int lineNumber) {
        if (args.length != 0) {
            sendError("Line " + lineNumber + ": 'releaseA' command takes no arguments");
            return false;
        }
        
        releaseKey(mc.gameSettings.keyBindLeft);
        return true;
    }
    
    /**
     * 执行释放S键操作
     */
    private boolean executeReleaseS(String[] args, int lineNumber) {
        if (args.length != 0) {
            sendError("Line " + lineNumber + ": 'releaseS' command takes no arguments");
            return false;
        }
        
        releaseKey(mc.gameSettings.keyBindBack);
        return true;
    }
    
    /**
     * 执行释放D键操作
     */
    private boolean executeReleaseD(String[] args, int lineNumber) {
        if (args.length != 0) {
            sendError("Line " + lineNumber + ": 'releaseD' command takes no arguments");
            return false;
        }
        
        releaseKey(mc.gameSettings.keyBindRight);
        return true;
    }
    
    /**
     * 执行蹲下操作
     */
    private boolean executeSneak(String[] args, int lineNumber) {
        if (args.length != 0) {
            sendError("Line " + lineNumber + ": 'sneak' command takes no arguments");
            return false;
        }
        
        pressKey(mc.gameSettings.keyBindSneak);
        return true;
    }
    
    /**
     * 执行站起操作
     */
    private boolean executeUnsneak(String[] args, int lineNumber) {
        if (args.length != 0) {
            sendError("Line " + lineNumber + ": 'unsneak' command takes no arguments");
            return false;
        }
        
        releaseKey(mc.gameSettings.keyBindSneak);
        return true;
    }
    
    /**
     * 执行暂停操作
     * 参数：[delay]
     */
    private boolean executePause(String[] args, int lineNumber) {
        if (args.length != 1) {
            sendError("Line " + lineNumber + ": 'pause' command requires exactly 1 argument: delay");
            return false;
        }
        
        try {
            int delay = Integer.parseInt(args[0]);
            if (delay < 0) {
                sendError("Line " + lineNumber + ": Pause delay cannot be negative");
                return false;
            }
            
            Thread.sleep(delay);
            return true;
        } catch (NumberFormatException e) {
            sendError("Line " + lineNumber + ": Invalid number in 'pause' command");
            return false;
        } catch (InterruptedException e) {
            sendError("Line " + lineNumber + ": Pause was interrupted");
            return false;
        }
    }
    
    /**
     * 执行断开连接操作
     */
    private boolean executeDisconnect(String[] args, int lineNumber) {
        if (args.length != 0) {
            sendError("Line " + lineNumber + ": 'disconnect' command takes no arguments");
            return false;
        }
        
        // 使用主线程调度器执行断开连接操作
        try {
            // 使用CountDownLatch来等待主线程执行完成
            final CountDownLatch latch = new CountDownLatch(1);
            final boolean[] success = {true};
            
            mc.addScheduledTask(() -> {
                try {
                    if (mc.theWorld != null) {
                        // 保存当前屏幕
                        net.minecraft.client.gui.GuiScreen currentScreen = mc.currentScreen;
                        
                        // 断开与服务器的连接
                        mc.theWorld.sendQuittingDisconnectingPacket();
                        
                        // 显示多人游戏界面，而不是直接使用loadWorld(null)
                        mc.displayGuiScreen(new net.minecraft.client.gui.GuiMainMenu());
                        
                        // 记录日志
                        sendMessage("Disconnected from server and returned to main menu");
                    }
                } catch (Exception e) {
                    sendError("Error during disconnect: " + e.getMessage());
                    e.printStackTrace();
                    success[0] = false;
                } finally {
                    latch.countDown();
                }
            });
            
            // 等待主线程完成操作，最多等待5秒
            latch.await(5, TimeUnit.SECONDS);
            
            // 给系统一些时间来处理UI更改
            Thread.sleep(1000);
            
            return success[0];
        } catch (InterruptedException e) {
            sendError("Line " + lineNumber + ": Disconnect operation was interrupted");
            return false;
        }
    }
    
    /**
     * 直接设置玩家视角（不使用平滑旋转）
     */
    private void setPlayerLookDirectly(float targetPitch, float targetYaw) throws InterruptedException {
        if (mc.thePlayer == null) return;
        
        // 获取当前角度用于日志输出
        float currentPitch = mc.thePlayer.rotationPitch;
        float currentYaw = mc.thePlayer.rotationYaw;
        
        // 规范化目标偏航角
        while (targetYaw < -180) targetYaw += 360;
        while (targetYaw >= 180) targetYaw -= 360;
        
        sendDebugMessage("Current rotation - pitch: " + currentPitch + ", yaw: " + currentYaw);
        sendDebugMessage("Setting rotation to - pitch: " + targetPitch + ", yaw: " + targetYaw);
        
        // 使用主线程调度器设置玩家视角
        final CountDownLatch latch = new CountDownLatch(1);
        final float finalTargetPitch = targetPitch;
        final float finalTargetYaw = targetYaw;
        
        mc.addScheduledTask(() -> {
            try {
                if (mc.thePlayer != null) {
                    // 直接设置视角
                    mc.thePlayer.rotationPitch = finalTargetPitch;
                    mc.thePlayer.rotationYaw = finalTargetYaw;
                    
                    // 输出设置后的实际角度
                    sendDebugMessage("Actual rotation after setting - pitch: " + 
                                    mc.thePlayer.rotationPitch + ", yaw: " + mc.thePlayer.rotationYaw);
                }
            } finally {
                latch.countDown();
            }
        });
        
        // 等待操作完成
        latch.await(2, TimeUnit.SECONDS);
    }
    
    /**
     * 平滑旋转视角到指定角度
     * @param targetPitch 目标俯仰角 (负值=低头，正值=抬头)
     * @param targetYaw 目标偏航角
     * @param speed 旋转速度（度/tick）
     */
    private void smoothRotateTo(float targetPitch, float targetYaw, float speed) throws InterruptedException {
        if (mc.thePlayer == null) return;

        // 获取当前角度
        final float startPitch = mc.thePlayer.rotationPitch;
        float startYaw = mc.thePlayer.rotationYaw;
        
        // 不再反转targetPitch的符号，保持方向一致
        // 创建final变量供后续lambda使用
        final float finalTargetPitch = targetPitch;
        
        // 规范化起始yaw和目标yaw，确保它们在[-180, 180)范围内
        while (startYaw < -180) startYaw += 360;
        while (startYaw >= 180) startYaw -= 360;
        
        // 规范化目标yaw
        float normalizedTargetYaw = targetYaw;
        while (normalizedTargetYaw < -180) normalizedTargetYaw += 360;
        while (normalizedTargetYaw >= 180) normalizedTargetYaw -= 360;
        
        sendDebugMessage("Starting smooth rotation from pitch: " + startPitch + ", yaw: " + startYaw);
        sendDebugMessage("Target rotation is pitch: " + targetPitch + ", yaw: " + normalizedTargetYaw + ", speed: " + speed);
        
        // 规范化偏航角差值，确保旋转走最短路径
        float yawDiff = normalizedTargetYaw - startYaw;
        while (yawDiff > 180) yawDiff -= 360;
        while (yawDiff < -180) yawDiff += 360;
        
        // 计算总角度差
        float pitchDiff = targetPitch - startPitch;
        float totalAngularDistance = (float) Math.sqrt(pitchDiff * pitchDiff + yawDiff * yawDiff);
        
        // 根据速度参数计算步数和延迟
        // 更高的速度 = 更少的步骤 + 更短的延迟
        int steps;
        int delayMs;
        
        if (speed <= 0.5f) {
            // 非常慢的速度
            steps = 25;
            delayMs = 60;
        } else if (speed <= 1.0f) {
            // 慢速
            steps = 20;
            delayMs = 50;
        } else if (speed <= 3.0f) {
            // 中速
            steps = 15;
            delayMs = 30;
        } else if (speed <= 5.0f) {
            // 快速
            steps = 10;
            delayMs = 20;
        } else if (speed <= 10.0f) {
            // 很快
            steps = 7;
            delayMs = 15;
        } else {
            // 非常快
            steps = 5;
            delayMs = 10;
        }
        
        // 确保至少有2步
        steps = Math.max(2, steps);
        
        sendDebugMessage("Angular distance: " + totalAngularDistance + ", will rotate in " + steps + 
                       " steps with " + delayMs + "ms delay per step");
        
        // 执行平滑旋转
        for (int i = 1; i <= steps; i++) {
            // 创建final变量供lambda使用
            final int currentStep = i;
            final int totalSteps = steps;
            final float progress = (float) i / steps;
            
            // 计算插值角度 - 使用平滑的缓动函数 (easeInOutQuad)
            float easedProgress;
            if (progress < 0.5f) {
                easedProgress = 2 * progress * progress;
            } else {
                easedProgress = -1 + (4 - 2 * progress) * progress;
            }
            
            final float currentPitch = startPitch + pitchDiff * easedProgress;
            float currentYaw = startYaw + yawDiff * easedProgress;
            
            // 规范化当前yaw，确保在[-180, 180)范围内
            while (currentYaw < -180) currentYaw += 360;
            while (currentYaw >= 180) currentYaw -= 360;
            
            final float finalCurrentYaw = currentYaw;
            
            // 在主线程中设置角度
            final CountDownLatch stepLatch = new CountDownLatch(1);
            
            mc.addScheduledTask(() -> {
                try {
                    if (mc.thePlayer != null) {
                        mc.thePlayer.rotationPitch = currentPitch;
                        mc.thePlayer.rotationYaw = finalCurrentYaw;
                        
                        // 使用final变量currentStep和totalSteps
                        if (currentStep == 1 || currentStep == totalSteps || currentStep % 5 == 0) {
                            sendDebugMessage("Step " + currentStep + "/" + totalSteps + ": Set pitch to " + 
                                           currentPitch + ", yaw to " + finalCurrentYaw);
                        }
                    }
                } finally {
                    stepLatch.countDown();
                }
            });
            
            // 等待此步骤完成
            stepLatch.await(500, TimeUnit.MILLISECONDS);
            
            // 使用计算出的延迟
            Thread.sleep(delayMs);
        }
        
        // 确保最终状态正确
        final CountDownLatch finalLatch = new CountDownLatch(1);
        
        // 使用标准化后的目标角度
        final float finalTargetYaw = normalizedTargetYaw;
        
        mc.addScheduledTask(() -> {
            try {
                if (mc.thePlayer != null) {
                    mc.thePlayer.rotationPitch = finalTargetPitch;
                    mc.thePlayer.rotationYaw = finalTargetYaw;
                    sendDebugMessage("Smooth rotation completed. Final pitch: " + 
                                    mc.thePlayer.rotationPitch + ", yaw: " + mc.thePlayer.rotationYaw);
                }
            } finally {
                finalLatch.countDown();
            }
        });
        
        finalLatch.await(500, TimeUnit.MILLISECONDS);
    }
    
    /**
     * 发送调试消息
     */
    private void sendDebugMessage(String message) {
        System.out.println("[AutoFish Macro Debug] " + message);
    }
    
    /**
     * 寻找指定范围内最近的玩家
     * @param maxDistance 最大搜索距离
     * @return 最近的玩家，如果没有找到则返回null
     */
    private EntityPlayer findNearestPlayer(double maxDistance) {
        if (mc.theWorld == null || mc.thePlayer == null) return null;
        
        EntityPlayer nearestPlayer = null;
        double nearestDistanceSq = maxDistance * maxDistance;
        
        for (EntityPlayer player : mc.theWorld.playerEntities) {
            if (player != mc.thePlayer && !player.isInvisible()) {
                double distanceSq = mc.thePlayer.getDistanceSqToEntity(player);
                if (distanceSq < nearestDistanceSq) {
                    nearestDistanceSq = distanceSq;
                    nearestPlayer = player;
                }
            }
        }
        
        return nearestPlayer;
    }
    
    /**
     * 执行聊天消息发送
     * 参数：[message] - 完整的聊天消息
     */
    private boolean executeChat(String[] args, int lineNumber) {
        if (args.length == 0) {
            sendError("Line " + lineNumber + ": 'chat' command requires a message");
            return false;
        }
        
        try {
            // 聊天消息，如果是从command方法直接传入的，则整个args[0]就是一条消息
            // 如果是从普通解析传入的，则需要将所有参数拼接
            String message;
            
            if (args.length == 1) {
                message = args[0];
            } else {
                // 多个参数拼接，用空格连接
                StringBuilder sb = new StringBuilder();
                for (int i = 0; i < args.length; i++) {
                    if (i > 0) {
                        sb.append(" ");
                    }
                    sb.append(args[i]);
                }
                message = sb.toString();
            }
            
            // 确保消息不为空
            if (message.trim().isEmpty()) {
                sendError("Line " + lineNumber + ": Chat message cannot be empty");
                return false;
            }
            
            sendDebugMessage("Executing chat command: " + message);
            
            // 检查是否是AutoFish指令
            if (message.startsWith("/af ") || message.equals("/af")) {
                sendDebugMessage("Detected AutoFish command: " + message);
                return executeAutoFishCommand(message, lineNumber);
            }
            
            // 直接执行聊天/命令
            final CountDownLatch latch = new CountDownLatch(1);
            final boolean[] success = {true};
            final String finalMessage = message;
            
            mc.addScheduledTask(() -> {
                try {
                    if (mc.thePlayer != null) {
                        // 发送聊天消息或命令 - 使用网络处理器直接发送
                        if (mc.thePlayer.sendQueue != null) {
                            mc.thePlayer.sendQueue.addToSendQueue(
                                new net.minecraft.network.play.client.C01PacketChatMessage(finalMessage)
                            );
                            sendDebugMessage("Message sent via network: " + finalMessage);
                        } else {
                            mc.thePlayer.sendChatMessage(finalMessage);
                            sendDebugMessage("Message sent via chat: " + finalMessage);
                        }
                    } else {
                        success[0] = false;
                        sendError("Line " + lineNumber + ": Cannot send message - player is null");
                    }
                } catch (Exception e) {
                    success[0] = false;
                    sendError("Line " + lineNumber + ": Error sending message: " + e.getMessage());
                } finally {
                    latch.countDown();
                }
            });
            
            // 等待主线程完成操作，最多等待1秒
            latch.await(1000, TimeUnit.MILLISECONDS);
            return success[0];
        } catch (Exception e) {
            sendError("Line " + lineNumber + ": Error in chat command: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * 执行AutoFish指令而不是发送聊天消息
     * @param command AutoFish命令
     * @param lineNumber 行号
     * @return 是否成功执行
     */
    private boolean executeAutoFishCommand(String command, int lineNumber) {
        try {
            final CountDownLatch latch = new CountDownLatch(1);
            final boolean[] success = {true};
            final String finalCommand = command;
            
            mc.addScheduledTask(() -> {
                try {
                    if (mc.thePlayer != null) {
                        // 显示执行信息
                        sendMessage("Executing AutoFish command: " + finalCommand);
                        
                        // 提取命令内容
                        String commandWithoutPrefix = finalCommand.startsWith("/af ") ? 
                                                   finalCommand.substring(4) : "";
                        
                        // 使用ClientCommandHandler本地执行命令
                        if (ClientCommandHandler.instance.executeCommand(mc.thePlayer, "/af" + (commandWithoutPrefix.isEmpty() ? "" : " " + commandWithoutPrefix)) != 0) {
                            sendDebugMessage("AutoFish command executed locally: " + finalCommand);
                        } else {
                            sendDebugMessage("Failed to execute AutoFish command: " + finalCommand);
                            success[0] = false;
                        }
                    } else {
                        success[0] = false;
                        sendError("Line " + lineNumber + ": Cannot execute AutoFish command - player is null");
                    }
                } catch (Exception e) {
                    success[0] = false;
                    sendError("Line " + lineNumber + ": Error executing AutoFish command: " + e.getMessage());
                    e.printStackTrace();
                } finally {
                    latch.countDown();
                }
            });
            
            // 等待主线程完成操作
            latch.await(1000, TimeUnit.MILLISECONDS);
            
            // 增加短暂延迟，确保命令有时间执行
            Thread.sleep(200);
            
            return success[0];
        } catch (Exception e) {
            sendError("Line " + lineNumber + ": Error executing AutoFish command: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }
    
    /**
     * 记录当前玩家朝向
     * 参数：[name] - 变量名
     */
    private boolean executeRecordDir(String[] args, int lineNumber) {
        if (args.length != 1) {
            sendError("Line " + lineNumber + ": 'recorddir' command requires exactly 1 argument: name");
            return false;
        }
        
        String dirName = args[0];
        
        // 检查变量名是否只包含英文字符
        if (!dirName.matches("[a-zA-Z]+")) {
            sendError("Line " + lineNumber + ": Direction name must contain only English letters");
            return false;
        }
        
        try {
            final CountDownLatch latch = new CountDownLatch(1);
            final boolean[] success = {true};
            
            mc.addScheduledTask(() -> {
                try {
                    if (mc.thePlayer != null) {
                        float pitch = mc.thePlayer.rotationPitch;
                        float yaw = mc.thePlayer.rotationYaw;
                        
                        // 规范化偏航角
                        while (yaw < -180) yaw += 360;
                        while (yaw >= 180) yaw -= 360;
                        
                        // 保存方向
                        savedDirections.put(dirName, new float[]{pitch, yaw});
                        sendMessage("Recorded direction '" + dirName + "': pitch=" + pitch + ", yaw=" + yaw);
                    } else {
                        success[0] = false;
                        sendError("Line " + lineNumber + ": Cannot record direction - player is null");
                    }
                } catch (Exception e) {
                    success[0] = false;
                    sendError("Line " + lineNumber + ": Error recording direction: " + e.getMessage());
                } finally {
                    latch.countDown();
                }
            });
            
            latch.await(1000, TimeUnit.MILLISECONDS);
            return success[0];
        } catch (Exception e) {
            sendError("Line " + lineNumber + ": Error in recorddir command: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * 旋转到已保存的方向
     * 参数：[name] [speed] - 方向变量名和旋转速度
     */
    private boolean executeRotateTo(String[] args, int lineNumber) {
        if (args.length != 2) {
            sendError("Line " + lineNumber + ": 'rotateTo' command requires exactly 2 arguments: name, speed");
            return false;
        }
        
        String dirName = args[0];
        
        try {
            float speed = Float.parseFloat(args[1]);
            
            // 检查方向是否已记录
            if (!savedDirections.containsKey(dirName)) {
                sendError("Line " + lineNumber + ": Direction '" + dirName + "' has not been recorded");
                return false;
            }
            
            float[] direction = savedDirections.get(dirName);
            float targetPitch = direction[0];
            float targetYaw = direction[1];
            
            // 限制俯仰角范围
            targetPitch = MathHelper.clamp_float(targetPitch, -90.0F, 90.0F);
            
            // 规范化偏航角
            while (targetYaw < -180) targetYaw += 360;
            while (targetYaw >= 180) targetYaw -= 360;
            
            sendMessage("Rotating to saved direction '" + dirName + "': pitch=" + targetPitch + ", yaw=" + targetYaw + " at speed: " + speed);
            
            // 使用平滑旋转，这里不需要给pitch加负号，因为保存的就是原始值
            smoothRotateTo(targetPitch, targetYaw, speed);
            return true;
        } catch (NumberFormatException e) {
            sendError("Line " + lineNumber + ": Invalid speed value in 'rotateTo' command");
            return false;
        } catch (InterruptedException e) {
            sendError("Line " + lineNumber + ": Rotation was interrupted");
            return false;
        }
    }
    
    /**
     * 执行相对旋转视角操作
     * 参数：[pitch] [yaw] [speed]
     * 相对于当前视角进行旋转
     */
    private boolean executeRelativeRotate(String[] args, int lineNumber) {
        if (args.length != 3) {
            sendError("Line " + lineNumber + ": 'relativeRotate' command requires exactly 3 arguments: pitch, yaw, speed");
            return false;
        }
        
        try {
            // 解析参数
            float deltaPitch = Float.parseFloat(args[0]);
            float deltaYaw = Float.parseFloat(args[1]);
            float speed = Float.parseFloat(args[2]);
            
            // 获取当前视角
            float currentPitch = mc.thePlayer.rotationPitch;
            float currentYaw = mc.thePlayer.rotationYaw;
            
            // 规范化当前偏航角
            while (currentYaw < -180) currentYaw += 360;
            while (currentYaw >= 180) currentYaw -= 360;
            
            // 计算目标视角（相对旋转）
            float targetPitch = currentPitch + deltaPitch;
            float targetYaw = currentYaw + deltaYaw;
            
            // 限制俯仰角范围在-90到90度之间
            targetPitch = MathHelper.clamp_float(targetPitch, -90.0F, 90.0F);
            
            // 规范化目标偏航角
            while (targetYaw < -180) targetYaw += 360;
            while (targetYaw >= 180) targetYaw -= 360;
            
            // 输出实际执行的命令内容
            sendMessage("Rotating relatively by pitch: " + deltaPitch + ", yaw: " + deltaYaw + 
                       " to pitch: " + targetPitch + ", yaw: " + targetYaw + " at speed: " + speed);
            
            // 使用平滑旋转，不再给pitch加负号
            smoothRotateTo(targetPitch, targetYaw, speed);
            return true;
        } catch (NumberFormatException e) {
            sendError("Line " + lineNumber + ": Invalid numbers in 'relativeRotate' command");
            return false;
        } catch (InterruptedException e) {
            sendError("Line " + lineNumber + ": RelativeRotation was interrupted");
            return false;
        }
    }
    
    /**
     * 执行切换快捷栏物品操作
     * 参数：[slot] - 快捷栏槽位(1-9)
     */
    private boolean executeSwap(String[] args, int lineNumber) {
        if (args.length != 1) {
            sendError("Line " + lineNumber + ": 'swap' command requires exactly 1 argument: slot (1-9)");
            return false;
        }
        
        try {
            // 解析槽位参数(1-9)
            int slot = Integer.parseInt(args[0]);
            
            // 验证槽位范围
            if (slot < 1 || slot > 9) {
                sendError("Line " + lineNumber + ": Slot number must be between 1 and 9");
                return false;
            }
            
            // Minecraft中的快捷栏索引从0开始，而用户输入的从1开始
            final int mcSlotIndex = slot - 1;
            
            sendDebugMessage("Swapping to hotbar slot " + slot + " (internal index: " + mcSlotIndex + ")");
            
            // 使用主线程调度器切换物品
            final CountDownLatch latch = new CountDownLatch(1);
            final boolean[] success = {true};
            
            mc.addScheduledTask(() -> {
                try {
                    if (mc.thePlayer != null) {
                        // 保存当前选中的槽位
                        int previousSlot = mc.thePlayer.inventory.currentItem;
                        
                        // 切换到指定槽位
                        mc.thePlayer.inventory.currentItem = mcSlotIndex;
                        
                        // 输出日志
                        sendDebugMessage("Swapped from slot " + (previousSlot + 1) + " to slot " + slot);
                    } else {
                        success[0] = false;
                    }
                } catch (Exception e) {
                    sendError("Error during hotbar swap: " + e.getMessage());
                    success[0] = false;
                } finally {
                    latch.countDown();
                }
            });
            
            // 等待操作完成
            latch.await(1, TimeUnit.SECONDS);
            return success[0];
        } catch (NumberFormatException e) {
            sendError("Line " + lineNumber + ": Invalid number in 'swap' command: " + args[0]);
            return false;
        } catch (InterruptedException e) {
            sendError("Line " + lineNumber + ": Swap operation was interrupted");
            return false;
        }
    }
    
    /**
     * 等待附近没有玩家
     * 参数：[timeout] - 最大等待时间(毫秒)，可选参数
     */
    private boolean executeWaitUntilPlayerLeaves(String[] args, int lineNumber) {
        long timeout = 0; // 默认无限等待
        if (args.length > 0) {
            try {
                timeout = Long.parseLong(args[0]);
                if (timeout < 0) {
                    sendError("Line " + lineNumber + ": Timeout cannot be negative");
                    return false;
                }
            } catch (NumberFormatException e) {
                sendError("Line " + lineNumber + ": Invalid timeout value: " + args[0]);
                return false;
            }
        }
        
        // 获取AutoFishMod实例
        AutoFishMod afMod = getAutoFishMod();
        if (afMod == null) {
            sendMessage("Warning: Unable to access AutoFishMod, using fallback detection method");
        } else {
            // 尝试获取检测范围
            try {
                Field rangeField = AutoFishMod.class.getDeclaredField("NEARBY_PLAYER_RANGE");
                rangeField.setAccessible(true);
                double range = rangeField.getDouble(afMod);
                sendMessage("Waiting until all players leave detection range (" + String.format("%.1f", range) + " blocks)...");
            } catch (Exception e) {
                sendMessage("Waiting until all players leave the area...");
            }
        }
        
        long startTime = System.currentTimeMillis();
        boolean hasTimeout = timeout > 0;
        long lastStatusUpdateTime = 0;
        boolean playersWereDetected = false;
        
        try {
            while (true) {
                // 检测是否有附近玩家
                boolean hasNearbyPlayers = (afMod != null) ? 
                    hasNearbyPlayersUsingModCheck(afMod) : hasNearbyPlayers();
                
                long currentTime = System.currentTimeMillis();
                
                // 如果有玩家，记录检测到玩家的状态
                if (hasNearbyPlayers) {
                    playersWereDetected = true;
                    
                    // 每5秒更新一次状态消息
                    if (currentTime - lastStatusUpdateTime > 5000) {
                        if (hasTimeout) {
                            long elapsed = currentTime - startTime;
                            long remaining = timeout - elapsed;
                            if (remaining > 0) {
                                sendMessage("Players still nearby. Continuing to wait... (" + 
                                          (remaining / 1000) + " seconds remaining)");
                            }
                        } else {
                            sendMessage("Players still nearby. Continuing to wait indefinitely...");
                        }
                        lastStatusUpdateTime = currentTime;
                    }
                } else {
                    // 没有检测到附近玩家
                    if (playersWereDetected) {
                        // 之前检测到玩家，现在离开了
                        sendMessage("Players have left the area - continuing macro");
                    } else {
                        // 从未检测到玩家
                        sendMessage("No players were detected in the area - continuing macro");
                    }
                    return true;
                }
                
                // 检查是否超时
                if (hasTimeout && currentTime - startTime > timeout) {
                    sendMessage("Wait timed out after " + (timeout / 1000) + " seconds - continuing macro anyway");
                    return true;
                }
                
                // 间隔检查，降低到300ms以提高响应速度
                Thread.sleep(300);
            }
        } catch (InterruptedException e) {
            sendError("Line " + lineNumber + ": Wait was interrupted");
            return false;
        }
    }
    
    /**
     * 获取AutoFishMod实例
     */
    private AutoFishMod getAutoFishMod() {
        try {
            // 尝试通过反射获取
            try {
                Field instanceField = AutoFishMod.class.getDeclaredField("INSTANCE");
                instanceField.setAccessible(true);
                Object instance = instanceField.get(null);
                if (instance instanceof AutoFishMod) {
                    return (AutoFishMod) instance;
                }
            } catch (Exception e) {
                sendDebugMessage("Could not get AutoFishMod instance through INSTANCE field: " + e.getMessage());
            }
            
            // 尝试通过@Mod注解获取实例
            try {
                net.minecraftforge.fml.common.Loader loader = net.minecraftforge.fml.common.Loader.instance();
                Object modContainer = loader.getIndexedModList().get(AutoFishMod.MODID);
                if (modContainer != null) {
                    Field modField = modContainer.getClass().getDeclaredField("mod");
                    modField.setAccessible(true);
                    Object mod = modField.get(modContainer);
                    if (mod instanceof AutoFishMod) {
                        return (AutoFishMod) mod;
                    }
                }
            } catch (Exception e) {
                sendDebugMessage("Could not get AutoFishMod instance through mod loader: " + e.getMessage());
            }
        } catch (Exception e) {
            sendDebugMessage("Error getting AutoFishMod: " + e.getMessage());
        }
        return null;
    }
    
    /**
     * 使用AutoFishMod来检查是否有附近玩家
     */
    private boolean hasNearbyPlayersUsingModCheck(AutoFishMod mod) {
        if (mod == null) {
            // 如果无法获取AutoFishMod实例，使用默认检测方法
            return hasNearbyPlayers();
        }
        
        // 检查附近玩家状态
        try {
            // 获取相关字段
            Field disabledDueToNearbyPlayersField = AutoFishMod.class.getDeclaredField("disabledDueToNearbyPlayers");
            disabledDueToNearbyPlayersField.setAccessible(true);
            
            Field nearbyPlayerNamesField = AutoFishMod.class.getDeclaredField("nearbyPlayerNames");
            nearbyPlayerNamesField.setAccessible(true);
            
            // 获取玩家检测范围
            Field playerCheckRangeField = AutoFishMod.class.getDeclaredField("NEARBY_PLAYER_RANGE");
            playerCheckRangeField.setAccessible(true);
            double detectionRange = playerCheckRangeField.getDouble(mod);
            
            // 获取配置对象
            Field configField = AutoFishMod.class.getDeclaredField("config");
            configField.setAccessible(true);
            AutoFishConfig config = (AutoFishConfig) configField.get(mod);
            
            // 强制执行一次检测，确保状态是最新的
            // 使用直接检测方式而不是依赖mod的状态
            if (mc.thePlayer != null && mc.theWorld != null) {
                double x = mc.thePlayer.posX;
                double y = mc.thePlayer.posY;
                double z = mc.thePlayer.posZ;
                
                // 使用更大的垂直检测范围
                double verticalRange = detectionRange * 1.5;
                
                net.minecraft.util.AxisAlignedBB boundingBox = new net.minecraft.util.AxisAlignedBB(
                    x - detectionRange, y - verticalRange, z - detectionRange,
                    x + detectionRange, y + verticalRange, z + detectionRange
                );
                
                // 直接检测附近玩家
                List<net.minecraft.entity.player.EntityPlayer> nearbyPlayers = mc.theWorld.getEntitiesWithinAABB(
                    net.minecraft.entity.player.EntityPlayer.class, boundingBox);
                
                boolean hasNearbyPlayer = false;
                List<String> playerNames = new ArrayList<>();
                
                for (net.minecraft.entity.player.EntityPlayer player : nearbyPlayers) {
                    if (player != mc.thePlayer) {
                        // 检查玩家是否在白名单中
                        if (config != null && config.isPlayerWhitelisted(player.getName())) {
                            sendDebugMessage("Ignored whitelisted player: " + player.getName());
                            continue; // 跳过白名单内的玩家
                        }
                        
                        hasNearbyPlayer = true;
                        playerNames.add(player.getName());
                    }
                }
                
                if (hasNearbyPlayer) {
                    sendDebugMessage("Players detected: " + String.join(", ", playerNames));
                    return true;
                } else {
                    sendDebugMessage("No players detected in range " + detectionRange);
                    return false;
                }
            }
            
            // 如果无法直接检测（MC实例不完整），则回退到使用mod的状态
            boolean disabledDueToNearbyPlayers = disabledDueToNearbyPlayersField.getBoolean(mod);
            @SuppressWarnings("unchecked")
            List<String> nearbyPlayerNames = (List<String>) nearbyPlayerNamesField.get(mod);
            
            boolean hasPlayers = disabledDueToNearbyPlayers || (nearbyPlayerNames != null && !nearbyPlayerNames.isEmpty());
            
            if (hasPlayers) {
                sendDebugMessage("Players detected by AutoFishMod state: " + 
                              (nearbyPlayerNames != null ? String.join(", ", nearbyPlayerNames) : "unknown"));
            } else {
                sendDebugMessage("No players detected by AutoFishMod state");
            }
            
            return hasPlayers;
        } catch (Exception e) {
            sendDebugMessage("Error checking nearby players using AutoFishMod: " + e.getMessage());
            return hasNearbyPlayers(); // 失败时回退到默认方法
        }
    }
    
    /**
     * 检查是否有附近玩家(备用方案)
     * @return 是否有玩家
     */
    private boolean hasNearbyPlayers() {
        final boolean[] result = {false};
        final CountDownLatch latch = new CountDownLatch(1);
        
        mc.addScheduledTask(() -> {
            try {
                if (mc.thePlayer != null && mc.theWorld != null) {
                    // 尝试获取AutoFishConfig中配置的检测范围
                    double range = 1.5; // 默认范围
                    AutoFishConfig config = null; // 添加配置对象
                    
                    try {
                        // 尝试获取配置
                        
                        // 先尝试从AutoFishMod获取
                        try {
                            AutoFishMod mod = getAutoFishMod();
                            if (mod != null) {
                                Field configField = AutoFishMod.class.getDeclaredField("config");
                                configField.setAccessible(true);
                                config = (AutoFishConfig) configField.get(mod);
                                
                                // 获取范围设置
                                Field rangeField = AutoFishMod.class.getDeclaredField("NEARBY_PLAYER_RANGE");
                                rangeField.setAccessible(true);
                                range = rangeField.getDouble(mod);
                            }
                        } catch (Exception e) {
                            sendDebugMessage("Failed to get config from mod: " + e.getMessage());
                        }
                        
                        // 如果无法从mod获取，尝试创建一个新实例
                        if (config == null) {
                            try {
                                // 尝试查找config目录
                                File configDir = new File("config");
                                if (configDir.exists() && configDir.isDirectory()) {
                                    // 创建配置对象
                                    File configFile = new File(configDir, "autofish.cfg");
                                if (configFile.exists()) {
                                    config = new AutoFishConfig(configFile);
                                        
                                        // 获取范围设置
                                        range = config.getPlayerCheckRange();
                                        sendDebugMessage("Created new config instance, range: " + range);
                                    }
                                }
                            } catch (Exception e) {
                                sendDebugMessage("Failed to create config: " + e.getMessage());
                            }
                        }
                    } catch (Exception e) {
                        sendDebugMessage("Error getting player check range: " + e.getMessage());
                    }
                    
                    // 使用更大的垂直检测范围
                    double verticalRange = range * 1.5;
                    double x = mc.thePlayer.posX;
                    double y = mc.thePlayer.posY;
                    double z = mc.thePlayer.posZ;
                    
                    net.minecraft.util.AxisAlignedBB boundingBox = new net.minecraft.util.AxisAlignedBB(
                        x - range, y - verticalRange, z - range,
                        x + range, y + verticalRange, z + range
                    );
                    
                    // 检测附近玩家
                    List<net.minecraft.entity.player.EntityPlayer> nearbyPlayers = mc.theWorld.getEntitiesWithinAABB(
                        net.minecraft.entity.player.EntityPlayer.class, boundingBox);
                    
                    // 检查非主玩家
                    for (net.minecraft.entity.player.EntityPlayer player : nearbyPlayers) {
                        if (player != mc.thePlayer) {
                            // 检查玩家是否在白名单中
                            if (config != null && config.isPlayerWhitelisted(player.getName())) {
                                sendDebugMessage("Ignored whitelisted player: " + player.getName());
                                continue; // 跳过白名单内的玩家
                            }
                            
                            result[0] = true;
                            sendDebugMessage("Player detected: " + player.getName() + " at distance: " + 
                                          Math.sqrt(mc.thePlayer.getDistanceSqToEntity(player)));
                            break;
                        }
                    }
                    
                    if (!result[0]) {
                        sendDebugMessage("No players detected in range " + range);
                    }
                }
            } finally {
                latch.countDown();
            }
        });
        
        try {
            latch.await(500, TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            sendDebugMessage("Player check was interrupted");
        }
        
        return result[0];
    }
    
    /**
     * 执行ESC键操作
     */
    private boolean executeEsc(String[] args, int lineNumber) {
        if (args.length != 0) {
            sendError("Line " + lineNumber + ": 'esc' command takes no arguments");
            return false;
        }
        
        try {
            sendDebugMessage("Executing ESC key press using Robot");
            
            // 创建Robot实例
            Robot robot = new Robot();
            
            // 模拟ESC键按下和释放，使用KeyEvent.VK_ESCAPE常量
            robot.keyPress(KeyEvent.VK_ESCAPE);
            Thread.sleep(50); // 短暂延迟，确保按键被注册
            robot.keyRelease(KeyEvent.VK_ESCAPE);
            
            sendMessage("ESC key pressed");
            return true;
        } catch (AWTException e) {
            sendError("Line " + lineNumber + ": Error creating Robot instance: " + e.getMessage());
            return false;
        } catch (InterruptedException e) {
            sendError("Line " + lineNumber + ": ESC operation was interrupted");
            return false;
        } catch (Exception e) {
            sendError("Line " + lineNumber + ": Unexpected error during ESC operation: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * 获取Minecraft窗口句柄
     * @return 窗口句柄或null
     */
    private HWND findMinecraftWindow() {
        // 使用自定义的User32Ex
        User32Ex user32 = User32Ex.INSTANCE;
        
        // 尝试几种可能的Minecraft窗口标题
        final String[] possibleTitles = {
            "Minecraft", 
            "Minecraft 1.8.9", 
            "Minecraft* 1.8.9",
            "Minecraft*"
        };
        
        for (String title : possibleTitles) {
            HWND hwnd = user32.FindWindow(null, title);
            if (hwnd != null && !hwnd.equals(Pointer.NULL)) {
                sendDebugMessage("Found Minecraft window with title: " + title);
                return hwnd;
            }
        }
        
        // 尝试使用JVM进程ID查找
        try {
            // 获取当前进程的进程名和ID
            String jvmName = ManagementFactory.getRuntimeMXBean().getName();
            sendDebugMessage("Current JVM process: " + jvmName);
            
            // 从JVM名称中提取PID（通常格式为 "PID@hostname"）
            String pidStr = jvmName.split("@")[0];
            final int pid = Integer.parseInt(pidStr);
            
            // 尝试通过枚举所有窗口查找匹配的PID
            final List<HWND> matchingWindows = new ArrayList<>();
            
            user32.EnumWindows(new WNDENUMPROC() {
                @Override
                public boolean callback(HWND hwnd, Pointer data) {
                    IntByReference processId = new IntByReference();
                    user32.GetWindowThreadProcessId(hwnd, processId);
                    
                    if (processId.getValue() == pid) {
                        char[] windowText = new char[512];
                        user32.GetWindowTextW(hwnd, windowText, 512);
                        String windowTitle = Native.toString(windowText);
                        
                        sendDebugMessage("Found window with matching PID: " + windowTitle + " (PID: " + pid + ")");
                        matchingWindows.add(hwnd);
                    }
                    return true;
                }
            }, null);
            
            if (!matchingWindows.isEmpty()) {
                sendDebugMessage("Using window with matching PID: " + matchingWindows.get(0));
                return matchingWindows.get(0);
            }
        } catch (Exception e) {
            sendDebugMessage("Error finding window by PID: " + e.getMessage());
        }
        
        // 尝试通过枚举所有窗口查找
        final List<HWND> minecraftWindows = new ArrayList<>();
        user32.EnumWindows(new WNDENUMPROC() {
            @Override
            public boolean callback(HWND hwnd, Pointer data) {
                char[] windowText = new char[512];
                user32.GetWindowTextW(hwnd, windowText, 512);
                String windowTitle = Native.toString(windowText);
                
                if (windowTitle.contains("Minecraft")) {
                    IntByReference processId = new IntByReference();
                    user32.GetWindowThreadProcessId(hwnd, processId);
                    
                    sendDebugMessage("Found potential window: " + windowTitle + " (PID: " + processId.getValue() + ")");
                    minecraftWindows.add(hwnd);
                }
                return true;
            }
        }, null);
        
        if (!minecraftWindows.isEmpty()) {
            sendDebugMessage("Using first matching window: " + minecraftWindows.get(0));
            return minecraftWindows.get(0);
        }
        
        sendDebugMessage("No Minecraft window found, using foreground window as fallback");
        return user32.GetForegroundWindow(); // 作为后备方案，使用当前活动窗口
    }
    
    /**
     * 确定键是否为扩展键
     * 扩展键包括insert、delete、home、end、pageup、pagedown、方向键等
     */
    private boolean isExtendedKey(int vkCode) {
        // 扩展键列表
        return Arrays.asList(
            0x21, // Page Up
            0x22, // Page Down
            0x23, // End
            0x24, // Home
            0x25, // Left Arrow
            0x26, // Up Arrow
            0x27, // Right Arrow
            0x28, // Down Arrow
            0x2D, // Insert
            0x2E  // Delete
        ).contains(vkCode);
    }
    
    /**
     * 执行后台按键模拟(替换原来的executeRobotPress方法)
     * 参数：[key1] [key2] ... [duration]
     * key: 按键名称，如A, 5, SPACE, CTRL等，可以指定多个按键
     * duration: 按下时长(毫秒)，必须是最后一个参数
     */
    private boolean executeRobotPressBackground(String[] args, int lineNumber) {
        if (args.length < 2) {
            sendError("Line " + lineNumber + ": 'rpress' command requires at least 2 arguments: key duration (or multiple keys followed by duration)");
            return false;
        }
        
        // 最后一个参数应该是持续时间
        int duration;
        try {
            duration = Integer.parseInt(args[args.length - 1]);
            if (duration < 0) {
                sendError("Line " + lineNumber + ": Duration must be a positive integer");
                return false;
            }
        } catch (NumberFormatException e) {
            sendError("Line " + lineNumber + ": Invalid duration: " + args[args.length - 1] + ". Last argument must be an integer duration.");
            return false;
        }
        
        // 收集所有按键的键码
        List<Integer> virtualKeyCodes = new ArrayList<>();
        List<String> keyNames = new ArrayList<>();
        for (int i = 0; i < args.length - 1; i++) {
            String keyName = args[i].toUpperCase();
            keyNames.add(keyName);
            Integer vkCode = VK_MAP.get(keyName);
            if (vkCode == null) {
                sendError("Line " + lineNumber + ": Unknown key: " + keyName);
                return false;
            }
            virtualKeyCodes.add(vkCode);
        }
        
        try {
            // 查找Minecraft窗口
            HWND minecraftWindow = findMinecraftWindow();
            
            if (minecraftWindow == null || minecraftWindow.equals(Pointer.NULL)) {
                sendError("Line " + lineNumber + ": Could not find Minecraft window. Falling back to Robot method.");
                return executeRobotPressFallback(args, lineNumber);
            }
            
            sendMessage("Sending keys to Minecraft window in background: " + String.join(", ", keyNames));
            sendDebugMessage("Using Windows API to send keys to window handle: " + minecraftWindow);
            
            // 获取User32实例用于PostMessage
            User32Ex user32 = User32Ex.INSTANCE;
            
            // 标记是否所有PostMessage都成功
            boolean allPostMessagesSucceeded = true;
            
            // 按下所有键
            for (int i = 0; i < virtualKeyCodes.size(); i++) {
                int vkCode = virtualKeyCodes.get(i);
                boolean isExtended = isExtendedKey(vkCode);
                
                // 创建消息参数
                WPARAM wParam = new WPARAM(vkCode);
                // lParam格式: 0-15位是repeat count, 16-23位是scan code, 24位是extended flag, 
                // 25-28位保留, 29位是context code, 30位是previous key state, 31位是transition state
                int lParamValue = 0x00000001; // repeat count = 1
                if (isExtended) {
                    lParamValue |= 0x01000000; // extended key flag
                }
                LPARAM lParam = new LPARAM(lParamValue);
                
                // 使用PostMessage发送按键消息
                boolean result = user32.PostMessage(minecraftWindow, WM_KEYDOWN, wParam, lParam);
                
                if (!result) {
                    sendDebugMessage("Failed to post WM_KEYDOWN for key " + keyNames.get(i) + 
                                   " (code: " + vkCode + "): " + Native.getLastError());
                    allPostMessagesSucceeded = false;
                } else {
                    sendDebugMessage("Successfully posted WM_KEYDOWN for key " + keyNames.get(i));
                }
                
                Thread.sleep(20); // 短暂延迟确保按键被正确处理
            }
            
            // 如果PostMessage有任何失败，尝试使用SendInput方法
            if (!allPostMessagesSucceeded) {
                sendDebugMessage("Some PostMessage calls failed. Trying SendInput method instead.");
                return executeRobotPressWithSendInput(virtualKeyCodes, keyNames, duration, lineNumber);
            }
            
            // 等待指定时间
            sendDebugMessage("Waiting for " + duration + " ms with keys pressed");
            Thread.sleep(duration);
            
            // 以相反顺序释放所有键（通常控制键应最后释放）
            for (int i = virtualKeyCodes.size() - 1; i >= 0; i--) {
                int vkCode = virtualKeyCodes.get(i);
                boolean isExtended = isExtendedKey(vkCode);
                
                // 创建消息参数
                WPARAM wParam = new WPARAM(vkCode);
                // lParam格式: 0-15位是repeat count, 16-23位是scan code, 24位是extended flag, 
                // 25-28位保留, 29位是context code, 30位是previous key state, 31位是transition state
                int lParamValue = 0x00000001; // repeat count = 1
                if (isExtended) {
                    lParamValue |= 0x01000000; // extended key flag
                }
                lParamValue |= 0xC0000000; // previous key state + transition state
                LPARAM lParam = new LPARAM(lParamValue);
                
                // 使用PostMessage发送键释放消息
                boolean result = user32.PostMessage(minecraftWindow, WM_KEYUP, wParam, lParam);
                
                if (!result) {
                    sendDebugMessage("Failed to post WM_KEYUP for key " + keyNames.get(i) + 
                                   " (code: " + vkCode + "): " + Native.getLastError());
                } else {
                    sendDebugMessage("Successfully posted WM_KEYUP for key " + keyNames.get(i));
                }
                
                Thread.sleep(20); // 短暂延迟确保释放被正确处理
            }
            
            sendMessage("Background key operation completed successfully");
            return true;
            
        } catch (Exception e) {
            sendError("Line " + lineNumber + ": Error sending keys: " + e.getMessage());
            e.printStackTrace();
            
            // 尝试备用方法
            try {
                sendDebugMessage("Exception occurred. Trying alternative SendInput method.");
                return executeRobotPressWithSendInput(virtualKeyCodes, keyNames, duration, lineNumber);
            } catch (Exception ex) {
                sendError("Line " + lineNumber + ": Error in alternative method: " + ex.getMessage());
                // 最后尝试使用Robot方法
                return executeRobotPressFallback(args, lineNumber);
            }
        }
    }
    
    /**
     * 执行原始的Robot按键模拟(作为后备方案)
     */
    private boolean executeRobotPressFallback(String[] args, int lineNumber) {
        if (args.length < 2) {
            sendError("Line " + lineNumber + ": 'rpress' command requires at least 2 arguments: key duration (or multiple keys followed by duration)");
            return false;
        }
        
        // 最后一个参数应该是持续时间
        int duration;
        try {
            duration = Integer.parseInt(args[args.length - 1]);
            if (duration < 0) {
                sendError("Line " + lineNumber + ": Duration must be a positive integer");
                return false;
            }
        } catch (NumberFormatException e) {
            sendError("Line " + lineNumber + ": Invalid duration: " + args[args.length - 1] + ". Last argument must be an integer duration.");
            return false;
        }
        
        // 收集所有按键的键码
        List<Integer> keyCodes = new ArrayList<>();
        for (int i = 0; i < args.length - 1; i++) {
            String keyName = args[i].toUpperCase();
            Integer keyCode = KEY_MAP.get(keyName);
            if (keyCode == null) {
                sendError("Line " + lineNumber + ": Unknown key: " + keyName);
                return false;
            }
            keyCodes.add(keyCode);
        }
        
        try {
            sendMessage("Falling back to Robot method for key simulation");
            Robot robot = new Robot();
            
            // 按下所有键
            for (Integer keyCode : keyCodes) {
                robot.keyPress(keyCode);
                sendDebugMessage("Pressed key: " + keyCode);
            }
            
            // 等待指定时间
            Thread.sleep(duration);
            
            // 以相反顺序释放所有键（通常控制键应最后释放）
            for (int i = keyCodes.size() - 1; i >= 0; i--) {
                robot.keyRelease(keyCodes.get(i));
                sendDebugMessage("Released key: " + keyCodes.get(i));
            }
            
            sendMessage("Key operation completed using Robot fallback method");
            return true;
        } catch (AWTException e) {
            sendError("Line " + lineNumber + ": Could not create Robot instance: " + e.getMessage());
            return false;
        } catch (InterruptedException e) {
            sendError("Line " + lineNumber + ": Interrupted while waiting: " + e.getMessage());
            Thread.currentThread().interrupt();
            return false;
        }
    }
    
    /**
     * 备用方法：使用SendInput API发送按键
     * 当PostMessage方法失败时调用此方法
     */
    private boolean executeRobotPressWithSendInput(List<Integer> virtualKeyCodes, List<String> keyNames, int duration, int lineNumber) {
        try {
            // 获取User32实例
            User32Ex user32 = User32Ex.INSTANCE;
            
            sendDebugMessage("Using SendInput API as alternative method");
            
            // 按下所有键
            for (int i = 0; i < virtualKeyCodes.size(); i++) {
                int vkCode = virtualKeyCodes.get(i);
                boolean isExtended = isExtendedKey(vkCode);
                
                // 创建INPUT结构
                INPUT input = new INPUT();
                input.type = new DWORD(INPUT_KEYBOARD);
                input.input.ki.wVk = new WORD(vkCode);
                input.input.ki.wScan = new WORD(0);
                input.input.ki.time = new DWORD(0);
                input.input.ki.dwFlags = new DWORD(isExtended ? KEYEVENTF_EXTENDEDKEY : 0);
                // dwExtraInfo应为ULONG_PTR类型
                input.input.ki.dwExtraInfo = new com.sun.jna.platform.win32.BaseTSD.ULONG_PTR(0);
                
                // 使用SendInput发送按键按下事件
                INPUT[] inputs = new INPUT[1];
                inputs[0] = input;
                int result = user32.SendInput(1, inputs, input.size());
                
                if (result != 1) {
                    sendDebugMessage("Failed to send key down for " + keyNames.get(i) + ": " + Native.getLastError());
                } else {
                    sendDebugMessage("Successfully sent key down for " + keyNames.get(i));
                }
                
                Thread.sleep(20); // 短暂延迟
            }
            
            // 等待指定的持续时间
            sendDebugMessage("Waiting for " + duration + " ms with keys pressed");
            Thread.sleep(duration);
            
            // 以相反顺序释放所有键
            for (int i = virtualKeyCodes.size() - 1; i >= 0; i--) {
                int vkCode = virtualKeyCodes.get(i);
                boolean isExtended = isExtendedKey(vkCode);
                
                // 创建INPUT结构
                INPUT input = new INPUT();
                input.type = new DWORD(INPUT_KEYBOARD);
                input.input.ki.wVk = new WORD(vkCode);
                input.input.ki.wScan = new WORD(0);
                input.input.ki.time = new DWORD(0);
                input.input.ki.dwFlags = new DWORD(KEYEVENTF_KEYUP | (isExtended ? KEYEVENTF_EXTENDEDKEY : 0));
                // dwExtraInfo应为ULONG_PTR类型
                input.input.ki.dwExtraInfo = new com.sun.jna.platform.win32.BaseTSD.ULONG_PTR(0);
                
                // 使用SendInput发送按键释放事件
                INPUT[] inputs = new INPUT[1];
                inputs[0] = input;
                int result = user32.SendInput(1, inputs, input.size());
                
                if (result != 1) {
                    sendDebugMessage("Failed to send key up for " + keyNames.get(i) + ": " + Native.getLastError());
                } else {
                    sendDebugMessage("Successfully sent key up for " + keyNames.get(i));
                }
                
                Thread.sleep(20); // 短暂延迟
            }
            
            sendMessage("Background key operation completed successfully using SendInput API");
            return true;
            
        } catch (Exception e) {
            sendError("Line " + lineNumber + ": Error using SendInput API: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }
    
    /**
     * 在箱子GUI中通过发送数据包点击指定位置的物品
     * 支持的箱子GUI大小: 9x6, 9x5, 9x4, 9x3, 9x2, 9x1, 5x1
     * 支持两种指令格式：
     * 1. clickgui [slotId] - 直接指定槽位索引（从0开始）
     * 2. clickgui [row] [col] - 指定行列坐标（从1开始）
     * @param args 参数数组
     * @param lineNumber 当前行号，用于错误报告
     * @return 执行是否成功
     */
    private boolean executeClickGui(String[] args, int lineNumber) {
        try {
            // 检查参数数量
            if (args.length < 1 || args.length > 2) {
                sendError("Line " + lineNumber + ": clickgui command requires either 1 parameter (slot number) or 2 parameters (row and column)");
                return false;
            }
            
            // 确保有打开的GUI
            if (mc.currentScreen == null || !(mc.currentScreen instanceof net.minecraft.client.gui.inventory.GuiContainer)) {
                sendError("Line " + lineNumber + ": No container GUI is currently open");
                return false;
            }
            
            // 获取当前容器
            net.minecraft.client.gui.inventory.GuiContainer guiContainer = (net.minecraft.client.gui.inventory.GuiContainer)mc.currentScreen;
            net.minecraft.inventory.Container container = ((net.minecraft.client.gui.inventory.GuiContainer)mc.currentScreen).inventorySlots;
            
            // 获取容器槽位信息
            int containerSize = container.inventorySlots.size();
            int playerInventorySize = 36; // 玩家物品栏大小（包括快捷栏）
            
            // 标准容器槽位（不包括玩家背包）
            int containerSlots = 0;
            
            // 获取实际容器槽位数，处理特殊情况
            for (int i = 0; i < containerSize; i++) {
                net.minecraft.inventory.Slot slot = container.getSlot(i);
                // 玩家物品栏通常有特定的inventory对象（playerInventory）
                if (!(slot.inventory instanceof net.minecraft.entity.player.InventoryPlayer)) {
                    containerSlots++;
                }
            }
            
            // 根据容器槽位数确定行列数
            int rows;
            int cols;
            
            // 根据总槽位数推断容器大小
            if (containerSlots <= 5) {
                // 5x1 小容器
                cols = containerSlots; // 可能是5或更少
                rows = 1;
            } else {
                // 标准箱子类型容器，列数总是9
                cols = 9;
                rows = containerSlots / cols;
                if (containerSlots % cols != 0) {
                    rows++; // 处理非标准容器
                }
            }
            
            int slotId;
            
            // 解析参数 - 支持两种格式
            if (args.length == 1) {
                // 单一参数模式：槽位索引（从0开始）
                try {
                    slotId = Integer.parseInt(args[0]);
                } catch (NumberFormatException e) {
                    sendError("Line " + lineNumber + ": Invalid slot number: " + args[0]);
                    return false;
                }
            } else {
                // 双参数模式：行列坐标（从1开始）
                try {
                    int row = Integer.parseInt(args[0]);
                    int col = Integer.parseInt(args[1]);
                    
                    // 检查行列范围
                    if (row < 1 || row > rows) {
                        sendError("Line " + lineNumber + ": Row " + row + " is out of range (1-" + rows + ")");
                        return false;
                    }
                    
                    if (col < 1 || col > cols) {
                        sendError("Line " + lineNumber + ": Column " + col + " is out of range (1-" + cols + ")");
                    return false;
                    }
                    
                    // 将从1开始的行列转换为从0开始的索引
                    // 例如：2行3列（用户输入）对应索引 (2-1)*cols + (3-1) = 1*9 + 2 = 11
                    slotId = (row - 1) * cols + (col - 1);
                    
                } catch (NumberFormatException e) {
                    sendError("Line " + lineNumber + ": Invalid row or column number");
                    return false;
                }
            }
            
            // 验证槽位ID是否有效
            if (slotId < 0 || slotId >= containerSlots) {
                sendError("Line " + lineNumber + ": Slot number " + slotId + " is out of range for current container (" + rows + "x" + cols + ")");
                return false;
            }
            
            // 获取实际槽位索引
            int slotIndex = -1;
            int containerSlotCount = 0;
            
            // 遍历所有槽位，找到对应容器中的槽位
            for (int i = 0; i < containerSize; i++) {
                net.minecraft.inventory.Slot slot = container.getSlot(i);
                if (!(slot.inventory instanceof net.minecraft.entity.player.InventoryPlayer)) {
                    if (containerSlotCount == slotId) {
                        slotIndex = i;
                        break;
                    }
                    containerSlotCount++;
                }
            }
            
            // 确保找到了有效的槽位
            if (slotIndex == -1) {
                sendError("Line " + lineNumber + ": Could not find a valid slot at index " + slotId);
                return false;
            }
            
            final CountDownLatch latch = new CountDownLatch(1);
            final int finalSlotIndex = slotIndex;
            final int finalSlotId = slotId;
            final int finalRows = rows;
            final int finalCols = cols;
            
            // 计算并显示行列信息
            final int row = (slotId / cols) + 1;
            final int col = (slotId % cols) + 1;
            
            // 在主线程执行点击操作
            mc.addScheduledTask(() -> {
                try {
                    // 获取容器窗口ID
                    int windowId = container.windowId;
                    
                    // 通过PlayerControllerMP发送点击数据包
                    // 参数：windowId, slotId, mouseButton(0=左键), clickType(0=普通点击), player
                    mc.playerController.windowClick(
                        windowId,
                        finalSlotIndex,
                        0, // 左键点击
                        0, // 普通点击
                        mc.thePlayer
                    );
                    
                    sendMessage("Clicked on slot " + finalSlotId + " (row " + row + ", column " + col + ") (internal index: " + finalSlotIndex + ") in container (" + finalRows + "x" + finalCols + ")");
                } finally {
                    latch.countDown();
                }
            });
            
            // 等待操作完成
            latch.await(500, TimeUnit.MILLISECONDS);
            return true;
            
        } catch (Exception e) {
            sendError("Line " + lineNumber + ": Error executing clickgui command: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }
    
    /**
     * 宏指令执行器接口
     */
    @FunctionalInterface
    private interface MacroAction {
        boolean execute(String[] args, int lineNumber) throws Exception;
    }
} 