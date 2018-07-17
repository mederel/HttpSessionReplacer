package com.amadeus.session.agent;

public class ByteBuddyServletContextAdapter {// implements AgentBuilder.Transformer {

  String className;

  boolean addedStaticInit;

  // @Override
  // public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
  // super.visit(version, access, name, signature, superName, interfaces);
  // className = name;
  // }
  //
  // @Override
  // public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
  // if ((access & (OppCodesACC_STATIC | ACC_PUBLIC)) == ACC_PUBLIC && "addListener".equals(name)
  // && ("(Ljava/lang/Object;)V".equals(desc) || "(Ljava/util/EventListener;)V".equals(desc))) {
  // debug("modifying addListener(...) method for %s", className);
  // MethodVisitor mv = super.visitMethod(access, name, desc, signature, exceptions);
  // return new ServletContextAdapter.EnahceAddListener(mv);
  // }
  // // Enhance static initializer if present
  // if ("<clinit>".equals(name) && !addedStaticInit) {
  // MethodVisitor mv = super.visitMethod(access, name, desc, signature, exceptions);
  // return new ServletContextAdapter.EnhanceStaticInit(mv);
  // }
  // return super.visitMethod(access, name, desc, signature, exceptions);
  // }
  //
  // @Override
  // public void visitEnd() {
  // ServletContextHelpers.methods(className, cv, !addedStaticInit);
  // super.visitEnd();
  // }
  //
  // @Override
  // public DynamicType.Builder<?> transform(DynamicType.Builder<?> builder, TypeDescription typeDescription,
  // ClassLoader classLoader, JavaModule module) {
  // return builder.method(ElementMatchers.named("addListener").and(ElementMatchers.isPublic())).intercept();
  // }

  // class EnhanceStaticInit extends MethodVisitor {
  //
  // EnhanceStaticInit(MethodVisitor mv) {
  // super(ASM5, mv);
  // }
  //
  // @Override
  // public void visitCode() {
  // ServletContextHelpers.staticInit(className, mv);
  // addedStaticInit = true;
  // super.visitCode();
  // }
  // }

  /**
   * This class modifies <code>javax.servlet.ServletContext#addListener()</code>
   * method by inserting call to equivalent of
   * <code>com.amadeus.session.servlet.SessionHelpers#onAddListener</code> as
   * the first statement.
   */
  // class EnahceAddListener extends MethodVisitor {
  //
  // EnahceAddListener(MethodVisitor mv) {
  // super(ASM5, mv);
  // }
  //
  // @Override
  // public void visitCode() {
  // mv.visitVarInsn(ALOAD, 0); // NOSONAR load argument 0 is more meaningful
  // // than load arg ZERO
  // mv.visitVarInsn(ALOAD, 1); // NOSONAR load argument 1 is more meaningful
  // // than load arg ONE
  // mv.visitMethodInsn(INVOKESTATIC, className, "$$onAddListener", "(Ljava/lang/Object;Ljava/lang/Object;)V", false);
  //
  // super.visitCode();
  // }
  // }
}
