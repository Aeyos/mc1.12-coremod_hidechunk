package com.example.hidechunk.coremod;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

import net.minecraft.launchwrapper.IClassTransformer;

/**
 * Intercepts F3+S to only switch the audio output device (by reloading the sound system),
 * without triggering full resource reload behavior.
 */
public final class HideChunkF3AudioSwitchTransformer implements IClassTransformer {

    private static final String TARGET_CLASS = "net.minecraft.client.Minecraft";

    /**
     * SRG name for Minecraft#processKeyF3(int) in 1.12.2.
     * Descriptor: (I)Z
     */
    private static final String PROCESS_F3_NAME = "func_184122_c";
    private static final String PROCESS_F3_DESC = "(I)Z";

    private static final String HOOK_INTERNAL = "com/example/hidechunk/client/HideChunkF3AudioSwitchHook";
    private static final String HOOK_METHOD = "handleF3Key";
    private static final String HOOK_DESC = "(I)Z";

    @Override
    public byte[] transform(String name, String transformedName, byte[] basicClass) {
        if (basicClass == null || !TARGET_CLASS.equals(transformedName)) {
            return basicClass;
        }

        ClassReader cr = new ClassReader(basicClass);
        ClassWriter cw = new ClassWriter(cr, ClassWriter.COMPUTE_MAXS | ClassWriter.COMPUTE_FRAMES) {
            @Override
            protected String getCommonSuperClass(String type1, String type2) {
                return "java/lang/Object";
            }
        };

        ClassVisitor adapter = new ClassVisitor(Opcodes.ASM5, cw) {
            @Override
            public MethodVisitor visitMethod(
                    int access, String methodName, String desc, String signature, String[] exceptions) {
                MethodVisitor mv = super.visitMethod(access, methodName, desc, signature, exceptions);
                if (PROCESS_F3_NAME.equals(methodName) && PROCESS_F3_DESC.equals(desc)) {
                    return new MethodVisitor(Opcodes.ASM5, mv) {
                        @Override
                        public void visitCode() {
                            super.visitCode();
                            // if (HideChunkF3AudioSwitchHook.handleF3Key(key)) return true;
                            mv.visitVarInsn(Opcodes.ILOAD, 1);
                            mv.visitMethodInsn(
                                    Opcodes.INVOKESTATIC,
                                    HOOK_INTERNAL,
                                    HOOK_METHOD,
                                    HOOK_DESC,
                                    false);
                            Label continueNormal = new Label();
                            mv.visitJumpInsn(Opcodes.IFEQ, continueNormal);
                            mv.visitInsn(Opcodes.ICONST_1);
                            mv.visitInsn(Opcodes.IRETURN);
                            mv.visitLabel(continueNormal);
                        }
                    };
                }
                return mv;
            }
        };

        cr.accept(adapter, ClassReader.EXPAND_FRAMES);
        return cw.toByteArray();
    }
}

