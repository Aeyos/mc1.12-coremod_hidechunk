package com.example.hidechunk.core;

import net.minecraft.launchwrapper.IClassTransformer;
import net.minecraftforge.fml.common.asm.transformers.deobf.FMLDeobfuscatingRemapper;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.*;

public class HideChunkTransformer implements IClassTransformer {

    private static final String TARGET_CLASS = "net.minecraft.client.renderer.chunk.RenderChunk";
    private static final String TARGET_CLASS_INTERNAL = "net/minecraft/client/renderer/chunk/RenderChunk";

    private static final String HOOK_OWNER = "com/example/hidechunk/client/HideChunkRenderHook";
    private static final String HOOK_NAME = "shouldSkipRebuild";
    private static final String HOOK_DESC =
            "(Lnet/minecraft/client/renderer/chunk/RenderChunk;"
                    + "Lnet/minecraft/client/renderer/chunk/ChunkCompileTaskGenerator;)Z";

    @Override
    public byte[] transform(String name, String transformedName, byte[] basicClass) {
        if (basicClass == null) {
            return null;
        }
        if (!(TARGET_CLASS.equals(transformedName) || TARGET_CLASS.equals(name))) {
            return basicClass;
        }

        ClassNode classNode = new ClassNode();
        ClassReader classReader = new ClassReader(basicClass);
        classReader.accept(classNode, 0);

        boolean patched = false;
        String ownerInternal = classNode.name;
        StringBuilder debugCandidates = new StringBuilder();

        for (MethodNode method : classNode.methods) {
            if (isRebuildChunk(ownerInternal, method)) {
                if (isAlreadyPatched(method)) {
                    patched = true;
                } else {
                    injectEarlyReturn(method);
                    patched = true;
                }
                break;
            }

            // Minimal diagnostics to help debug modpack/coremod conflicts.
            if (debugCandidates.length() < 800) {
                debugCandidates.append(method.name).append(method.desc).append('\n');
            }
        }

        if (!patched) {
            String msg =
                    "Failed to patch RenderChunk.rebuildChunk"
                            + " (name=" + name + ", transformedName=" + transformedName + ")"
                            + " candidateMethods:\n" + debugCandidates;
            throw new RuntimeException(msg);
        }

        ClassWriter writer = new ClassWriter(ClassWriter.COMPUTE_MAXS | ClassWriter.COMPUTE_FRAMES);
        classNode.accept(writer);
        return writer.toByteArray();
    }

    /**
     * Must use the internal name from the class bytecode (e.g. {@code bxr} in production),
     * not the deobfuscated {@link #TARGET_CLASS_INTERNAL}, or {@link FMLDeobfuscatingRemapper}
     * will not resolve SRG names for {@code func_178581_b}/{@code rebuildChunk}.
     */
    private boolean isRebuildChunk(String ownerInternal, MethodNode method) {
        String expectedMappedDesc =
                "(FFFLnet/minecraft/client/renderer/chunk/ChunkCompileTaskGenerator;)V";

        FMLDeobfuscatingRemapper remapper = FMLDeobfuscatingRemapper.INSTANCE;
        String mappedDesc = remapper.mapMethodDesc(method.desc);
        if (!expectedMappedDesc.equals(mappedDesc)) {
            return false;
        }

        String mappedName = remapper.mapMethodName(ownerInternal, method.name, method.desc);
        if ("rebuildChunk".equals(mappedName) || "func_178581_b".equals(mappedName)) {
            return true;
        }
        if (!TARGET_CLASS_INTERNAL.equals(ownerInternal)) {
            mappedName = remapper.mapMethodName(TARGET_CLASS_INTERNAL, method.name, method.desc);
            return "rebuildChunk".equals(mappedName) || "func_178581_b".equals(mappedName);
        }
        return false;
    }

    private boolean isAlreadyPatched(MethodNode method) {
        int scanned = 0;
        for (AbstractInsnNode node = method.instructions.getFirst(); node != null && scanned < 40; node = node.getNext(), scanned++) {
            if (node instanceof MethodInsnNode) {
                MethodInsnNode m = (MethodInsnNode) node;
                if (m.getOpcode() == Opcodes.INVOKESTATIC
                        && HOOK_OWNER.equals(m.owner)
                        && HOOK_NAME.equals(m.name)
                        && HOOK_DESC.equals(m.desc)) {
                    return true;
                }
            }
        }
        return false;
    }

    private void injectEarlyReturn(MethodNode method) {
        InsnList insn = new InsnList();

        LabelNode continueLabel = new LabelNode();

        insn.add(new VarInsnNode(Opcodes.ALOAD, 0));
        insn.add(new VarInsnNode(Opcodes.ALOAD, 4));

        insn.add(new MethodInsnNode(
                Opcodes.INVOKESTATIC,
                HOOK_OWNER,
                HOOK_NAME,
                HOOK_DESC,
                false
        ));

        insn.add(new JumpInsnNode(Opcodes.IFEQ, continueLabel));
        insn.add(new InsnNode(Opcodes.RETURN));
        insn.add(continueLabel);

        method.instructions.insert(insn);
    }
}
