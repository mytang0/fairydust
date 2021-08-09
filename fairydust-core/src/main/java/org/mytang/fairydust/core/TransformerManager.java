package org.mytang.fairydust.core;

import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.Instrumentation;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * @author tangmengyang
 */
public class TransformerManager {

    private Instrumentation instrumentation;
    private ClassFileTransformer classFileTransformer;

    private List<ClassFileTransformer> transformers = new CopyOnWriteArrayList<>();

    public TransformerManager(Instrumentation instrumentation) {
        this.instrumentation = instrumentation;

        classFileTransformer = (loader, className, classBeingRedefined, protectionDomain, classfileBuffer) -> {

            for (ClassFileTransformer classFileTransformer : transformers) {
                byte[] transformResult = classFileTransformer.transform(loader, className, classBeingRedefined,
                    protectionDomain, classfileBuffer);
                if (transformResult != null) {
                    classfileBuffer = transformResult;
                }
            }
            return classfileBuffer;
        };

        this.instrumentation.addTransformer(classFileTransformer, true);
    }

    public void addTransformer(ClassFileTransformer transformer) {
        transformers.add(transformer);
    }

    public void removeTransformer(ClassFileTransformer transformer) {
        transformers.remove(transformer);
    }

    public void destroy() {
        transformers.clear();
        instrumentation.removeTransformer(classFileTransformer);
    }
}
