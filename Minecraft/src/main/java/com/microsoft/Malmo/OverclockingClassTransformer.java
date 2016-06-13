package com.microsoft.Malmo;

import net.minecraft.launchwrapper.IClassTransformer;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldInsnNode;
import org.objectweb.asm.tree.LdcInsnNode;
import org.objectweb.asm.tree.MethodNode;

public class OverclockingClassTransformer implements IClassTransformer
{
     @Override
    public byte[] transform(String name, String transformedName, byte[] basicClass)
    {
        boolean isObfuscated = !name.equals(transformedName);
        return transformedName.equals("net.minecraft.server.MinecraftServer") ? transform(basicClass, isObfuscated) : basicClass;
    }
    
    private static byte[] transform(byte[] serverClass, boolean isObfuscated)
    {
        System.out.println("MALMO: Attempting to transform MinecraftServer");
        try
        {
            ClassNode cnode = new ClassNode();
            ClassReader creader = new ClassReader(serverClass);
            creader.accept(cnode, 0);
            
            overclock(cnode, isObfuscated);
            
            ClassWriter cwriter = new ClassWriter(ClassWriter.COMPUTE_MAXS | ClassWriter.COMPUTE_FRAMES);
            cnode.accept(cwriter);
            return cwriter.toByteArray();
        }
        catch (Exception e)
        {
            System.out.println("MALMO FAILED to transform MinecraftServer - overclocking not available!");
        }
        return serverClass;
    }
    
    private static void overclock(ClassNode node, boolean isObfuscated)
    {
        // We're attempting to replace this code (from the heart of MinecraftServer.run):
        /*       
            {
                while (i > 50L)
                {
                    i -= 50L;
                    this.tick();
                }
            }
    
            Thread.sleep(Math.max(1L, 50L - i));
        */

        // With this:
        /*       
        {
            while (i > TimeHelper.serverTickLength)
            {
                i -= TimeHelper.serverTickLength;
                this.tick();
            }
        }

        Thread.sleep(Math.max(1L, TimeHelper.serverTickLength - i));
    */
        // This allows us to alter the tick length via TimeHelper.
        
        final String methodName = isObfuscated ? "minecraft" : "run";
        final String methodDescriptor = "()V"; // No params, returns void.

        System.out.println("MALMO: Found MinecraftServer, attempting to transform it");

        for (MethodNode method : node.methods)
        {
            if (method.name.equals(methodName) && method.desc.equals(methodDescriptor))
            {
                System.out.println("MALMO: Found MinecraftServer.run() method, attempting to transform it");
                for (AbstractInsnNode instruction : method.instructions.toArray())
                {
                    if (instruction.getOpcode() == Opcodes.LDC)
                    {
                        Object cst = ((LdcInsnNode)instruction).cst;
                        if ((cst instanceof Long) && (Long)cst == 50)
                        {
                            System.out.println("MALMO: Transforming LDC");
                            AbstractInsnNode replacement = new FieldInsnNode(Opcodes.GETSTATIC, "com/microsoft/Malmo/Utils/TimeHelper", "serverTickLength", "J");
                            method.instructions.set(instruction, replacement);
                        }
                    }
                }
            }
        }
    }
}
