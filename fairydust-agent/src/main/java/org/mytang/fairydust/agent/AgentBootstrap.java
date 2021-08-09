package org.mytang.fairydust.agent;

import java.io.File;
import java.io.PrintStream;
import java.io.UnsupportedEncodingException;
import java.lang.instrument.Instrumentation;
import java.net.URL;
import java.net.URLDecoder;
import java.security.CodeSource;

/**
 * @author tangmengyang
 */
public class AgentBootstrap {

    private static final String FAIRY_DUST_BOOTSTRAP = "org.mytang.fairydust.core.FairyDustBootstrap";
    private static final String GET_INSTANCE = "getInstance";

    private static PrintStream ps = System.err;

    private static volatile ClassLoader fairyDustClassLoader;

    public static void premain(String args, Instrumentation inst) {
        main(args, inst);
    }

    public static void agentmain(String args, Instrumentation inst) {
        main(args, inst);
    }

    private static synchronized void main(String args, final Instrumentation inst) {
        try {
            ps.println("fairydust agent start...");

            args = decodeArg(args);

            File jarFile = null;

            CodeSource codeSource = AgentBootstrap.class.getProtectionDomain().getCodeSource();
            if (codeSource != null) {
                try {
                    jarFile = new File(codeSource.getLocation().toURI().getSchemeSpecificPart());
                    if (!jarFile.exists()) {
                        ps.println("Can not find fairydust-agent jar file from agent jar directory: " + jarFile);
                    }
                } catch (Throwable e) {
                    ps.println("Can not find fairydust-agent jar file from " + codeSource.getLocation());
                    e.printStackTrace(ps);
                }
            }

            if (jarFile == null || !jarFile.exists()) {
                return;
            }

            start(inst, getClassLoader(jarFile), args);

        } catch (Throwable t) {
            t.printStackTrace(ps);
            try {
                if (ps != System.err) {
                    ps.close();
                }
            } catch (Throwable ignored) {
            }
            throw new RuntimeException(t);
        }
    }

    private static void start(Instrumentation inst, ClassLoader classLoader, String args)
        throws Throwable {

        Class<?> bootstrapClass = classLoader.loadClass(FAIRY_DUST_BOOTSTRAP);

        bootstrapClass.getMethod(GET_INSTANCE, Instrumentation.class, String.class)
            .invoke(null, inst, args);

        ps.println("fairydust agent already started.");
    }

    private static ClassLoader getClassLoader(File jarFile) throws Throwable {
        if (fairyDustClassLoader == null) {
            fairyDustClassLoader = new FairyDustClassLoader(new URL[] {jarFile.toURI().toURL()});
        }
        return fairyDustClassLoader;
    }

    private static String decodeArg(String arg) {
        try {
            return URLDecoder.decode(arg, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            return arg;
        }
    }
}
