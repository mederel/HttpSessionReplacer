package com.amadeus.session.agent;

import static com.amadeus.session.agent.SessionAgent.*;
import static org.objectweb.asm.ClassWriter.*;
import static org.objectweb.asm.Opcodes.*;

import java.lang.instrument.ClassFileTransformer;
import java.security.ProtectionDomain;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;

/**
 * This class finds <code>javax.servlet.ServletContext</code> and
 * <code>javax.servlet.Filter</code> instances and applies transformations to
 * them. See {@link ServletContextAdapter} and {@link FilterAdapter} for details
 * of transformations. In case when the support for Servlet 2.5 containers is
 * active, HttpSessionListeners and HttpSessionAttributeListeners are also
 * insrumented. See {@link ListenerAdapter}.
 */
class SessionSupportTransformer implements ClassFileTransformer {
  HashSet<String> servletContextClasses = new HashSet<>();

  /**
   * Keeps track of all filter classes found and
   */
  Map<String, Set<String>> classTree = new HashMap<>();

  HashSet<String> listenerClasses = new HashSet<>();

  private final boolean interceptListeners;

  SessionSupportTransformer(boolean interceptListeners) {
    this.interceptListeners = interceptListeners;
  }



  @Override
  public byte[] transform(ClassLoader loader, String className, Class<?> classBeingRedefined,
      ProtectionDomain protectionDomain, byte[] classfileBuffer) {
    ClassReader cr = new ClassReader(classfileBuffer);

    // We assume that class can only one of servlet context, filter or listener
    ServletContextCandidateFinder sh = new ServletContextCandidateFinder();
    cr.accept(sh, 0);
    if (sh.candidate) {
      debug("Transforming ServletContext implementation %s", className);
      ClassWriter cw = new ClassWriter(cr, COMPUTE_FRAMES);
      ClassVisitor cv = new ServletContextAdapter(cw);
      cr.accept(cv, 0);

      return cw.toByteArray();
    }
    return interceptListenersIfNeeded(className, cr);
  }


  byte[] interceptListenersIfNeeded(String className, ClassReader cr) {
    if (interceptListeners) {
      ListenerCandidateFinder listenerFinder = new ListenerCandidateFinder();
      cr.accept(listenerFinder, 0);
      if (listenerFinder.candidate) {
        listenerClasses.add(className);
        debug("Transforming Listener implementation %s", className);
        ClassWriter cw = new ClassWriter(cr, COMPUTE_FRAMES);
        ClassVisitor cv = new ListenerWithInjectCalls(cw);
        cr.accept(cv, 0);
        return cw.toByteArray();
      }
    }
    return null; // NOSONAR Transformer contract requires null if no changes
  }

  /**
   * Utility class to identify if a class is a ServletContext implementation or
   * if it inherits from a ServletContext implementation.
   */
  class ServletContextCandidateFinder extends ClassVisitor {
    private boolean candidate;

    ServletContextCandidateFinder() {
      super(ASM5);
    }

    @Override
    public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
      // First check if super class has already been identified as servlet context
      if (servletContextClasses.contains(superName)) {
        candidate = true;
        debug("ServletContext candidate by inheritance (%s extends %s)", name, superName);
      } else {
        // Then find if class implements ServletContext interface
        for (String iface : interfaces) {
          if ("javax/servlet/ServletContext".equals(iface)) {
            candidate = true;
            debug("ServletContext candidate by interface implementation (%s implements %s)", name, iface);
            break;
          }
        }
      }
      // If candidate is an interface, ignore it
      if (candidate) {
        servletContextClasses.add(name);
        if ((access & Opcodes.ACC_INTERFACE) != 0) {
          candidate = false;
        }
      }
      super.visit(version, access, name, signature, superName, interfaces);
    }
  }


  /**
   * Utility class to identify if a class is an HttpSessionListener or
   * HttpSessionAttributeListener or if it
   * inherits from an implementation of those.
   */
  class ListenerCandidateFinder extends ClassVisitor {
    boolean candidate;

    ListenerCandidateFinder() {
      super(ASM5);
    }

    @Override
    public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
      // Check if super class has already been identified as a listener
      if (listenerClasses.contains(superName)) {
        candidate = true;
        debug("Listener is candidate by inheritance (%s extends %s)", name, superName);
      } else {
        for (String iface : interfaces) {
          // find if class implements one of listener interfaces
          if ("javax/servlet/http/HttpSessionListener".equals(iface)
              || "javax/servlet/http/HttpSessionAttributeListener".equals(iface)) {
            candidate = true;
            debug("Listener is candidate by interface implementation (%s implements %s)", name, iface);
            break;
          }
        }
      }
      // If candidate is an interface, ignore it
      if (candidate) {
        listenerClasses.add(name);
        if ((access & Opcodes.ACC_INTERFACE) != 0) {
          candidate = false;
        }
      }

      super.visit(version, access, name, signature, superName, interfaces);
    }
  }
}
