package org.getlwc.forge.asm;

import org.getlwc.SimpleEngine;
import org.getlwc.forge.asm.mappings.MappedClass;
import org.getlwc.forge.asm.mappings.MappedField;
import org.getlwc.forge.asm.mappings.MappedMethod;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.LocalVariableNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;

import java.util.HashMap;
import java.util.Map;

public abstract class AbstractMultiClassTransformer extends AbstractTransformer {

    public static final Map<Class<? extends AbstractTransformer>, TransformerStatus> TRANSFORMER_STATUSES = new HashMap<>();

    /**
     * The class name (simple, not canonical / full) of the desired class we want to transform
     */
    private final String[] classNames;

    /**
     * If the classes are detected to be obfuscated or not
     */
    private boolean obfuscated = false;

    /**
     * The class node we are operating on
     */
    private ClassNode classNode = new ClassNode();

    /**
     * The list of instructions to be injected into a method
     */
    private InsnList instructions = new InsnList();

    /**
     * The method we are currently visiting
     */
    private MethodNode currentMethod = null;

    /**
     * The class we are currently rewriting
     */
    protected String targetClass = null;

    /**
     * Flag for if the class has been changed (if true, injections will be made)
     */
    private boolean changed = false;

    public AbstractMultiClassTransformer(String[] classNames) {
        this.classNames = classNames;
    }

    @Override
    public byte[] transform(String name, String transformedName, byte[] bytes) {
        // No LWC classes are going to be transformed, so ignore all of them.
        // This sidesteps the issue where this transformer is called on
        // AbstractMultiClassTransformer.class which causes some issues.
        if (name.startsWith("org.getlwc.forge.asm")) {
            return bytes;
        }

        boolean transformed = false;
        targetClass = null;

        try {
            for (String className : classNames) {
                if (name.equals(getClassName(className, false))) {
                    obfuscated = false;
                    targetClass = className;
                } else if (name.equals(getClassName(className, true))) {
                    obfuscated = true;
                    targetClass = className;
                }

                if (targetClass != null) {
                    instructions = new InsnList();
                    classNode = new ClassNode();

                    ClassReader reader = new ClassReader(bytes);
                    reader.accept(classNode, 0);

                    transform();
                    transformed = true;
                    break;
                }
            }

            if (transformed) {
                if (changed) {
                    ClassWriter writer = new ClassWriter(ClassWriter.COMPUTE_MAXS);
                    classNode.accept(writer);
                    SimpleEngine.getInstance().getConsoleSender().sendMessage("[ASM] Patched {0} ({1}) successfully!", getClass().getSimpleName() + "::" + targetClass, getClassName(targetClass));
                    TRANSFORMER_STATUSES.put(getClass(), TransformerStatus.SUCCESSFUL);
                    return writer.toByteArray();
                } else {
                    SimpleEngine.getInstance().getConsoleSender().sendMessage("[ASM] {0} ({1}) was not changed during transformations", getClass().getSimpleName() + "::" + targetClass, getClassName(targetClass));
                    TRANSFORMER_STATUSES.put(getClass(), TransformerStatus.FAILED);
                    return bytes;
                }
            }
        } catch (Exception e) {
            SimpleEngine.getInstance().getConsoleSender().sendMessage("[ASM] Failed to patch {0} ({1})", getClass().getSimpleName() + "::" + targetClass, getClassName(targetClass));
            TRANSFORMER_STATUSES.put(getClass(), TransformerStatus.FAILED);
            e.printStackTrace();
            return bytes;
        }

        return bytes;
    }

    /**
     * Transform the given matched class
     *
     * @return
     */
    public abstract void transform();

    /**
     * Visit a method and allow writing starting at the beginning of the method
     *
     * @param methodName
     */
    public boolean visitMethod(String methodName) {
        for (Object object : classNode.methods) {
            MethodNode method = (MethodNode) object;

            if (methodEquals(method, targetClass, methodName)) {
                currentMethod = method;
                return true;
            }
        }

        return false;
    }

    /**
     * Add an instruction to the list of instructions
     *
     * @param insn
     */
    public void addInstruction(AbstractInsnNode insn) {
        instructions.add(insn);
    }

