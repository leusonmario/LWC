/*
 * Copyright (c) 2011-2013 Tyler Blair
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without modification, are
 * permitted provided that the following conditions are met:
 *
 *    1. Redistributions of source code must retain the above copyright notice, this list of
 *       conditions and the following disclaimer.
 *
 *    2. Redistributions in binary form must reproduce the above copyright notice, this list
 *       of conditions and the following disclaimer in the documentation and/or other materials
 *       provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE AUTHOR ''AS IS'' AND ANY EXPRESS OR IMPLIED
 * WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND
 * FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE AUTHOR OR
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR,
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON
 * ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
 * NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF
 * ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 *
 * The views and conclusions contained in the software and documentation are those of the
 * authors and contributors and should not be interpreted as representing official policies,
 * either expressed or implied, of anybody else.
 */

package org.getlwc.forge.asm;

import cpw.mods.fml.relauncher.FMLInjectionData;
import net.minecraft.launchwrapper.IClassTransformer;
import org.getlwc.SimpleEngine;
import org.getlwc.forge.asm.mappings.MappedClass;
import org.getlwc.forge.util.asm.MappingLoader;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

public abstract class AbstractTransformer implements IClassTransformer {

    /**
     * Mappings of all classes
     */
    protected static final Map<String, MappedClass> mappings = new HashMap<>();

    /**
     * If mappings have been loaded
     */
    private static boolean mappingsLoaded = false;

    /**
     * Transform a class
     *
     * @param name
     * @param transformedName
     * @param bytes
     * @return
     */
    @Override
    public abstract byte[] transform(String name, String transformedName, byte[] bytes);

    /**
     * Init and load the mappings file if it has not yet been loaded
     */
    public static void init() {
        if (!mappingsLoaded) {
            String minecraftVersion = (String) FMLInjectionData.data()[4];
            InputStream stream = AbstractTransformer.class.getResourceAsStream("/mappings/" + minecraftVersion + ".json");

            if (stream != null) {
                SimpleEngine.getInstance().getConsoleSender().sendMessage("[ASM] Loaded native class mappings for Minecraft {0}", minecraftVersion);
            } else {
                stream = AbstractTransformer.class.getResourceAsStream("/mappings/latest.json");
                SimpleEngine.getInstance().getConsoleSender().sendMessage("[ASM] ================   NOTE !!!   ================\n"
                        + "[ASM] There are no included native mappings for Minecraft {0}\n"
                        + "[ASM] If this is a major Minecraft release then LWC IS MOST LIKELY BROKEN!\n"
                        + "[ASM] MAKE SURE YOU ARE USING THE LATEST VERSION!", minecraftVersion);
            }

            try {
                for (MappedClass clazz : MappingLoader.loadClasses(stream)) {
                    mappings.put(clazz.getSimpleName(), clazz);
                }
            } catch (IOException e) {
                e.printStackTrace();
                SimpleEngine.getInstance().getConsoleSender().sendMessage("[ASM] Failed to load mappings!");
            }

            mappingsLoaded = true;
        }
    }

}
