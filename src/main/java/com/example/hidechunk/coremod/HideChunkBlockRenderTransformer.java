package com.example.hidechunk.coremod;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

import net.minecraft.launchwrapper.IClassTransformer;

/**
 * Injects an early exit into {@code BlockRendererDispatcher.renderBlock} (SRG {@code func_175018_a}) so hidden columns emit no geometry during chunk mesh rebuild.
 */
public final class HideChunkBlockRenderTransformer implements IClassTransformer {

    private static final String TARGET_CLASS = "net.minecraft.client.renderer.BlockRendererDispatcher";
    /** SRG — stable in obfuscated 1.12.2 + Forge. */
    private static final String RENDER_BLOCK_NAME = "func_175018_a";
    private static final String RENDER_BLOCK_DESC =
            "(Lnet/minecraft/block/state/IBlockState;Lnet/minecraft/util/math/BlockPos;Lnet/minecraft/world/IBlockAccess;Lnet/minecraft/client/renderer/BufferBuilder;)Z";

    private static final String HOOK_INTERNAL = "com/example/hidechunk/client/HideChunkBlockRenderHook";
    private static final String HOOK_METHOD = "shouldSkipBlockMesh";
    private static final String HOOK_DESC =
            "(Lnet/minecraft/world/IBlockAccess;Lnet/minecraft/util/math/BlockPos;)Z";

    @Override
    public byte[] transform(String name, String transformedName, byte[] basicClass) {
        if (basicClass == null || !TARGET_CLASS.equals(transformedName)) {
            return basicClass;
        }
        ClassReader cr = new ClassReader(basicClass);
        // Recompute StackMapTable: injected branches invalidate vanilla frames; COMPUTE_MAXS alone triggers VerifyError on JRE 8.
        ClassWriter cw = new ClassWriter(cr, ClassWriter.COMPUTE_MAXS | ClassWriter.COMPUTE_FRAMES) {
            @Override
            protected String getCommonSuperClass(String type1, String type2) {
                // Avoid loading game types from the transformer ClassLoader during frame analysis.
                return "java/lang/Object";
            }
        };
        ClassVisitor adapter = new ClassVisitor(Opcodes.ASM5, cw) {
            @Override
            public MethodVisitor visitMethod(
                    int access, String methodName, String desc, String signature, String[] exceptions) {
                MethodVisitor mv = super.visitMethod(access, methodName, desc, signature, exceptions);
                if (RENDER_BLOCK_NAME.equals(methodName) && RENDER_BLOCK_DESC.equals(desc)) {
                    return new MethodVisitor(Opcodes.ASM5, mv) {
                        @Override
                        public void visitCode() {
                            super.visitCode();
                            // renderBlock(IBlockState state, BlockPos pos, IBlockAccess blockAccess, BufferBuilder buffer)
                            mv.visitVarInsn(Opcodes.ALOAD, 3);
                            mv.visitVarInsn(Opcodes.ALOAD, 2);
                            mv.visitMethodInsn(
                                    Opcodes.INVOKESTATIC,
                                    HOOK_INTERNAL,
                                    HOOK_METHOD,
                                    HOOK_DESC,
                                    false);
                            Label continueNormal = new Label();
                            mv.visitJumpInsn(Opcodes.IFEQ, continueNormal);
                            mv.visitInsn(Opcodes.ICONST_0);
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