    /**
     * Finds the index of the given local variable using its class type
     *
     * @param className the name of the class of the variable's type
     * @return the index of the variable if found otherwise -1
     */
    public int findMethodLocalVariable(String className) {
        for (Object o : currentMethod.localVariables) {
            LocalVariableNode var = (LocalVariableNode) o;

            if (variableMatchesClass(var, className)) {
                return var.index;
            }
        }

        return -1;
    }

    /**
     * Finds the index of a method call in the method
     *
     * @param className
     * @param methodName
     * @return the index of the method call otherwise -1
     */
    public int findMethodCall(String className, String methodName) {
        for (int index = 0; index < currentMethod.instructions.size(); index++) {
            if (currentMethod.instructions.get(index).getType() == AbstractInsnNode.METHOD_INSN) {
                MethodInsnNode node = (MethodInsnNode) currentMethod.instructions.get(index);

                if (node.owner.equals(className) && node.name.equals(methodName)) {
                    return index;
                }

            }
        }

        return -1;
    }

    /**
     * Finds the first instance of the given opcode in the function
     *
     * @param opcode
     * @return the offset in the function the opcode is located at otherwise -1 if not found
     */
    public int findMethodOpcode(int opcode) {
        int offset = 0;

        if (getMethodOpcode(offset) == opcode) {
            return offset;
        }

        int max = currentMethod.instructions.size() - 1;
        while (offset <= max && getMethodOpcode(offset) != opcode) {
            offset++;
        }

        return offset == 0 ? -1 : offset;
    }

    /**
     * Finds the last instance of the given opcode in the function
     *
     * @param opcode
     * @return the offset in the function the opcode is located at otherwise -1 if not found
     */
    public int findMethodLastOpcode(int opcode) {
        int offset = currentMethod.instructions.size() - 1;

        if (getMethodOpcode(offset) == opcode) {
            return offset;
        }

        while (offset >= 0 && getMethodOpcode(offset) != opcode) {
            offset--;
        }

        return offset == 0 ? -1 : offset;
    }

    /**
     * Inject the instruction list to the beginning of the method
     */
    public void injectMethod() {
        currentMethod.instructions.insert(instructions);
        changed = true;
    }

    /**
     * Inject the instruction list at the given offset in the method
     *
     * @param offset
     */
    public void injectMethod(int offset) {
        currentMethod.instructions.insert(currentMethod.instructions.get(offset), instructions);
        changed = true;
    }

    /**
     * Inject the instruction list before the given offset in the method
     *
     * @param offset
     */
    public void injectMethodBefore(int offset) {
        currentMethod.instructions.insertBefore(currentMethod.instructions.get(offset), instructions);
        changed = true;
    }

    /**
     * Get the opcode at the given offset in the method
     *
     * @param offset
     * @return
     */
    public int getMethodOpcode(int offset) {
        return currentMethod.instructions.get(offset).getOpcode();
    }

    /**
     * Check if a given {@link org.objectweb.asm.tree.MethodNode} equals the method in the given method
     *
     * @param method
     * @param className
     * @param methodName
     * @return
     */
    public boolean methodEquals(MethodNode method, String className, String methodName) {
        return method.desc.equals(getMethodSignature(className, methodName)) && method.name.equals(getMethodName(className, methodName));
    }

    /**
     * Check if the variable matches the given class
     *
     * @param variable
     * @param className
     * @return
     */
    public boolean variableMatchesClass(LocalVariableNode variable, String className) {
        String signature = "L" + getJavaClassName(className) + ";";

        return variable.desc.equals(signature);
    }

    /**
     * Get a class name
     *
     * @param className
     */
    public String getClassName(String className) {
        return getClassName(className, obfuscated);
    }

    /**
     * Get a class name
     *
     * @param className
     * @param obfuscated
     * @return
     */
    public static String getClassName(String className, boolean obfuscated) {
        if ("ForgeEventHelper".equals(className)) {
            return "org.getlwc.forge.ForgeEventHelper";
        }

        MappedClass clazz = mappings.get(className);

        if (clazz == null) {
            if (mappings.size() > 0) {
                SimpleEngine.getInstance().getConsoleSender().sendMessage("Class not found: {0}", className);
            }

            return null;
        }

        if (obfuscated) {
            return clazz.getObfuscatedName();
        } else {
            return clazz.getCanonicalName();
        }
    }

    /**
     * Get the Java bytecode class name for the given class (replaces . with /)
     *
     * @param className
     * @return
     */
    public String getJavaClassName(String className) {
        return getJavaClassName(className, obfuscated);
    }

    /**
     * Get the Java bytecode class name for the given class (replaces . with /)
     *
     * @param className
     * @param obfuscated
     * @return
     */
    public static String getJavaClassName(String className, boolean obfuscated) {
        return getClassName(className, obfuscated).replaceAll("\\.", "/");
    }

    /**
     * Get the method name to use in the given class
     *
     * @param className
     * @param methodName
     * @return
     */
    public String getMethodName(String className, String methodName) {
        return getMethodName(className, methodName, obfuscated);
    }

    /**
     * Get the method name to use in the given class
     *
     * @param className
     * @param methodName
     * @param obfuscated
     * @return
     */
    public static String getMethodName(String className, String methodName, boolean obfuscated) {
        return getMethodName(className, methodName, obfuscated ? CompilationType.OBFUSCATED : CompilationType.UNOBFUSCATED);
    }

    /**
     * Get the method name to use in the given class
     *
     * @param className
     * @param methodName
     * @param type
     * @return
     */
    public static String getMethodName(String className, String methodName, CompilationType type) {
        if ("ForgeEventHelper".equals(className)) {
            return methodName;
        }

        MappedClass clazz = mappings.get(className);

        if (clazz == null) {
            return null;
        }

        MappedMethod method = clazz.getMethod(methodName);

        if (method == null) {
            SimpleEngine.getInstance().getConsoleSender().sendMessage("Method not found: {0}/{1}", clazz.getCanonicalName(), methodName);
            return null;
        }

        switch (type) {
            case UNOBFUSCATED:
                return method.getName();
            case OBFUSCATED:
                return method.getObfuscatedName();
            case SRG:
                return method.getSrgName();
            default:
                throw new UnsupportedClassVersionError("Unknown CompilationType " + type);
        }
    }

    /**
     * Get the signature of a method
     *
     * @param className
     * @param methodName
     * @return
     */
    public String getMethodSignature(String className, String methodName) {
        return getMethodSignature(className, methodName, obfuscated);
    }

    /**
     * Get the signature of a method
     *
     * @param className
     * @param methodName
     * @param obfuscated
     * @return
     */
    public static String getMethodSignature(String className, String methodName, boolean obfuscated) {
        MappedClass clazz = mappings.get(className);

        if (clazz == null) {
            return null;
        }

        MappedMethod method = clazz.getMethod(methodName);

        if (method == null) {
            SimpleEngine.getInstance().getConsoleSender().sendMessage("Method not found: {0}/{1}", clazz.getCanonicalName(), methodName);
            return null;
        }

        if (obfuscated) {
            return method.getObfuscatedSignature();
        } else {
            return method.getSrgSignature();
        }
    }

    /**
     * Get the field name to use in the given class
     *
     * @param className
     * @param fieldName
     * @return
     */
    public String getFieldName(String className, String fieldName) {
        return getFieldName(className, fieldName, CompilationType.OBFUSCATED);
    }

    /**
     * Get the field name to use in the given class
     *
     * @param className
     * @param fieldName
     * @param type
     * @return
     */
    public static String getFieldName(String className, String fieldName, CompilationType type) {
        if ("ForgeEventHelper".equals(className)) {
            return fieldName;
        }

        MappedClass clazz = mappings.get(className);

        if (clazz == null) {
            return null;
        }

        MappedField field = clazz.getField(fieldName);

        if (field == null) {
            SimpleEngine.getInstance().getConsoleSender().sendMessage("Field not found: {0}/{1}", clazz.getCanonicalName(), fieldName);
            return null;
        }

        switch (type) {
            case UNOBFUSCATED:
                return field.getName();
            case OBFUSCATED:
                return field.getObfuscatedName();
            case SRG:
                return field.getSrgName();
            default:
                throw new UnsupportedClassVersionError("Unknown CompilationType " + type);
        }
    }

}
